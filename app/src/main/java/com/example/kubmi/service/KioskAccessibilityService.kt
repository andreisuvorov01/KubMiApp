package com.example.kubmi.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.kubmi.MainActivity

/**
 * Accessibility Service for intercepting global key events (HOME, RECENTS)
 * and monitoring window state changes to prevent exiting kiosk mode.
 * 
 * User must manually enable this service in Settings > Accessibility.
 */
class KioskAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KioskAccessibility"
        
        // Packages that are allowed (our own app and system UI for dialogs)
        private val ALLOWED_PACKAGES = setOf(
            "com.example.kubmi",
            "com.android.systemui",
            "com.android.settings" // Temporarily allowed for enabling permissions
        )
        
        @Volatile
        private var instance: KioskAccessibilityService? = null
        
        fun isServiceEnabled(): Boolean = instance != null
        
        /**
         * Check if the Accessibility Service is enabled in system settings
         */
        fun isServiceEnabledInSettings(context: Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            val serviceName = "${context.packageName}/${KioskAccessibilityService::class.java.canonicalName}"
            val isEnabled = enabledServices.contains(serviceName)
            
            // #region agent log
            com.example.kubmi.util.DebugLogger.log("A", "KioskAccessibilityService:isServiceEnabledInSettings", "Checking service status", mapOf("enabledServices" to enabledServices, "ourService" to serviceName, "isEnabled" to isEnabled))
            // #endregion
            
            return isEnabled
        }
        
        /**
         * Open accessibility settings for user to enable the service
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private val kioskPrefs by lazy { 
        getSharedPreferences(KioskService.PREF_KIOSK_GUARD, Context.MODE_PRIVATE) 
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        // #region agent log
        com.example.kubmi.util.DebugLogger.log("A", "KioskAccessibilityService:onServiceConnected", "Accessibility service connected", mapOf("instance" to (instance != null)))
        // #endregion
        
        // Configure the service to receive key events and window state changes
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isTemporaryExitAllowed()) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                
                // If a non-allowed package comes to foreground, return to our app
                if (!isAllowedPackage(packageName)) {
                    returnToApp()
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        // #region agent log
        val tempExitAllowed = isTemporaryExitAllowed()
        com.example.kubmi.util.DebugLogger.log("B", "KioskAccessibilityService:onKeyEvent", "Key event received", mapOf("keyCode" to (event?.keyCode ?: -1), "action" to (event?.action ?: -1), "tempExitAllowed" to tempExitAllowed))
        // #endregion
        
        if (event == null || tempExitAllowed) {
            return super.onKeyEvent(event)
        }
        
        // Block HOME, BACK and APP_SWITCH (RECENTS) keys
        return when (event.keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_SEARCH -> {
                // #region agent log
                com.example.kubmi.util.DebugLogger.log("B", "KioskAccessibilityService:onKeyEvent", "Blocking key", mapOf("keyCode" to event.keyCode))
                // #endregion
                // Block these keys by returning true (consumed)
                true
            }
            else -> super.onKeyEvent(event)
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    private fun isAllowedPackage(packageName: String): Boolean {
        return ALLOWED_PACKAGES.contains(packageName) || 
               packageName == this.packageName ||
               packageName.startsWith("com.example.kubmi")
    }

    private fun isTemporaryExitAllowed(): Boolean {
        val allowUntil = kioskPrefs.getLong(KioskService.KEY_ALLOW_EXIT_UNTIL, 0L)
        return System.currentTimeMillis() < allowUntil
    }

    private fun returnToApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
