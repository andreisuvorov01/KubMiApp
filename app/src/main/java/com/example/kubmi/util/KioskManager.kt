package com.example.kubmi.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

object KioskManager {
    fun enableKioskMode(activity: Activity) {
        activity.window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes = attributes.apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
            }
            decorView.isLongClickable = false
            decorView.setOnLongClickListener { true }
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
        startLockTaskIfAllowed(activity)
    }

    fun startLockTaskIfAllowed(activity: Activity) {
        try {
            val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (dpm.isDeviceOwnerApp(activity.packageName)) {
                activity.startLockTask()
            }
        } catch (_: Exception) {}
    }

    fun disableKioskMode(activity: Activity) {
        activity.window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
