package com.example.kubmi.util

import android.content.Context
import com.example.kubmi.domain.model.News
import com.example.kubmi.domain.model.ScheduleIndexEntry
import com.example.kubmi.domain.model.StudentGroupsTable
import com.example.kubmi.domain.model.WeeklyScheduleData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight file-based cache for parsed data.
 * Stores JSON snapshots under cacheDir/parser to avoid repeated downloads between app launches.
 */
@Singleton
class ParserCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val baseDir: File by lazy { context.cacheDir.resolve("parser").also { it.mkdirs() } }

    private data class Payload<T>(
        val version: Int,
        val savedAt: Long,
        val data: T
    )

    private val newsFile get() = baseDir.resolve("news.json")
    private val groupsFile get() = baseDir.resolve("groups.json")
    private val teachersFile get() = baseDir.resolve("teachers.json")

    private fun scheduleFile(ownerType: String, ownerTitle: String): File {
        val safeTitle = ownerTitle.ifBlank { "untitled" }.hashCode().toString()
        return baseDir.resolve("schedule_${ownerType}_$safeTitle.json")
    }

    suspend fun readNews(): List<News>? =
        readPayload<List<News>>(
            newsFile,
            NEWS_VERSION,
            object : TypeToken<Payload<List<News>>>() {}.type
        )?.data?.sortedByDescending { it.timestamp }

    suspend fun writeNews(items: List<News>) {
        writePayload(newsFile, NEWS_VERSION, items.sortedByDescending { it.timestamp })
    }

    suspend fun readGroupsTable(): StudentGroupsTable? =
        readPayload<StudentGroupsTable>(
            groupsFile,
            GROUPS_VERSION,
            object : TypeToken<Payload<StudentGroupsTable>>() {}.type
        )?.data

    suspend fun writeGroupsTable(table: StudentGroupsTable) {
        writePayload(groupsFile, GROUPS_VERSION, table)
    }

    suspend fun readTeachers(): List<ScheduleIndexEntry>? =
        readPayload<List<ScheduleIndexEntry>>(
            teachersFile,
            TEACHERS_VERSION,
            object : TypeToken<Payload<List<ScheduleIndexEntry>>>() {}.type
        )?.data

    suspend fun writeTeachers(entries: List<ScheduleIndexEntry>) {
        writePayload(teachersFile, TEACHERS_VERSION, entries)
    }

    suspend fun readSchedule(ownerType: String, ownerTitle: String): List<WeeklyScheduleData>? {
        val file = scheduleFile(ownerType, ownerTitle)
        return readPayload<List<WeeklyScheduleData>>(
            file,
            SCHEDULE_VERSION,
            object : TypeToken<Payload<List<WeeklyScheduleData>>>() {}.type
        )?.data
    }

    suspend fun writeSchedule(ownerType: String, ownerTitle: String, data: List<WeeklyScheduleData>) {
        val file = scheduleFile(ownerType, ownerTitle)
        writePayload(file, SCHEDULE_VERSION, data)
    }

    private suspend fun <T> writePayload(file: File, version: Int, data: T) {
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = Payload(version = version, savedAt = System.currentTimeMillis(), data = data)
                file.parentFile?.mkdirs()
                file.writeText(gson.toJson(payload))
            }
        }
    }

    private suspend fun <T> readPayload(file: File, expectedVersion: Int, type: java.lang.reflect.Type): Payload<T>? {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (!file.exists()) return@withContext null
                val payload: Payload<T> = gson.fromJson(file.reader(), type)
                if (payload.version != expectedVersion) return@withContext null
                payload
            }.getOrNull()
        }
    }

    companion object {
        private const val NEWS_VERSION = 1
        private const val GROUPS_VERSION = 1
        private const val TEACHERS_VERSION = 1
        private const val SCHEDULE_VERSION = 1
    }
}
