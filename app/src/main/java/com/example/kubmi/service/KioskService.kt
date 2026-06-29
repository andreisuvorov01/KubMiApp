package com.example.kubmi.service

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.kubmi.MainActivity
import com.example.kubmi.kiosk.KeyEventBlocker
import com.example.kubmi.R

/**
 * Lightweight watchdog to return the app to foreground if a user exits it.
 * Does not require Device Owner; relies on periodic foreground checks.
 */
class KioskService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            ensureAppInForeground()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.postDelayed(checkRunnable, CHECK_INTERVAL_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(checkRunnable)
        handler.postDelayed(checkRunnable, CHECK_INTERVAL_MS)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureAppInForeground() {
        // If the system (e.g. GaokeView board kiosk) already holds us in Lock Task,
        // our watchdog is redundant and can cause restart loops — skip it.
        if (isSystemLockTaskActive()) return

        val inForeground = isAppInForeground()
        val tempExitAllowed = isTemporaryExitAllowed()
        
        // #region agent log
        com.example.kubmi.util.DebugLogger.log("C", "KioskService:ensureAppInForeground", "Checking foreground", mapOf("inForeground" to inForeground, "tempExitAllowed" to tempExitAllowed))
        // #endregion
        
        if (inForeground || tempExitAllowed) return
        if (KeyEventBlocker.foregroundReturnPaused) return

        // #region agent log
        com.example.kubmi.util.DebugLogger.log("C", "KioskService:ensureAppInForeground", "App not in foreground, bringing back", mapOf())
        // #endregion

        moveTaskToFront()
    }

    private fun isSystemLockTaskActive(): Boolean {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
            } else {
                @Suppress("DEPRECATION")
                am.isInLockTaskMode
            }
        } catch (_: Exception) { false }
    }

    private fun isAppInForeground(): Boolean {
        // Method 1: Check via running processes
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = packageName

        val runningProcess = activityManager.runningAppProcesses
            ?.firstOrNull { process ->
                process.processName == packageName &&
                    process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    process.uid == Process.myUid()
            }
        if (runningProcess != null) return true

        // Method 2: Check via app tasks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activityManager.appTasks.any { it.taskInfo?.topActivity?.packageName == packageName }) {
                return true
            }
        } else {
            @Suppress("DEPRECATION")
            if (activityManager.getRunningTasks(1)?.firstOrNull()?.topActivity?.packageName == packageName) {
                return true
            }
        }
        
        // Method 3: Check via UsageStats (most reliable if permission granted)
        if (isUsageStatsPermissionGranted()) {
            val foregroundPackage = getForegroundPackageViaUsageStats()
            if (foregroundPackage == packageName) return true
        }
        
        return false
    }
    
    /**
     * Check if we have Usage Stats permission
     */
    private fun isUsageStatsPermissionGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    /**
     * Get the current foreground package using UsageStatsManager
     * More reliable than other methods on newer Android versions
     */
    private fun getForegroundPackageViaUsageStats(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return null
        
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        
        // Query recent usage events (last 10 seconds)
        val usageEvents = usageStatsManager.queryEvents(currentTime - 10_000, currentTime)
        
        var lastForegroundPackage: String? = null
        var lastForegroundTime = 0L
        
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            
            // Look for MOVE_TO_FOREGROUND events (most reliable indicator)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                 event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)) {
                if (event.timeStamp > lastForegroundTime) {
                    lastForegroundTime = event.timeStamp
                    lastForegroundPackage = event.packageName
                }
            }
        }
        
        return lastForegroundPackage
    }

    private fun isTemporaryExitAllowed(): Boolean {
        val prefs = getSharedPreferences(PREF_KIOSK_GUARD, Context.MODE_PRIVATE)
        val allowUntil = prefs.getLong(KEY_ALLOW_EXIT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        val isAllowed = now < allowUntil
        val remainingMs = if (isAllowed) allowUntil - now else 0
        
        // #region agent log
        if (isAllowed && remainingMs < 15000) {
            com.example.kubmi.util.DebugLogger.log("D", "KioskService:isTemporaryExitAllowed", "Exit window status", mapOf("allowed" to isAllowed, "remainingMs" to remainingMs))
        }
        // #endregion
        
        return isAllowed
    }

    private fun moveTaskToFront(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val task = activityManager.appTasks.firstOrNull { it.taskInfo?.baseActivity?.packageName == packageName }
            if (task != null) {
                return runCatching { task.moveToFront() }.isSuccess
            }
        } else {
            @Suppress("DEPRECATION")
            return runCatching {
                val tasks = activityManager.getRunningTasks(5)
                val ownTask = tasks.firstOrNull { it.baseActivity?.packageName == packageName }
                if (ownTask != null) {
                    @Suppress("DEPRECATION")
                    activityManager.moveTaskToFront(ownTask.id, ActivityManager.MOVE_TASK_WITH_HOME)
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
        }
        return false
    }

    private fun buildNotification(): Notification {
        val channelId = ensureChannel()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.kiosk_service_notification_title))
            .setContentText(getString(R.string.kiosk_service_notification_body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun ensureChannel(): String {
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                getString(R.string.kiosk_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.kiosk_service_channel_desc)
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
        return channelId
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "kiosk_guard_channel"
        private const val CHECK_INTERVAL_MS = 1500L
        const val PREF_KIOSK_GUARD = "kiosk_guard"
        const val KEY_ALLOW_EXIT_UNTIL = "allow_exit_until"
        const val ALLOW_EXIT_WINDOW_MS = 20_000L
        const val ADMIN_EXIT_WINDOW_MS = 120_000L

        fun start(context: Context) {
            try {
                val intent = Intent(context, KioskService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // On Android 12+, starting foreground services from background is restricted.
                // We log it and rely on the activity starting it when it comes to foreground.
                android.util.Log.e("KioskService", "Failed to start KioskService: ${e.message}")
            }
        }
    }
}

