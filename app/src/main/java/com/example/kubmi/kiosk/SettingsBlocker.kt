package com.example.kubmi.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Блокировщик Intent-ов для предотвращения открытия Settings и других системных приложений
 * 
 * Работает путём регистрации BroadcastReceiver с высоким приоритетом для перехвата
 * системных Intent-ов перед их обработкой системой
 * 
 * Поддерживается: Android 5.0+
 * Требуемое разрешение: android.permission.SYSTEM_ALERT_WINDOW (опционально)
 */
class SettingsBlocker(private val context: Context) {
    
    private var isRegistered = false
    private var intentReceiver: BroadcastReceiver? = null
    
    companion object {
        private const val TAG = "SettingsBlocker"
    }
    
    /**
     * Инициализировать и зарегистрировать блокировку Intent-ов
     */
    fun initialize() {
        if (isRegistered) {
            Log.w(TAG, "Already registered")
            return
        }
        
        try {
            setupIntentBlocker()
            isRegistered = true
            Log.d(TAG, "✓ Settings blocker initialized")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to initialize settings blocker", e)
        }
    }
    
    /**
     * Отменить регистрацию блокировщика
     */
    fun unregister() {
        if (!isRegistered || intentReceiver == null) {
            return
        }
        
        try {
            context.unregisterReceiver(intentReceiver)
            isRegistered = false
            Log.d(TAG, "✓ Settings blocker unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to unregister receiver", e)
        }
    }
    
    /**
     * Настроить перехват всех Intent-ов связанных с Settings
     */
    private fun setupIntentBlocker() {
        val filter = IntentFilter()
        
        // Основные Intent-ы Settings
        filter.addAction(Settings.ACTION_SETTINGS)
        filter.addAction(Settings.ACTION_WIFI_SETTINGS)
        filter.addAction(Settings.ACTION_BLUETOOTH_SETTINGS)
        filter.addAction(Settings.ACTION_APPLICATION_SETTINGS)
        filter.addAction(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        filter.addAction(Settings.ACTION_SECURITY_SETTINGS)
        filter.addAction(Settings.ACTION_PRIVACY_SETTINGS)
        filter.addAction(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        filter.addAction(Settings.ACTION_DEVICE_INFO_SETTINGS)
        filter.addAction(Settings.ACTION_DATE_SETTINGS)
        filter.addAction(Settings.ACTION_SOUND_SETTINGS)
        filter.addAction(Settings.ACTION_DISPLAY_SETTINGS)
        filter.addAction(Settings.ACTION_LOCALE_SETTINGS)
        filter.addAction(Settings.ACTION_INPUT_METHOD_SETTINGS)
        filter.addAction(Settings.ACTION_NFC_SETTINGS)
        filter.addAction(Settings.ACTION_NFC_PAYMENT_SETTINGS)
        filter.addAction(Settings.ACTION_MEMORY_CARD_SETTINGS)
        
        // Android 5.0+ Intent-ы
        if (Build.VERSION_CODES.LOLLIPOP >= 21) {
            filter.addAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            filter.addAction(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        }
        
        // Android 6.0+ Intent-ы
        if (Build.VERSION_CODES.M >= 23) {
            filter.addAction(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            filter.addAction(Settings.ACTION_DATA_ROAMING_SETTINGS)
        }
        
        // Android 8.0+ Intent-ы
        if (Build.VERSION_CODES.O >= 26) {
            filter.addAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            filter.addAction(Settings.ACTION_APP_LOCALE_SETTINGS)
        }
        
        // Установить высокий приоритет для перехвата
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        
        intentReceiver = SettingsBlockerReceiver()
        
        // Попробовать использовать Context.registerReceiver с нужными флагами
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(
                    intentReceiver!!,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(intentReceiver!!, filter)
            }
            
            Log.d(TAG, "✓ Intent filters registered")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to register intent filters", e)
            intentReceiver = null
        }
    }
    
    /**
     * Внутренний BroadcastReceiver для блокировки Intent-ов
     */
    private inner class SettingsBlockerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            
            val action = intent.action
            Log.w(TAG, "Blocked intent: $action")
            
            // Остановить дальнейшую обработку Intent-а
            abortBroadcast()
            
            // Опционально: вернуть приложение в foreground
            try {
                val mainIntent = Intent(context, getMainActivityClass()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context?.startActivity(mainIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to return to main activity", e)
            }
        }
    }
    
    /**
     * Получить класс главной Activity (нужно переопределить)
     */
    private fun getMainActivityClass(): Class<*> {
        return try {
            Class.forName("com.example.kubmi.MainActivity")
        } catch (e: Exception) {
            android.app.Activity::class.java
        }
    }
}
