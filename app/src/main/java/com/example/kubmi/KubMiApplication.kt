package com.example.kubmi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.example.kubmi.data.worker.DataSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import timber.log.Timber
import coil.Coil
import coil.ImageLoader
import android.os.Build

/**
 * Main Application class for KubMI.
 *
 * Features:
 * - Hilt dependency injection setup
 * - WorkManager configuration for background data synchronization
 * - Timber logging initialization for debug builds
 *
 * @see [Hilt Android Documentation](https://developer.android.com/training/dependency-injection/hilt-android)
 * @see [WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)
 */
@HiltAndroidApp
class KubMiApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Coil with custom ImageLoader for optimized image caching
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Coil will be initialized with our custom ImageLoader from Hilt
            // The ImageLoader is provided via Hilt dependency injection
        }

        setupWorkManager()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /**
     * Sets up periodic data synchronization work.
     * Runs every 30 minutes when device is connected to network.
     */
    private fun setupWorkManager() {
        val workRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
            repeatInterval = 30,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            1,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "data_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        // Timber.d("WorkManager setup completed for data synchronization")
    }
}
