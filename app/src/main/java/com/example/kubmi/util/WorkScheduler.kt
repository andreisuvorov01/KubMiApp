package com.example.kubmi.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kubmi.data.worker.DataSyncWorker
import com.example.kubmi.receiver.SyncAlarmReceiver
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.json.JSONObject

object WorkScheduler {
    private const val DATA_SYNC_WORK = "data_sync"
    private const val DATA_SYNC_PERIODIC_FALLBACK = "data_sync_periodic_fallback"
    private const val ACTION_SYNC = "com.example.kubmi.ACTION_PARSER_SYNC"
    private const val REQUEST_MIDNIGHT = 1001
    private const val REQUEST_NOON = 1002

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
            // ignore logging issues
        }
    }
    // #endregion

    fun scheduleDailySync(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val canUseExact = alarmManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        } ?: false

        if (canUseExact && alarmManager != null) {
            scheduleExactAlarm(context, alarmManager, REQUEST_MIDNIGHT, 0, 0)
            scheduleExactAlarm(context, alarmManager, REQUEST_NOON, 12, 0)
            // Also keep a WorkManager periodic fallback in case alarms get killed.
            schedulePeriodicFallback(context)
        } else {
            // Fallback to WorkManager only (Doze-friendly, but not exact).
            schedulePeriodicFallback(context)
        }
    }

    fun cancelDataSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DATA_SYNC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(DATA_SYNC_PERIODIC_FALLBACK)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(buildPendingIntent(context, REQUEST_MIDNIGHT))
        alarmManager?.cancel(buildPendingIntent(context, REQUEST_NOON))
    }

    fun enqueueImmediateSync(context: Context, reason: String = "manual") {
        val request = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .setInputData(androidx.work.Data.Builder().putString("reason", reason).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DATA_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun schedulePeriodicFallback(context: Context) {
        val periodicRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        agentLog(
            hypothesisId = "H1",
            location = "WorkScheduler:schedulePeriodicFallback",
            message = "Scheduling 12h fallback work",
            data = mapOf(
                "network" to periodicRequest.workSpec.constraints.requiredNetworkType.name,
                "batteryNotLow" to periodicRequest.workSpec.constraints.requiresBatteryNotLow(),
                "policy" to ExistingPeriodicWorkPolicy.UPDATE.name
            )
        )

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DATA_SYNC_PERIODIC_FALLBACK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val triggerAtMillis = computeNextTrigger(hour, minute)
        val pendingIntent = buildPendingIntent(context, requestCode)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        agentLog(
            hypothesisId = "H1",
            location = "WorkScheduler:scheduleExactAlarm",
            message = "Alarm scheduled",
            data = mapOf(
                "requestCode" to requestCode,
                "time" to triggerAtMillis,
                "hour" to hour,
                "minute" to minute
            )
        )
    }

    private fun buildPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, SyncAlarmReceiver::class.java).apply {
            action = ACTION_SYNC
            putExtra("requestCode", requestCode)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun computeNextTrigger(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
