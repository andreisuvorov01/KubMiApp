package com.example.kubmi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.kubmi.util.WorkScheduler

/**
 * Receives exact alarms (00:00 and 12:00) and triggers a one-off sync.
 * Also reschedules the next alarm to keep the twice-daily cadence stable.
 */
class SyncAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val requestCode = intent?.getIntExtra("requestCode", -1) ?: -1
        Log.i("SyncAlarmReceiver", "Alarm received code=$requestCode action=${intent?.action}")

        // Kick off an immediate WorkManager sync.
        WorkScheduler.enqueueImmediateSync(context, reason = "alarm_$requestCode")

        // Ensure the next alarms remain scheduled (idempotent).
        WorkScheduler.scheduleDailySync(context)
    }
}
