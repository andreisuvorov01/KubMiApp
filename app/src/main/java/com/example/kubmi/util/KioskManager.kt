package com.example.kubmi.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.app.UiModeManager
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object KioskManager {
    /**
     * Enables aggressive kiosk mode UI by hiding system bars and keeping screen on.
     */
    fun enableKioskMode(activity: Activity) {
        activity.window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
                val controller = decorView.windowInsetsController
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
        }
        
        // Also use WindowInsetsControllerCompat for compatibility
        val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Disables kiosk mode UI.
     */
    fun disableKioskMode(activity: Activity) {
        activity.window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(true)
                val controller = decorView.windowInsetsController
                controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        
        val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Checks if the app is the Device Owner.
     */
    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    /**
     * Starts Lock Task mode (Screen Pinning).
     * If the app is set as Device Owner, this will be silent and un-escapable.
     * Otherwise, it will show a toast and can be escaped by holding Back+Recents.
     * On Android TV, Lock Task may not be available or behave differently, so we handle TV separately.
     */
    fun startLockTask(activity: Activity) {
        // Check if this is a TV device - Lock Task is generally not available or not recommended on TV
        val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            DebugLogger.log("K", "KioskManager:startLockTask", "Lock Task not started - running on TV device", mapOf())
            return
        }
        
        try {
            val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                    activity.startLockTask()
                }
            } else {
                @Suppress("DEPRECATION")
                if (!activityManager.isInLockTaskMode) {
                    activity.startLockTask()
                }
            }
        } catch (e: Exception) {
            DebugLogger.log("K", "KioskManager:startLockTask", "Failed to start lock task", mapOf("error" to e.message))
        }
    }

    /**
     * Stops Lock Task mode.
     */
    fun stopLockTask(activity: Activity) {
        // Check if this is a TV device - Lock Task is generally not available or not recommended on TV
        val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            DebugLogger.log("K", "KioskManager:stopLockTask", "Lock Task not stopped - running on TV device", mapOf())
            return
        }
        
        try {
            val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                    activity.stopLockTask()
                }
            } else {
                @Suppress("DEPRECATION")
                if (activityManager.isInLockTaskMode) {
                    activity.stopLockTask()
                }
            }
        } catch (e: Exception) {
            DebugLogger.log("K", "KioskManager:stopLockTask", "Failed to stop lock task", mapOf("error" to e.message))
        }
    }

    /**
     * Checks if Lock Task mode (Screen Pinning) is currently active.
     */
    fun isLockTaskActive(activity: Activity): Boolean {
        // Check if this is a TV device - Lock Task is generally not available or not recommended on TV
        val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            DebugLogger.log("K", "KioskManager:isLockTaskActive", "Lock Task not checked - running on TV device", mapOf())
            return false
        }
        
        return try {
            val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
            } else {
                @Suppress("DEPRECATION")
                activityManager.isInLockTaskMode
            }
        } catch (e: Exception) {
            DebugLogger.log("K", "KioskManager:isLockTaskActive", "Failed to check lock task state", mapOf("error" to e.message))
            false
        }
    }
}
