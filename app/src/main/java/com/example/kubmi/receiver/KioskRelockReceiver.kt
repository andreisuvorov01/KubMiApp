package com.example.kubmi.receiver

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.kubmi.MainActivity
import com.example.kubmi.service.KioskService
import com.example.kubmi.util.AdminLogger

/**
 * Fail-safe endpoint for both the maintenance timeout and the notification
 * action that returns the device to kiosk immediately.
 */
class KioskRelockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RELOCK_NOW) return

        KioskService.clearTemporaryExit(context)
        AdminLogger(context).logAction("KIOSK_RELOCKED", "source=${intent.action}")
        KioskService.start(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching {
            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= 34) {
                options.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
            context.startActivity(launchIntent, options.toBundle())
        }.onFailure {
            Log.e(TAG, "Failed to return to kiosk", it)
        }
    }

    companion object {
        const val ACTION_RELOCK_NOW = "com.example.kubmi.ACTION_RELOCK_NOW"
        private const val TAG = "KioskRelockReceiver"
    }
}
