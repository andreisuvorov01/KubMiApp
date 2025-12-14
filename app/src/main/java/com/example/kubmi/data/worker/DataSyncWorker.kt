package com.example.kubmi.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kubmi.domain.repository.NewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val newsRepository: NewsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Refresh both news and schedule data
            newsRepository.refreshNews()
            // Note: Schedule refresh would require ScheduleRepository
            // For now, we're only refreshing news in this worker
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
