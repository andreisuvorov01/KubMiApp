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
        if (isTemporaryExitAllowed()) return

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

    private fun isTemporaryExitAllowed(): Boolean {
        return KioskService.isTemporaryExitAllowed(this)
    }
    
    private fun returnToKiosk() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
    
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
        Log.d(TAG, "App monitor service stopped")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
