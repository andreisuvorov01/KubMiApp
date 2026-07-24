package com.example.kubmi.kiosk

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.kubmi.MainActivity
import com.example.kubmi.service.KioskService

class AppMonitorService : Service() {
    
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager
    
    companion object {
        private const val TAG = "AppMonitorService"
        private const val CHECK_INTERVAL = 500L
    }
    
    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        startMonitoring()
        Log.d(TAG, "App monitor service started")
    }
    
    private fun startMonitoring() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkForegroundApp()
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }, CHECK_INTERVAL)
    }
    
    private fun checkForegroundApp() {
        if (KioskService.isTemporaryExitAllowed(this)) return

        // If the board's own kiosk (Lock Task) is managing us, skip — avoid restart loops.
        if (isSystemLockTaskActive()) return

        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000,
            time
        )
        
        val foregroundApp = stats?.maxByOrNull { it.lastTimeUsed }
        
        if (foregroundApp?.packageName != packageName && 
            foregroundApp?.packageName != null) {
            Log.w(TAG, "Detected foreign app: ${foregroundApp.packageName}")
            returnToKiosk()
        }
    }

    private fun isSystemLockTaskActive(): Boolean {
        return try {
            val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE
            } else {
                @Suppress("DEPRECATION")
                am.isInLockTaskMode
            }
        } catch (_: Exception) { false }
    }
    
    private fun returnToKiosk() {
        val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        // Prefer moveToFront (no BAL restriction) — works when a task already exists
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val task = am.appTasks.firstOrNull { it.taskInfo?.baseActivity?.packageName == packageName }
            if (task != null) {
                runCatching { task.moveToFront() }
                return
            }
        }
        // Fallback: use a full-screen PendingIntent to work around BAL
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pi = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        runCatching { pi.send() }
    }
    
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
        Log.d(TAG, "App monitor service stopped")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
