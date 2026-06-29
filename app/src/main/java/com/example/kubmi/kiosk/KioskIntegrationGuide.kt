package com.example.kubmi

import android.app.Activity
import android.view.KeyEvent
import android.util.Log
import com.example.kubmi.kiosk.AdvancedKioskManager
import com.example.kubmi.kiosk.TvRemoteHandler
import com.example.kubmi.kiosk.SettingsBlocker

/**
 * Интеграция Kiosk Mode защиты в MainActivity
 * 
 * Эта утилита класса показывает как интегрировать все компоненты защиты
 * Используйте этот код как шаблон для вашей Activity
 */
object KioskIntegrationExample {
    
    private const val TAG = "KioskIntegration"
    
    /**
     * Вызвать это в Activity.onCreate()
     */
    fun setupKioskMode(
        activity: Activity,
        kioskManager: AdvancedKioskManager
    ) {
        Log.d(TAG, "Setting up Kiosk Mode protection")
        
        // 1. Проверить Device Owner статус и применить конфигурацию
        if (kioskManager.isDeviceOwner()) {
            Log.i(TAG, "Device Owner detected - applying full protection")
            kioskManager.setupFullKioskMode()
            
            // Опционально: Конфигурация для OEM панелей
            // kioskManager.configureForInteractivePanel("BENQ")
            
        } else {
            Log.i(TAG, "Device Owner NOT detected - applying limited protection")
            // setupLimitedKioskMode is private, so just call setupFullKioskMode
            kioskManager.setupFullKioskMode()
        }
        
        // 2. Запустить Lock Task Mode
        kioskManager.startLockTask(activity)
        
        // 3. Установить обработчик пультов (для Android TV)
        setupTvRemoteHandling(activity)
        
        // 4. Установить блокировку Settings
        setupSettingsBlocking(activity)
        
        // 5. Вывести статус защиты
        logProtectionStatus(kioskManager)
    }
    
    /**
     * Обработка пультов Android TV
     */
    private fun setupTvRemoteHandling(activity: Activity) {
        Log.d(TAG, "Setting up TV remote handler")
        
        // Создать и настроить обработчик пульта
        val tvRemoteHandler = TvRemoteHandler(activity)
        tvRemoteHandler.setup()
        
        // Переопределить в Activity.onKeyDown():
        // override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        //     return tvRemoteHandler.handleTvRemoteKey(keyCode) || 
        //            super.onKeyDown(keyCode, event)
        // }
    }
    
    /**
     * Блокировка Intent-ов на Settings
     */
    private fun setupSettingsBlocking(activity: Activity) {
        Log.d(TAG, "Setting up Settings blocker")
        
        val settingsBlocker = SettingsBlocker(activity)
        settingsBlocker.initialize()
        
        // Сохранить для использования в onDestroy()
        // В Activity добавить:
        // override fun onDestroy() {
        //     settingsBlocker.unregister()
        //     super.onDestroy()
        // }
    }
    
    /**
     * Вывести статус защиты в логи
     */
    private fun logProtectionStatus(kioskManager: AdvancedKioskManager) {
        val status = kioskManager.getProtectionStatus()
        Log.i(TAG, status)
        
        // Можно также вывести на экран для отладки
        // showStatusDialog(status)
    }
}
/**
 * Пример правильной интеграции в MainActivity
 * 
 * СКОПИРУЙТЕ ЭТО В ВАШУ MainActivity и адаптируйте под свои нужды
 */
/*
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    private lateinit var kioskManager: AdvancedKioskManager
    
    private lateinit var tvRemoteHandler: TvRemoteHandler
    private lateinit var settingsBlocker: SettingsBlocker
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализировать Kiosk защиту
        initializeKioskProtection()
        
        // Инициализировать UI...
        setContentView(R.layout.activity_main)
    }
    
    private fun initializeKioskProtection() {
        Log.d(TAG, "Initializing Kiosk protection")
        
        try {
            // 1. Полная инициализация Device Owner
            if (kioskManager.isDeviceOwner()) {
                Log.i(TAG, "✓ Device Owner detected")
                kioskManager.setupFullKioskMode()
                
                // Конфигурация для OEM панелей (опционально)
                // detectAndConfigureOem()
                
            } else {
                Log.w(TAG, "⚠ Device Owner NOT detected - limited mode")
                kioskManager.setupLimitedKioskMode()
            }
            
            // 2. Инициализировать обработчик пультов
            tvRemoteHandler = TvRemoteHandler(this)
            tvRemoteHandler.setup()
            Log.d(TAG, "✓ TV remote handler initialized")
            
            // 3. Инициализировать блокировку Settings
            settingsBlocker = SettingsBlocker(this)
            settingsBlocker.initialize()
            Log.d(TAG, "✓ Settings blocker initialized")
            
            // 4. Запустить Lock Task Mode
            kioskManager.startLockTask(this)
            Log.d(TAG, "✓ Lock Task mode started")
            
            // 5. Вывести статус в лог
            val status = kioskManager.getProtectionStatus()
            Log.i(TAG, status)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during kiosk protection setup", e)
        }
    }
    
    /**
     * Обработка нажатия кнопок (включая пульт Android TV)
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Обработать пультом Android TV
        return tvRemoteHandler.handleTvRemoteKey(keyCode) || 
               super.onKeyDown(keyCode, event)
    }
    
    /**
     * Обработка выхода приложения
     */
    override fun onBackPressed() {
        // Переопределить для предотвращения выхода из приложения
        // или показать диалог подтверждения
        Log.w(TAG, "Back button pressed - blocked")
        // super.onBackPressed()  // НЕ вызываем - блокируем выход
    }
    
    /**
     * Очистка при уничтожении Activity
     */
    override fun onDestroy() {
        settingsBlocker.unregister()
        kioskManager.stopLockTask(this)
        super.onDestroy()
    }
    
    /**
     * Определение типа OEM панели и применение конфигурации
     */
    private fun detectAndConfigureOem() {
        val manufacturer = android.os.Build.MANUFACTURER.uppercase()
        val model = android.os.Build.MODEL.uppercase()
        
        Log.d(TAG, "Detecting OEM panel: $manufacturer / $model")
        
        val oemType = when {
            manufacturer.contains("BENQ") -> "BENQ"
            manufacturer.contains("PROMETHEAN") -> "PROMETHEAN"
            manufacturer.contains("NEWLINE") -> "NEWLINE"
            manufacturer.contains("CLEVERTOUCH") -> "CLEVERTOUCH"
            manufacturer.contains("HIKVISION") -> "HIKVISION"
            manufacturer.contains("VIEWSONIC") -> "VIEWSONIC"
            model.contains("SMART") -> "SMART"
            else -> "GENERIC"
        }
        
        Log.i(TAG, "Configuring for OEM type: $oemType")
        kioskManager.configureForInteractivePanel(oemType)
    }
}
*/

/**
 * Android TV специфичная конфигурация
 * 
 * Используйте это если приложение работает на Android TV
 */
object AndroidTvKioskSetup {
    
    private const val TAG = "AndroidTvSetup"
    
    /**
     * Применить конфигурацию для Android TV
     */
    fun configureForAndroidTv(
        activity: Activity,
        kioskManager: AdvancedKioskManager
    ) {
        Log.d(TAG, "Configuring for Android TV")
        
        // 1. Применить Device Owner конфигурацию
        if (kioskManager.isDeviceOwner()) {
            kioskManager.setupFullKioskMode()
        }
        
        // 4. Скрыть системные элементы
        hideSystemUIElements(activity)
    }
    
    /**
     * Скрыть статус бар и навигационный бар для полноэкранного режима
     */
    fun hideSystemUIElements(activity: Activity) {
        val decorView = activity.window.decorView
        
        // Установить флаги для полноэкранного режима
        val flags = android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                   android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                   android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                   android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                   android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        
        decorView.systemUiVisibility = flags
        
        // Запретить выбитие флагов
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and android.view.View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                decorView.systemUiVisibility = flags
            }
        }
        
        Log.d(TAG, "System UI elements hidden")
    }
}

/**
 * Интерактивные панели специфичная конфигурация
 */
object InteractivePanelSetup {
    
    private const val TAG = "InteractivePanelSetup"
    
    /**
     * Применить конфигурацию для интерактивной панели
     */
    fun configureForInteractivePanel(
        activity: Activity,
        kioskManager: AdvancedKioskManager
    ) {
        Log.d(TAG, "Configuring for Interactive Panel")
        
        // 1. Обнаружить производителя
        val oemType = detectPanelManufacturer()
        Log.i(TAG, "Detected panel type: $oemType")
        
        // 2. Применить Device Owner ограничения
        if (kioskManager.isDeviceOwner()) {
            kioskManager.setupFullKioskMode()
        }
        
        // 2. Настроить обработчик касаний и кнопок
        setupPanelControls(activity, kioskManager)
    }
    
    private fun detectPanelManufacturer(): String {
        val manufacturer = android.os.Build.MANUFACTURER.uppercase()
        val model = android.os.Build.MODEL.uppercase()
        
        return when {
            manufacturer.contains("BENQ") || model.contains("BENQ") -> "BENQ"
            manufacturer.contains("PROMETHEAN") -> "PROMETHEAN"
            manufacturer.contains("NEWLINE") -> "NEWLINE"
            manufacturer.contains("CLEVERTOUCH") -> "CLEVERTOUCH"
            manufacturer.contains("HIKVISION") -> "HIKVISION"
            manufacturer.contains("VIEWSONIC") -> "VIEWSONIC"
            model.contains("SMART") || manufacturer.contains("SMARTTECH") -> "SMART"
            else -> "GENERIC"
        }
    }
    
    private fun setupPanelControls(
        activity: Activity,
        kioskManager: AdvancedKioskManager
    ) {
        // Настроить обработчик кнопок на панели (если есть)
        val tvRemoteHandler = TvRemoteHandler(activity)
        tvRemoteHandler.setup()
        
        Log.d(TAG, "Panel controls configured")
    }
}
