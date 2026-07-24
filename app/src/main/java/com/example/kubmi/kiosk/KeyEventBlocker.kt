package com.example.kubmi.kiosk

import android.view.KeyEvent

object KeyEventBlocker {

    @Volatile
    var paused: Boolean = false

    /** Пока true — KioskService не пытается вернуть приложение на передний план */
    @Volatile
    var foregroundReturnPaused: Boolean = false

    private val blockedKeys = setOf(
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_APP_SWITCH,
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_SETTINGS,
        KeyEvent.KEYCODE_SEARCH,
        KeyEvent.KEYCODE_ASSIST,
        KeyEvent.KEYCODE_VOICE_ASSIST,
        KeyEvent.KEYCODE_WINDOW,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_F11,
        KeyEvent.KEYCODE_F12,
        KeyEvent.KEYCODE_SYSRQ,
        KeyEvent.KEYCODE_GUIDE,
        KeyEvent.KEYCODE_TV_INPUT,
        KeyEvent.KEYCODE_TV_POWER,
        KeyEvent.KEYCODE_CHANNEL_UP,
        KeyEvent.KEYCODE_CHANNEL_DOWN,
        KeyEvent.KEYCODE_TV_INPUT_HDMI_1,
        KeyEvent.KEYCODE_TV_INPUT_HDMI_2,
        KeyEvent.KEYCODE_TV_INPUT_HDMI_3,
        KeyEvent.KEYCODE_TV_INPUT_HDMI_4,
        KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1,
        KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1,
        KeyEvent.KEYCODE_TV_INPUT_VGA_1
    )
    
    fun shouldBlockKey(event: KeyEvent): Boolean {
        if (paused) return false
        // Блокировка комбинаций клавиш
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_W,      // Ctrl+W
                KeyEvent.KEYCODE_Q,      // Ctrl+Q
                KeyEvent.KEYCODE_TAB,    // Alt+Tab
                KeyEvent.KEYCODE_F4,     // Alt+F4
                KeyEvent.KEYCODE_L,      // Ctrl+L
                KeyEvent.KEYCODE_N,      // Ctrl+N
                KeyEvent.KEYCODE_T,      // Ctrl+T
                KeyEvent.KEYCODE_R,      // Ctrl+R
                KeyEvent.KEYCODE_P,      // Ctrl+P
                KeyEvent.KEYCODE_O,      // Ctrl+O
                KeyEvent.KEYCODE_U,      // Ctrl+U
                KeyEvent.KEYCODE_D,      // Meta+D
                KeyEvent.KEYCODE_M -> true // Meta+M
                else -> false
            }
        }
        
        // Блокировка Ctrl+Shift комбинаций
        if (event.isCtrlPressed && event.isShiftPressed) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_I,      // Ctrl+Shift+I (DevTools)
                KeyEvent.KEYCODE_J -> true // Ctrl+Shift+J (Console)
                else -> false
            }
        }
        
        // Блокировка Alt+Left для Android TV
        if (event.isAltPressed && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            return true
        }
        
        // Блокировка отдельных клавиш
        return event.keyCode in blockedKeys
    }
}
