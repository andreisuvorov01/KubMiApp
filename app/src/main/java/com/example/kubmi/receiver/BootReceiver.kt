package com.example.kubmi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.kubmi.MainActivity
import com.example.kubmi.service.KioskService
import com.example.kubmi.util.WorkScheduler

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_REBOOT,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                KioskService.start(context)
                startMainActivity(context)
                WorkScheduler.scheduleDailySync(context)
            }
        }
    }
    
    private fun startMainActivity(context: Context) {
        try {
            Log.d(TAG, "Starting MainActivity")
            val startIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(startIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MainActivity", e)
            // Implement fallback mechanism if needed
        }
    }
}
