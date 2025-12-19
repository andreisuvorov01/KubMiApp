package com.example.kubmi.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kubmi.domain.repository.AboutRepository
import com.example.kubmi.domain.repository.NewsRepository
import com.example.kubmi.domain.repository.ScheduleRepository
import com.example.kubmi.domain.model.News
import com.example.kubmi.domain.model.ScheduleIndexEntry
import com.example.kubmi.domain.model.AboutPageContent
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.CachePolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val newsRepository: NewsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val aboutRepository: AboutRepository
) : CoroutineWorker(context, params) {

    private val syncDispatcher = Dispatchers.IO.limitedParallelism(5)

    // #region agent log
    private fun agentLog(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "run1"
    ) {
        try {
            val payload = mapOf(
                "sessionId" to "debug-session",
                "runId" to runId,
                "hypothesisId" to hypothesisId,
                "location" to location,
                "message" to message,
                "data" to data,
                "timestamp" to System.currentTimeMillis()
            )
            val json = JSONObject(payload).toString()
            Log.d("DEBUG_LOG", json) // Пишем в Logcat, чтобы не зависеть от файловой системы
        } catch (_: Exception) {
        }
    }
    // #endregion

    init {
        agentLog(
            hypothesisId = "H1",
            location = "DataSyncWorker:init",
            message = "Worker constructed",
            data = mapOf(
                "appContext" to applicationContext.javaClass.simpleName,
                "paramsId" to params.id.toString()
            )
        )
    }

    override suspend fun doWork(): Result {
        agentLog(
            hypothesisId = "H2",
            location = "DataSyncWorker:doWork",
            message = "Starting full offline sync",
            data = mapOf(
                "attempt" to runAttemptCount,
                "reason" to inputData.getString("reason")
            )
        )
        return try {
            withContext(syncDispatcher) {
                // 1. Sync News
                syncNews()
                
                // 2. Sync Schedules
                syncSchedules()
                
                // 3. Sync About Page Media
                syncAboutMedia()
            }

            agentLog(
                hypothesisId = "H2",
                location = "DataSyncWorker:doWork",
                message = "Full sync success",
                data = mapOf("attempt" to runAttemptCount)
            )
            Result.success()
        } catch (e: Exception) {
            agentLog(
                hypothesisId = "H2",
                location = "DataSyncWorker:doWork",
                message = "Full sync failed",
                data = mapOf(
                    "attempt" to runAttemptCount,
                    "error" to (e.message ?: e::class.java.simpleName)
                )
            )
            Timber.e(e, "DataSyncWorker: Sync failed")
            Result.retry()
        }
    }

    private suspend fun syncNews() = coroutineScope {
        Timber.d("Syncing news list...")
        newsRepository.refreshNews(forceNetwork = true)
        
        val allNews = newsRepository.getAllNewsSync()
        Timber.d("Syncing ${allNews.size} news articles and their images...")
        
        allNews.map { news ->
            async {
                try {
                    // This will scrape the article and download all images within it
                    newsRepository.refreshNewsArticle(news.id)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync article detail: ${news.id}")
                }
            }
        }.awaitAll()
    }

    private suspend fun syncSchedules() = coroutineScope {
        Timber.d("Syncing full groups list...")

        // Use getAllGroups instead of groups table for more robust syncing of all items
        val allGroups = scheduleRepository.getAllGroups(forceNetwork = true)
        
        Timber.d("Syncing ${allGroups.size} individual group schedules...")
        allGroups.map { group ->
            async {
                try {
                    if (group.url.isNotBlank()) {
                        Log.d("DataSyncWorker", "Syncing schedule for group: ${group.title} ($group.url)")
                        scheduleRepository.refreshStudentScheduleByUrl(group.title, group.url, forceNetwork = true)
                    }
                } catch (e: Exception) {
                    Log.e("DataSyncWorker", "Failed to sync group schedule: ${group.title}, url=${group.url}", e)
                }
            }
        }.awaitAll()

        Timber.d("Syncing full teachers list...")
        val teachers = scheduleRepository.getAllTeachers(forceNetwork = true)
        
        Timber.d("Syncing ${teachers.size} individual teacher schedules...")
        teachers.map { teacher ->
            async {
                try {
                    if (teacher.url.isNotBlank()) {
                        scheduleRepository.refreshTeacherScheduleByUrl(teacher.title, teacher.url, forceNetwork = true)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync teacher schedule: ${teacher.title}")
                }
            }
        }.awaitAll()
        
        Timber.d("Syncing main groups table...")
        // Also refresh the main groups table for the UI
        scheduleRepository.getStudentGroupsTable(forceNetwork = true)
    }

    private suspend fun syncAboutMedia() = coroutineScope {
        Timber.d("Syncing About page media...")
        val aboutContent = aboutRepository.refreshAboutContent()
        
        val imageUrls = mutableSetOf<String>()
        aboutContent.foundingLeaders.forEach { imageUrls.add(it.imageUrl) }
        aboutContent.todayLeaders.forEach { imageUrls.add(it.imageUrl) }
        
        val context = applicationContext
        imageUrls.filter { it.startsWith("http") }.map { url ->
            async {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.DISABLED) // We only care about disk for pre-fetching
                        .build()
                    context.imageLoader.execute(request)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to pre-fetch image: $url")
                }
            }
        }.awaitAll()
    }
}
