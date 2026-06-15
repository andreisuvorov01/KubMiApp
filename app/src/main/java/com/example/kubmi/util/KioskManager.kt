package com.example.kubmi.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import timber.log.Timber

object KioskManager {
    private val blockedKeyCodes = setOf(
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_APP_SWITCH,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_F11,
        KeyEvent.KEYCODE_F12,
        KeyEvent.KEYCODE_WINDOW,
        KeyEvent.KEYCODE_SYSRQ,
        KeyEvent.KEYCODE_FORWARD,
        KeyEvent.KEYCODE_REFRESH,
        KeyEvent.KEYCODE_MOVE_HOME,
        KeyEvent.KEYCODE_TV_POWER,
        KeyEvent.KEYCODE_POWER
    )

    fun enableKioskMode(activity: Activity) {
        activity.window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            decorView.setOnSystemUiVisibilityChangeListener {
                Timber.w("Kiosk system UI visibility changed: %s", it)
                decorView.postDelayed({ enableKioskMode(activity) }, 300)
            }
            decorView.setOnLongClickListener {
                Timber.w("Blocked long press in kiosk mode")
                true
            }
            decorView.setOnContextClickListener {
                Timber.w("Blocked context click in kiosk mode")
                true
            }
            decorView.isHapticFeedbackEnabled = false
            decorView.systemUiVisibility = legacyImmersiveFlags()
            hideSystemBars(activity)
        }
        runCatching { activity.startLockTask() }
            .onSuccess { Timber.i("Android lock task requested") }
            .onFailure { Timber.w(it, "Android lock task unavailable; continue with immersive kiosk fallback") }
    }

    fun disableKioskMode(activity: Activity) {
        runCatching { activity.stopLockTask() }
        activity.window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            decorView.setOnSystemUiVisibilityChangeListener(null)
            decorView.setOnLongClickListener(null)
            decorView.setOnContextClickListener(null)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController?.show(WindowInsets.Type.systemBars())
            }
        }
    }

    fun isBlockedKey(event: KeyEvent?): Boolean {
        if (event == null) return false
        val keyCode = event.keyCode
        val ctrl = event.isCtrlPressed
        val shift = event.isShiftPressed
        val alt = event.isAltPressed
        val blocked = keyCode in blockedKeyCodes ||
            (alt && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ||
            (alt && keyCode == KeyEvent.KEYCODE_TAB) ||
            (alt && keyCode == KeyEvent.KEYCODE_F4) ||
            (ctrl && keyCode in setOf(KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_U)) ||
            (ctrl && shift && keyCode in setOf(KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_ESCAPE))
        if (blocked) Timber.w("Blocked kiosk key event: keyCode=%d ctrl=%s shift=%s alt=%s", keyCode, ctrl, shift, alt)
        return blocked
    }

    fun handleTouchEvent(event: MotionEvent?): Boolean {
        val blocked = event?.buttonState?.and(MotionEvent.BUTTON_SECONDARY) == MotionEvent.BUTTON_SECONDARY
        if (blocked) Timber.w("Blocked secondary touch/mouse action in kiosk mode")
        return blocked
    }

    fun restoreFocus(activity: Activity) {
        Timber.w("Restoring kiosk focus")
        activity.window.decorView.requestFocus()
        enableKioskMode(activity)
    }

    fun isInLockTaskMode(activity: Activity): Boolean {
        val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        } else {
            @Suppress("DEPRECATION")
            manager.isInLockTaskMode
        }
    }

    private fun legacyImmersiveFlags(): Int = (
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

    private fun hideSystemBars(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}
