package com.example.kubmi

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.kubmi.util.SecurePreferences
import com.example.kubmi.util.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import java.io.File
import org.json.JSONObject

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
class KubMiApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Ensure default admin password exists for kiosk exit flow.
        val securePreferences = SecurePreferences(this)
        if (!securePreferences.isPasswordSet()) {
            securePreferences.savePassword("kubmiadmin")
        }

        setupWorkManager()
    }

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
            // logging must not crash app
        }
    }
    // #endregion

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /**
     * Sets up periodic data synchronization work.
     * Now scheduled twice daily (00:00 and 12:00) with WorkManager fallback.
     */
    private fun setupWorkManager() {
        agentLog(
            hypothesisId = "H1",
            location = "KubMiApplication:setupWorkManager",
            message = "Configuring twice-daily sync",
            data = mapOf(
                "factory" to workerFactory::class.java.simpleName
            )
        )

        // Twice-daily exact alarms with WorkManager fallback.
        WorkScheduler.scheduleDailySync(this)
        // Ensure we have data on first app start without waiting for alarm.
        WorkScheduler.enqueueImmediateSync(this, reason = "app_start")
    }

    /**
     * Default Coil ImageLoader used by AsyncImage/rememberAsyncImagePainter across the app.
     * - Memory + Disk cache enabled
     * - Concurrency limited to avoid overloading weaker devices (TV boxes, low RAM phones)
     * - SVG support enabled (site uses SVG in some places)
     */
    override fun newImageLoader(): ImageLoader {
        val limited = Dispatchers.IO.limitedParallelism(3)
        return buildAppImageLoader(this, limited)
    }

    private fun buildAppImageLoader(context: Context, limitedDispatcher: kotlinx.coroutines.CoroutineDispatcher): ImageLoader {
        val builder = ImageLoader.Builder(context)
            // Reduce simultaneous downloads/decodes to avoid stalls.
            .dispatcher(limitedDispatcher)
            .fetcherDispatcher(limitedDispatcher)
            .decoderDispatcher(limitedDispatcher)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L) // 100MB
                    .build()
            }
            .crossfade(true)
            .allowHardware(true)
            .respectCacheHeaders(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .components { add(SvgDecoder.Factory()) }
        if (BuildConfig.DEBUG) {
            builder.logger(DebugLogger())
        }
        return builder.build()
    }
}
