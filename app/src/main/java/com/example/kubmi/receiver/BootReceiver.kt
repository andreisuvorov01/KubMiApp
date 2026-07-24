package com.example.kubmi.receiver

import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.kubmi.MainActivity
import com.example.kubmi.service.KioskService
import com.example.kubmi.service.KioskJobService
import com.example.kubmi.util.WorkScheduler

/**
 * BootReceiver отвечает за инициализацию Kiosk режима при загрузке устройства
 * 
 * Функции:
 * 1. Запуск KioskService (Foreground Service)
 * 2. Запуск MainActivity
 * 3. Планирование периодических проверок через AlarmManager
 * 4. Запуск JobService для Android 12+
 * 5. Применение Device Owner конфигурации (если доступно)
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
        private const val STARTUP_DELAY_MS = 3000L  // Задержка при загрузке для стабильности
        private const val WATCHDOG_ALARM_ID = 42
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        
        when (intent.action) {
            // Основные события загрузки
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_REBOOT,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                setupKioskModeAtBoot(context)
            }
        }
    }
    
    /**
     * Полная инициализация Kiosk режима при загрузке
     */
    private fun setupKioskModeAtBoot(context: Context) {
        Log.d(TAG, "Setting up Kiosk Mode at boot")
        // A maintenance window must never survive a reboot or app update.
        KioskService.clearTemporaryExit(context)
        
        // Использовать Handler с задержкой для дождаться полной загрузки системы
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                // 1. Запуск Foreground Service (главная защита)
                startKioskService(context)
                
                // 2. Запуск MainActivity
                startMainActivity(context)
                
                // 3. Запуск фоновых сервисов
                startBackgroundServices(context)
                
                // 4. Планирование периодических проверок
                scheduleWatchdogAlarm(context)
                
                // 5. Планирование JobService (Android 12+)
                scheduleJobService(context)
                
                // 6. Синхронизация работ
                WorkScheduler.scheduleDailySync(context)
                
                Log.i(TAG, "✓ Kiosk Mode boot setup complete")
                
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error during kiosk boot setup", e)
            }
        }, STARTUP_DELAY_MS)
    }
    
    /**
     * Запуск KioskService (Foreground Service для постоянной защиты)
     */
    private fun startKioskService(context: Context) {
        try {
            KioskService.start(context)
            Log.d(TAG, "✓ KioskService started")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to start KioskService", e)
        }
    }
    
    /**
     * Запуск MainActivity
     */
    private fun startMainActivity(context: Context) {
        try {
            Log.d(TAG, "Starting MainActivity")
            val startIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= 34) {
                options.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
            context.startActivity(startIntent, options.toBundle())
            Log.d(TAG, "✓ MainActivity started")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to start MainActivity", e)
        }
    }
    
    /**
     * Запуск дополнительных фоновых сервисов
     */
    private fun startBackgroundServices(context: Context) {
        try {
            // Запуск AppMonitorService
            val appMonitorIntent = Intent(context, com.example.kubmi.kiosk.AppMonitorService::class.java)
            context.startService(appMonitorIntent)
            Log.d(TAG, "✓ AppMonitorService started")
            
            // Запуск OverlayService (если доступен)
            val devicePolicyManager =
                context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                val overlayIntent = Intent(context, com.example.kubmi.OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(overlayIntent)
                } else {
                    context.startService(overlayIntent)
                }
            }
            Log.d(TAG, "✓ OverlayService started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background services", e)
        }
    }
    
    /**
     * Планирование AlarmManager watchdog для периодической проверки
     * Это дополнительная защита если Foreground Service будет убит
     */
    private fun scheduleWatchdogAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Создать Intent для повторного запуска KioskService
            val watchdogIntent = Intent(context, BootReceiver::class.java).apply {
                action = "com.example.kubmi.ACTION_WATCHDOG"
                putExtra("reason", "periodic_check")
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                WATCHDOG_ALARM_ID,
                watchdogIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Установить повторяющееся оповещение каждые 5 минут
            val interval = 5 * 60 * 1000L  // 5 минут
            val triggerAtMs = System.currentTimeMillis() + interval
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+: использовать setAndAllowWhileIdle
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    interval,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "✓ Watchdog alarm scheduled (5 min interval)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule watchdog alarm", e)
        }
    }
    
    /**
     * Планирование JobService для Android 12+
     */
    private fun scheduleJobService(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                KioskJobService.schedule(context)
                Log.d(TAG, "✓ JobService scheduled (Android 12+)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule JobService", e)
        }
    }
}
