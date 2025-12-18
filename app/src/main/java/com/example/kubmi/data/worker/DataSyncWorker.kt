package com.example.kubmi.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kubmi.domain.repository.NewsRepository
import com.example.kubmi.domain.repository.ScheduleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import org.json.JSONObject

@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val newsRepository: NewsRepository,
    private val scheduleRepository: ScheduleRepository
) : CoroutineWorker(context, params) {

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
            File("d:\\AndroidProject\\.cursor\\debug.log").appendText(
                JSONObject(payload).toString() + "\n"
            )
        } catch (_: Exception) {
            // swallow to avoid impacting worker
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
            message = "Starting sync",
            data = mapOf(
                "attempt" to runAttemptCount,
                "reason" to inputData.getString("reason")
            )
        )
        return try {
            // Refresh news and seed caches so UI can start offline.
            newsRepository.refreshNews(forceNetwork = true)
            // Refresh lookup tables for schedule navigation.
            scheduleRepository.getStudentGroupsTable(forceNetwork = true)
            scheduleRepository.getAllTeachers(forceNetwork = true)
            agentLog(
                hypothesisId = "H2",
                location = "DataSyncWorker:doWork",
                message = "Sync success",
                data = mapOf("attempt" to runAttemptCount)
            )
            Result.success()
        } catch (e: Exception) {
            agentLog(
                hypothesisId = "H2",
                location = "DataSyncWorker:doWork",
                message = "Sync failed",
                data = mapOf(
                    "attempt" to runAttemptCount,
                    "error" to (e.message ?: e::class.java.simpleName)
                )
            )
            Result.retry()
        }
    }
}
