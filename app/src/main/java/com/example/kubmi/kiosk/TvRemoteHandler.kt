package com.example.kubmi.kiosk

import android.app.Activity
import android.util.Log
import android.view.KeyEvent

/**
 * Обработчик пультов управления для Android TV
 * 
 * Блокирует системные кнопки на пульте управления и позволяет только
 * навигационные кнопки (DPAD) и функциональные кнопки приложения
 * 
 * Поддерживается: Android 5.0+ (особенно Android TV)
 */
class TvRemoteHandler(private val activity: Activity) {
    
    companion object {
        private const val TAG = "TvRemoteHandler"
    }
    
    /**
     * Инициализировать обработчик пульта
     * Должна быть вызвана в Activity.onResume()
     */
    fun setup() {
        Log.d(TAG, "Setting up TV remote handler")
        
        // Установить слушатель на главное окно Activity
        activity.window.decorView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener handleTvRemoteKey(keyCode)
            }
            false
        }
    }
    
    /**
     * Обработка нажатия кнопки пульта
     * 
     * @return true если событие было обработано (блокировано)
     *         false если событие должно быть обработано дальше
     */
    private fun handleTvRemoteKey(keyCode: Int): Boolean {
        return when (keyCode) {
            // ============ РАЗРЕШИТЬ: Навигационные кнопки ============
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                Log.v(TAG, "Navigation key allowed: $keyCode")
                false  // Разрешить обработку
            }
            
            // ============ РАЗРЕШИТЬ: Кнопки Enter/OK ============
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B -> {
                Log.v(TAG, "Action key allowed: $keyCode")
                false  // Разрешить обработку
            }
            
            // ============ РАЗРЕШИТЬ: Мультимедийные кнопки ============
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                Log.v(TAG, "Media key allowed: $keyCode")
                false  // Разрешить обработку
            }
            
            // ============ БЛОКИРОВАТЬ: Системные кнопки ============
            KeyEvent.KEYCODE_HOME -> {
                Log.w(TAG, "Blocked: HOME button")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_BACK -> {
                Log.w(TAG, "Blocked: BACK button")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_MENU -> {
                Log.w(TAG, "Blocked: MENU button")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_SETTINGS -> {
                Log.w(TAG, "Blocked: SETTINGS button")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_POWER -> {
                Log.w(TAG, "Blocked: POWER button")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_GUIDE -> {
                Log.w(TAG, "Blocked: GUIDE button (TV)")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_INFO -> {
                Log.w(TAG, "Blocked: INFO button (TV)")
                true  // Блокировать
            }
            
            // ============ БЛОКИРОВАТЬ: Переключение входов ============
            KeyEvent.KEYCODE_TV_INPUT -> {
                Log.w(TAG, "Blocked: TV_INPUT (source switch)")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_TV_INPUT_HDMI_1,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_2,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_3,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_4 -> {
                Log.w(TAG, "Blocked: HDMI_INPUT switch")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1,
            KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1,
            KeyEvent.KEYCODE_TV_INPUT_VGA_1 -> {
                Log.w(TAG, "Blocked: Input switch (Composite/Component/VGA)")
                true  // Блокировать
            }
            
            // ============ БЛОКИРОВАТЬ: Управление ТВ ============
            KeyEvent.KEYCODE_TV_POWER -> {
                Log.w(TAG, "Blocked: TV_POWER")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                Log.w(TAG, "Blocked: CHANNEL switch")
                true  // Блокировать
            }
            
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                Log.w(TAG, "Blocked: VOLUME control")
                true  // Блокировать
            }
            
            // ============ БЛОКИРОВАТЬ: PVR/Записи ============
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_PROG_GREEN,
            KeyEvent.KEYCODE_PROG_YELLOW,
            KeyEvent.KEYCODE_PROG_BLUE -> {
                Log.w(TAG, "Blocked: Colored programmable buttons")
                true  // Блокировать
            }
            
            // ============ БЛОКИРОВАТЬ: Разные системные кнопки ============
            KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_WINDOW,
            KeyEvent.KEYCODE_ESCAPE -> {
                Log.w(TAG, "Blocked: System button ($keyCode)")
                true  // Блокировать
            }
            
            // ============ РАЗРЕШИТЬ: Остальные клавиши по умолчанию ============
            else -> {
                // Для неизвестных кнопок разрешить (может быть нужна для будущих расширений)
                Log.v(TAG, "Unknown key code: $keyCode (allowing)")
                false  // Разрешить
            }
        }
    }
    
    /**
     * Вспомогательный метод для блокировки комбинаций клавиш
     */
    fun isKeyEventBlocked(event: KeyEvent): Boolean {
        return handleTvRemoteKey(event.keyCode)
    }
    
    /**
     * Получить описание блокированной кнопки
     */
    fun getKeyDescription(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME -> "Home"
            KeyEvent.KEYCODE_BACK -> "Back"
            KeyEvent.KEYCODE_MENU -> "Menu"
            KeyEvent.KEYCODE_SETTINGS -> "Settings"
            KeyEvent.KEYCODE_POWER -> "Power"
            KeyEvent.KEYCODE_TV_POWER -> "TV Power"
            KeyEvent.KEYCODE_GUIDE -> "Guide"
            KeyEvent.KEYCODE_INFO -> "Info"
            KeyEvent.KEYCODE_TV_INPUT -> "TV Input"
            KeyEvent.KEYCODE_CHANNEL_UP -> "Channel Up"
            KeyEvent.KEYCODE_CHANNEL_DOWN -> "Channel Down"
            KeyEvent.KEYCODE_VOLUME_UP -> "Volume Up"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "Volume Down"
            KeyEvent.KEYCODE_VOLUME_MUTE -> "Volume Mute"
            KeyEvent.KEYCODE_DPAD_UP -> "DPAD Up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DPAD Down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "DPAD Left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "DPAD Right"
            KeyEvent.KEYCODE_DPAD_CENTER -> "DPAD Center"
            KeyEvent.KEYCODE_ENTER -> "Enter"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "Play"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "Pause"
            KeyEvent.KEYCODE_MEDIA_STOP -> "Stop"
            else -> "Key($keyCode)"
        }
    }
}
