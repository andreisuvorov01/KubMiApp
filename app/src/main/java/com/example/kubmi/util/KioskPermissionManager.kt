package com.example.kubmi.util

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.kubmi.receiver.DeviceAdminReceiver
import com.example.kubmi.service.KioskAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages kiosk mode permissions and provides status checks
 * for all required permissions to fully enable kiosk mode.
 */
@Singleton
class KioskPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Data class representing the current status of all kiosk permissions
     */
    data class KioskPermissionStatus(
        val isAccessibilityEnabled: Boolean,
        val isUsageStatsEnabled: Boolean,
        val isDeviceAdminEnabled: Boolean,
        val isDefaultLauncher: Boolean,
        val overallProtectionLevel: ProtectionLevel
    )
    
    enum class ProtectionLevel {
        NONE,       // No protection enabled
        BASIC,      // Just launcher + watchdog service
        MEDIUM,     // + Usage stats or accessibility
        HIGH,       // + Both usage stats and accessibility (maximum without device admin)
        MAXIMUM     // All permissions enabled
    }
    
    /**
     * Get the current status of all kiosk-related permissions
     */
    fun getPermissionStatus(): KioskPermissionStatus {
        val accessibility = isAccessibilityServiceEnabled()
        val usageStats = isUsageStatsPermissionGranted()
        val launcher = isDefaultLauncher()
        
        // #region agent log
        DebugLogger.log("A", "KioskPermissionManager:getPermissionStatus", "Permission status", mapOf("accessibility" to accessibility, "usageStats" to usageStats, "launcher" to launcher))
        // #endregion
        
        val level = calculateProtectionLevel(accessibility, usageStats, launcher)
        
        return KioskPermissionStatus(
            isAccessibilityEnabled = accessibility,
            isUsageStatsEnabled = usageStats,
            isDeviceAdminEnabled = false, // Device admin removed
            isDefaultLauncher = launcher,
            overallProtectionLevel = level
        )
    }
    
    private fun calculateProtectionLevel(
        accessibility: Boolean,
        usageStats: Boolean,
        launcher: Boolean
    ): ProtectionLevel {
        if (!launcher) return ProtectionLevel.NONE
        
        return when {
            accessibility && usageStats -> ProtectionLevel.MAXIMUM // All 3 permissions = maximum
            accessibility || usageStats -> ProtectionLevel.MEDIUM
            else -> ProtectionLevel.BASIC
        }
    }
    
    /**
     * Check if Accessibility Service is enabled
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return KioskAccessibilityService.isServiceEnabledInSettings(context)
    }
    
    /**
     * Check if Usage Stats permission is granted
     */
    fun isUsageStatsPermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    /**
     * Check if Device Admin is enabled
     */
    fun isDeviceAdminEnabled(): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, DeviceAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(componentName)
    }
    
    /**
     * Check if our app is the default launcher
     */
    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }
    
    // ===== Intent builders for opening settings =====
    
    /**
     * Open Accessibility Settings
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Open Usage Stats Settings
     */
    fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Open Device Admin Settings to enable device admin
     */
    fun openDeviceAdminSettings() {
        // #region agent log
        DebugLogger.log("E", "KioskPermissionManager:openDeviceAdminSettings", "Attempting to open device admin settings", mapOf())
        // #endregion
        
        try {
            val componentName = ComponentName(context, DeviceAdminReceiver::class.java)
            
            // #region agent log
            DebugLogger.log("E", "KioskPermissionManager:openDeviceAdminSettings", "Component name created", mapOf("component" to componentName.flattenToString()))
            // #endregion
            
            // First try: standard ADD_DEVICE_ADMIN intent
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Разрешите права администратора устройства для защиты приложения от удаления"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Check if this intent can be handled
            val canHandle = intent.resolveActivity(context.packageManager) != null
            
            // #region agent log
            DebugLogger.log("E", "KioskPermissionManager:openDeviceAdminSettings", "Checking intent", mapOf("canHandle" to canHandle))
            // #endregion
            
            if (canHandle) {
                context.startActivity(intent)
                // #region agent log
                DebugLogger.log("E", "KioskPermissionManager:openDeviceAdminSettings", "ADD_DEVICE_ADMIN started", mapOf())
                // #endregion
            } else {
                // Fallback: open device admin list in settings
                // #region agent log
                DebugLogger.log("E", "KioskPermissionManager:openDeviceAdminSettings", "Trying fallback - device admin list", mapOf())
                // #endregion
                
                val fallbackIntent = Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.DeviceAdminSettings"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                try {
                    context.startActivity(fallbackIntent)
                    // #region agent log
                    DebugLogger.log("E", "KioskPermissionManager:openDeviceAdminSettings", "Fallback started", mapOf())
                    // #endregion
                } catch (e2: Exception) {
                    // Last resort: open general security settings
                    // #region agent log
                    DebugLogger.log("E", "KioskPermissionManager:openDeviceAdminSettings", "Trying security settings", mapOf())
                    // #endregion
                    
                    val securityIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(securityIntent)
                }
            }
        } catch (e: Exception) {
            // #region agent log
            DebugLogger.log("F", "KioskPermissionManager:openDeviceAdminSettings", "Exception occurred", mapOf("error" to e.message, "errorType" to e.javaClass.simpleName))
            // #endregion
            e.printStackTrace()
        }
    }
    
    /**
     * Open Home/Launcher Settings
     */
    fun openLauncherSettings() {
        val intents = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        )
        
        intents.firstOrNull { intent ->
            intent.resolveActivity(context.packageManager) != null
        }?.let { 
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it) 
        }
    }
    
    /**
     * Disable Device Admin (must be called before uninstalling)
     */
    fun disableDeviceAdmin() {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, DeviceAdminReceiver::class.java)
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.removeActiveAdmin(componentName)
        }
    }
}
