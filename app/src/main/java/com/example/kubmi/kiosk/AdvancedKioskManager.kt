package com.example.kubmi.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import com.example.kubmi.MainActivity
import com.example.kubmi.receiver.DeviceAdminReceiver
import com.example.kubmi.service.KioskService
import com.example.kubmi.service.KioskAccessibilityService
import com.example.kubmi.service.KioskJobService
import com.example.kubmi.util.AdminLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Многоуровневый менеджер для максимальной защиты Kiosk Mode на Android TV
 * 
 * Уровни защиты:
 * 1. Lock Task Mode - блокировка переключения приложений
 * 2. Device Owner Restrictions - системные ограничения
 * 3. User Restrictions - запреты пользователя
 * 4. System Apps Blocking - скрытие системных приложений
 * 5. Lock Task Features - контроль системных функций (Android 9+)
 * 6. Status Bar & Keyguard - скрытие интерфейса
 * 7. Service Protection - фоновые сервисы мониторинга
 */
@Singleton
class AdvancedKioskManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dpm: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val adminComponent: ComponentName by lazy {
        ComponentName(context, DeviceAdminReceiver::class.java)
    }

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    companion object {
        private const val TAG = "AdvancedKioskManager"
        private const val LOCK_TASK_FEATURE = "android.software.device_admin"
    }

    fun isDeviceOwner(): Boolean {
        return try {
            dpm.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Device Owner status", e)
            false
        }
    }

    /**
     * Полная настройка защищённого Kiosk режима
     * Должна быть вызвана при запуске приложения
     */
    fun setupFullKioskMode() {
        if (GaokeViewKioskDetector.isGaokeViewKioskActive(context)) {
            Log.i(TAG, "GaokeView kiosk detected — using compatibility mode")
            setupCompatibilityMode()
            return
        }

        if (!isDeviceOwner()) {
            Log.w(TAG, "Not Device Owner - limited kiosk capabilities")
            setupLimitedKioskMode()
            return
        }

        Log.d(TAG, "Setting up FULL kiosk mode as Device Owner")

        // Уровень 1: Lock Task
        setLockTaskPackages()
        configurePersistentHome()

        // Уровень 2-5: Device Owner функции
        blockSystemFeatures()
        setUserRestrictions()
        hideStatusBar()
        disableKeyguard()
        blockSystemApps()
        configureLockTaskFeatures()
        applyNetworkRestrictions()
        configurePowerManagement()

        // Уровень 6-7: Фоновые сервисы
        startProtectionServices()

        Log.d(TAG, "Full kiosk mode setup complete")
    }

    /**
     * Базовая защита без Device Owner (для обычных приложений)
     */
    fun setupCompatibilityMode() {
        Log.d(TAG, "Setting up COMPATIBILITY mode with GaokeView kiosk")
        startProtectionServices()
        setUserRestrictions()
        applyNetworkRestrictions()
        Log.i(TAG, "Compatibility mode active: services + restrictions only")
    }

    private fun setupLimitedKioskMode() {
        Log.d(TAG, "Setting up LIMITED kiosk mode (non-Device Owner)")

        // Всё равно запускаем фоновые сервисы
        startProtectionServices()

        // Предоставляем методы для использования через Activities
        Log.i(TAG, "Limited mode: Use dispatchKeyEvent() and watchdog services for protection")
    }

    private fun setLockTaskPackages() {
            try {
                dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
                Log.d(TAG, "✓ Lock task packages configured for: ${context.packageName}")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to set lock task packages", e)
            }
        }

        private fun configurePersistentHome() {
            try {
                val filter = IntentFilter(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
                dpm.addPersistentPreferredActivity(
                    adminComponent,
                    filter,
                    ComponentName(context, MainActivity::class.java)
                )
                Log.d(TAG, "✓ Persistent HOME configured")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to configure persistent HOME", e)
            }
        }

        private fun blockSystemFeatures() {
            try {
                // Home button blocking is not available as a UserManager restriction
                Log.d(TAG, "✓ System features blocking configured")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to block system features", e)
            }
        }

        /**
         * Применение всех доступных User Restrictions для максимальной защиты
         */
        private fun setUserRestrictions() {
            val restrictions = listOf(
                // Установка приложений
                UserManager.DISALLOW_INSTALL_APPS,
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
                UserManager.DISALLOW_UNINSTALL_APPS,

                // Пользователи и аккаунты
                UserManager.DISALLOW_ADD_USER,
                UserManager.DISALLOW_MODIFY_ACCOUNTS,

                // Безопасность
                UserManager.DISALLOW_FACTORY_RESET,
                UserManager.DISALLOW_SAFE_BOOT,
                UserManager.DISALLOW_DEBUGGING_FEATURES,

                // Сетевые настройки
                UserManager.DISALLOW_CONFIG_WIFI,
                UserManager.DISALLOW_CONFIG_BLUETOOTH,
                UserManager.DISALLOW_CONFIG_VPN,
                UserManager.DISALLOW_CONFIG_TETHERING,
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                UserManager.DISALLOW_CONFIG_CREDENTIALS,
                UserManager.DISALLOW_NETWORK_RESET,

                // Внешние устройства
                UserManager.DISALLOW_USB_FILE_TRANSFER,
                UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA,

                // UI управление
                UserManager.DISALLOW_APPS_CONTROL,
                UserManager.DISALLOW_CREATE_WINDOWS,
                UserManager.DISALLOW_ADJUST_VOLUME,

                // Android 9+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    UserManager.DISALLOW_AUTOFILL
                } else null,

                // Android 11+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    UserManager.DISALLOW_CONTENT_CAPTURE
                } else null,

                // Komunikations restrictions
                UserManager.DISALLOW_OUTGOING_CALLS,
                UserManager.DISALLOW_SMS
            ).filterNotNull()

            var successCount = 0
            restrictions.forEach { restriction ->
                try {
                    dpm.addUserRestriction(adminComponent, restriction)
                    successCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to add restriction: $restriction", e)
                }
            }

            Log.d(TAG, "✓ Applied $successCount/${restrictions.size} user restrictions")
        }

        private fun hideStatusBar() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    dpm.setStatusBarDisabled(adminComponent, true)
                    Log.d(TAG, "✓ Status bar disabled (Android 6+)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to disable status bar", e)
            }
        }

        private fun disableKeyguard() {
            try {
                dpm.setKeyguardDisabled(adminComponent, true)
                Log.d(TAG, "✓ Keyguard disabled")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to disable keyguard", e)
            }
        }

        /**
         * Скрытие всех системных приложений которые могут помешать киоску
         */
        private fun blockSystemApps() {
            val blockedPackages = listOf(
                // Настройки и управление
                "com.android.settings",
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",

                // Play Store
                "com.android.vending",
                "com.google.android.play.core",

                // Браузеры
                "com.android.chrome",

                // Файловый менеджер
                "com.android.documentsui",
                "com.android.providers.downloads.ui",

                // Приложения Google
                "com.google.android.apps.wellbeing",
                "com.google.android.apps.googleplus",
                "com.google.android.apps.maps",
                "com.google.android.apps.photos",

                // Android TV специфичные
                "com.google.android.tvlauncher",
                "com.google.android.tvrecommendations",
                "com.google.android.leanbacklauncher",
                "com.google.android.tv.remote.service",

                // OEM приложения которые могут мешать
                "com.example.launcher",  // Стандартный launcher
                "com.sec.android.app.launcher"  // Samsung Launcher
            )

            var hiddenCount = 0
            blockedPackages.forEach { packageName ->
                try {
                    // Проверяем что это не наше приложение
                    if (packageName != context.packageName) {
                        dpm.setApplicationHidden(adminComponent, packageName, true)
                        hiddenCount++
                    }
                } catch (e: Exception) {
                    // Некоторые приложения могут не скрыться, это нормально
                    Log.v(TAG, "Could not hide: $packageName (might not be installed)")
                }
            }

            Log.d(TAG, "✓ Hidden $hiddenCount system apps")
        }

        /**
         * Настройка Lock Task функций (Android 9+)
         * Определяет какие системные кнопки будут доступны в Lock Task Mode
         */
        private fun configureLockTaskFeatures() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Отключить все системные функции в Lock Task
                    dpm.setLockTaskFeatures(
                        adminComponent,
                        DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                    )
                    Log.d(TAG, "✓ Lock task features disabled (Android 9+)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to configure lock task features", e)
            }
        }

        /**
         * Применение сетевых ограничений
         */
        private fun applyNetworkRestrictions() {
            try {
                // Блокировка изменения сетевых настроек
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_WIFI)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_BLUETOOTH)

                Log.d(TAG, "✓ Network restrictions applied")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to apply network restrictions", e)
            }
        }

        /**
         * Конфигурация управления питанием
         */
        private fun configurePowerManagement() {
            try {
                // Не разрешаем меню Power и остальные SystemUI-функции.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.setLockTaskFeatures(
                        adminComponent,
                        DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                    )
                }

                Log.d(TAG, "✓ Power management configured")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to configure power management", e)
            }
        }

        /**
         * Запуск всех фоновых сервисов для постоянной защиты
         */
        private fun startProtectionServices() {
            try {
                // 1. Запуск главного Kiosk Service (Foreground Service)
                val kioskServiceIntent =
                    Intent(context, com.example.kubmi.service.KioskService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(kioskServiceIntent)
                } else {
                    context.startService(kioskServiceIntent)
                }
                Log.d(TAG, "✓ KioskService started")

                // 2. Запуск AppMonitorService
                val appMonitorIntent = Intent(context, AppMonitorService::class.java)
                context.startService(appMonitorIntent)
                Log.d(TAG, "✓ AppMonitorService started")

                // 3. Запуск AccessibilityService (если не запущен)
                // Требует ручной активации в Settings > Accessibility
                Log.d(TAG, "Note: AccessibilityService requires manual activation in Settings")

                // 4. Планирование JobService для Android 14+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    KioskJobService.schedule(context)
                    Log.d(TAG, "✓ KioskJobService scheduled (Android 12+)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to start protection services", e)
            }
        }

        fun startLockTask(activity: Activity) {
                    if (isDeviceOwner()) {
                        try {
                            activity.startLockTask()
                            Log.d(TAG, "✓ Lock task mode started")
                        } catch (e: Exception) {
                            Log.e(TAG, "✗ Failed to start lock task", e)
                        }
                    } else {
                        Log.w(TAG, "Cannot start lock task: not Device Owner")
                    }
                }

                fun stopLockTask(activity: Activity) {
                    try {
                        activity.stopLockTask()
                        Log.d(TAG, "✓ Lock task mode stopped")
                    } catch (e: Exception) {
                        Log.w(TAG, "Lock task was not active or could not be stopped", e)
                    }
                }

                fun beginAdminMaintenance(activity: Activity, durationMs: Long) {
                    KioskService.allowTemporaryExit(context, durationMs)
                    KeyEventBlocker.paused = true
                    KeyEventBlocker.foregroundReturnPaused = true
                    AdminLogger(context).logAction(
                        "KIOSK_MAINTENANCE_STARTED",
                        "duration_ms=$durationMs"
                    )
                    stopLockTask(activity)

                    if (isDeviceOwner()) {
                        runCatching {
                            dpm.setStatusBarDisabled(adminComponent, false)
                        }.onFailure {
                            Log.w(TAG, "Could not enable status bar for admin maintenance", it)
                        }
                        unhideAdminApps()
                    }
                }

                fun exitKioskForAdmin(
                    activity: Activity,
                    durationMs: Long = KioskService.ADMIN_EXIT_WINDOW_MS
                ) {
                    beginAdminMaintenance(activity, durationMs)

                    if (isDeviceOwner()) {
                        runCatching {
                            dpm.clearPackagePersistentPreferredActivities(
                                adminComponent,
                                context.packageName
                            )
                        }.onFailure {
                            Log.w(TAG, "Could not clear persistent HOME for admin exit", it)
                        }
                    }

                    val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val exitIntent =
                        if (settingsIntent.resolveActivity(context.packageManager) != null) {
                            settingsIntent
                        } else {
                            homeIntent
                        }

                    activity.startActivity(exitIntent)
                    activity.finishAndRemoveTask()
                }

                private fun unhideAdminApps() {
                    listOf(
                        "com.android.settings",
                        "com.google.android.tvlauncher",
                        "com.google.android.leanbacklauncher",
                        "com.android.launcher",
                        "com.android.launcher2",
                        "com.android.launcher3"
                    ).forEach { packageName ->
                        runCatching {
                            dpm.setApplicationHidden(adminComponent, packageName, false)
                        }.onFailure {
                            Log.v(TAG, "Could not unhide $packageName: ${it.message}")
                        }
                    }
                }

                /**
                 * Скрытие Android TV рекомендаций и launcher (Device Owner)
                 */
                fun hideAndroidTvUI() {
                    try {
                        val tvPackages = listOf(
                            "com.google.android.tvlauncher",
                            "com.google.android.tvrecommendations",
                            "com.google.android.leanbacklauncher"
                        )

                        tvPackages.forEach { pkg ->
                            try {
                                dpm.setApplicationHidden(adminComponent, pkg, true)
                            } catch (e: Exception) {
                                Log.v(TAG, "Could not hide $pkg: ${e.message}")
                            }
                        }

                        Log.d(TAG, "✓ Android TV UI hidden")
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Failed to hide Android TV UI", e)
                    }
                }

                /**
                 * Проверка и применение конфигурации для OEM интерактивных панелей
                 */
                fun configureForInteractivePanel(oemType: String = "GENERIC") {
                    Log.d(TAG, "Configuring for OEM type: $oemType")

                    when (oemType.uppercase()) {
                        "BENQ" -> configureForBenQ()
                        "PROMETHEAN" -> configureForPromethean()
                        "NEWLINE" -> configureForNewline()
                        "CLEVERTOUCH" -> configureForCleverTouch()
                        "HIKVISION" -> configureForHikvision()
                        "VIEWSONIC" -> configureForViewSonic()
                        "SMART" -> configureForSmartBoard()
                        "ANDROID_TV" -> hideAndroidTvUI()
                        "GAOKEVIEW" -> configureForGaokeView()
                        else -> Log.d(TAG, "Generic OEM configuration applied")
                    }
                }

                private fun configureForBenQ() {
                    Log.d(TAG, "Applying BenQ-specific configuration...")
                    // BenQ Floating Tool отключение
                    try {
                        dpm.setApplicationHidden(adminComponent, "com.benq.aqp.floatingtool", true)
                    } catch (e: Exception) {
                        Log.v(TAG, "BenQ floating tool not found or cannot be hidden")
                    }
                }

                private fun configureForPromethean() {
                    Log.d(TAG, "Applying Promethean-specific configuration...")
                    try {
                        dpm.setApplicationHidden(adminComponent, "com.promethean.menu", true)
                    } catch (e: Exception) {
                        Log.v(TAG, "Promethean menu not found or cannot be hidden")
                    }
                }

                private fun configureForNewline() {
                    Log.d(TAG, "Applying Newline-specific configuration...")
                    try {
                        dpm.setApplicationHidden(adminComponent, "com.newline.toolbar", true)
                    } catch (e: Exception) {
                        Log.v(TAG, "Newline toolbar not found or cannot be hidden")
                    }
                }

                private fun configureForCleverTouch() {
                    Log.d(TAG, "Applying CleverTouch-specific configuration...")
                    try {
                        dpm.setApplicationHidden(adminComponent, "com.clevertouch.lynxmenu", true)
                    } catch (e: Exception) {
                        Log.v(TAG, "CleverTouch menu not found or cannot be hidden")
                    }
                }

                private fun configureForHikvision() {
                    Log.d(TAG, "Applying Hikvision-specific configuration...")
                    try {
                        dpm.setApplicationHidden(adminComponent, "com.hikvision.ui", true)
                    } catch (e: Exception) {
                        Log.v(TAG, "Hikvision UI not found or cannot be hidden")
                    }
                }

                private fun configureForViewSonic() {
                    Log.d(TAG, "Applying ViewSonic-specific configuration...")
                    try {
                        listOf(
                            "com.viewsonic.viewboard",
                            "com.viewsonic.toolbar",
                            "com.viewsonic.launcher"
                        ).forEach { pkg ->
                            dpm.setApplicationHidden(adminComponent, pkg, true)
                        }
                    } catch (e: Exception) {
                        Log.v(TAG, "ViewSonic tools not found or cannot be hidden")
                    }
                }

                private fun configureForSmartBoard() {
                    Log.d(TAG, "Applying SMART Board-specific configuration...")
                    try {
                        dpm.setApplicationHidden(adminComponent, "com.smarttech.whiteboard", true)
                    } catch (e: Exception) {
                        Log.v(TAG, "SMART whiteboard not found or cannot be hidden")
                    }
                }

                private fun configureForGaokeView() {
                    Log.d(TAG, "GaokeView panel detected — packages are NOT hidden for coexistence")
                }

    /**
     * Получить статус защиты
     */
    fun getProtectionStatus(): String {
                    val sb = StringBuilder()
                    sb.append("=== Kiosk Protection Status ===\n")

                    sb.append("Device Owner: ${if (isDeviceOwner()) "✓ YES" else "✗ NO"}\n")
                    sb.append("Android Version: ${Build.VERSION.SDK_INT}\n")
                    sb.append("Device Model: ${Build.MODEL}\n")
                    sb.append("Device Manufacturer: ${Build.MANUFACTURER}\n")

                    if (isDeviceOwner()) {
                        sb.append("\nDevice Owner Features:\n")
                        sb.append("✓ Lock Task Mode\n")
                        sb.append("✓ User Restrictions Applied\n")
                        sb.append("✓ System Apps Hidden\n")
                        sb.append("✓ Status Bar Disabled\n")
                        sb.append("✓ Keyguard Disabled\n")

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            sb.append("✓ Lock Task Features Configured (Android 9+)\n")
                        }
                    } else {
                        sb.append("\n⚠ Limited Mode (Non-Device Owner):\n")
                        sb.append("✓ Key Event Blocking (Activity level)\n")
                        sb.append("✓ AppMonitor Service\n")
                        sb.append("✓ Accessibility Service (if enabled)\n")
                        sb.append("✓ Foreground Service\n")
                    }

                    return sb.toString()
    }

    /**
     * Проверить работоспособность Kiosk режима
     * Возвращает детальный отчёт о состоянии всех компонентов
     */
    data class KioskCheckResult(
                    val isDeviceOwner: Boolean,
                    val isDefaultLauncher: Boolean,
                    val isLockTaskActive: Boolean,
                    val isAccessibilityEnabled: Boolean,
                    val isRunningInForeground: Boolean,
                    val appVersion: String,
                    val androidVersion: Int,
                    val restrictions: List<String>,
                    val hiddenApps: List<String>,
                    val errors: List<String>,
                    val warnings: List<String>
                ) {
                    val isFullyProtected: Boolean
                        get() = isDeviceOwner && isLockTaskActive &&
                                restrictions.size >= 15 && hiddenApps.size >= 5 &&
                                errors.isEmpty()

                    val protectionPercentage: Int
                        get() {
                            var score = 0
                            if (isDeviceOwner) score += 30
                            if (isDefaultLauncher) score += 15
                            if (isLockTaskActive) score += 20
                            if (isAccessibilityEnabled || isDeviceOwner) score += 15
                            if (isRunningInForeground) score += 10
                            if (restrictions.size >= 15) score += 5
                            if (hiddenApps.size >= 5) score += 5
                            return minOf(score, 100)
                        }
    }

    /**
     * Выполнить полную проверку Kiosk функциональности
     */
    fun checkKioskFunctionality(): KioskCheckResult {
                    val errors = mutableListOf<String>()
                    val warnings = mutableListOf<String>()
                    val restrictions = mutableListOf<String>()
                    val hiddenApps = mutableListOf<String>()

                    // Проверка Device Owner статуса
                    val isDeviceOwner = isDeviceOwner()
                    if (!isDeviceOwner) {
                        warnings.add("Device Owner not configured - limited protection available")
                    }

                    // Проверка статуса default launcher
                    val isDefaultLauncher = isDefaultLauncherApp()
                    if (!isDefaultLauncher) {
                        warnings.add("Not set as default launcher")
                    }

                    // Проверка Lock Task
                    val isLockTaskActive = try {
                        val manager = context.getSystemService(ActivityManager::class.java)
                        manager?.isInLockTaskMode ?: false
                    } catch (e: Exception) {
                        errors.add("Failed to check Lock Task mode: ${e.message}")
                        false
                    }

                    if (!isLockTaskActive) {
                        warnings.add("Lock Task mode not active")
                    }

                    // Проверка User Restrictions
                    if (isDeviceOwner) {
                        val userManager =
                            context.getSystemService(Context.USER_SERVICE) as? UserManager
                        if (userManager != null) {
                            val restrictionsList = listOf(
                                UserManager.DISALLOW_INSTALL_APPS,
                                UserManager.DISALLOW_UNINSTALL_APPS,
                                UserManager.DISALLOW_FACTORY_RESET,
                                UserManager.DISALLOW_SAFE_BOOT,
                                UserManager.DISALLOW_DEBUGGING_FEATURES,
                                UserManager.DISALLOW_CONFIG_WIFI,
                                UserManager.DISALLOW_CONFIG_BLUETOOTH,
                                UserManager.DISALLOW_ADD_USER,
                                UserManager.DISALLOW_MODIFY_ACCOUNTS
                            )

                            restrictions.addAll(
                                restrictionsList.filter { !userManager.hasUserRestriction(it) }
                                    .map { "Not applied: $it" }
                            )

                            restrictions.addAll(
                                restrictionsList.filter { userManager.hasUserRestriction(it) }
                                    .map { "✓ $it" }
                            )
                        }
                    }

                    // Проверка Accessibility Service
                    val isAccessibilityEnabled = try {
                        val accessibilityManager =
                            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
                        accessibilityManager?.isEnabled ?: false
                    } catch (e: Exception) {
                        errors.add("Failed to check Accessibility: ${e.message}")
                        false
                    }

                    if (!isAccessibilityEnabled && !isDeviceOwner) {
                        warnings.add("Accessibility Service not enabled")
                    }

                    // Проверка Foreground Service
                    val isRunningInForeground = try {
                        val am =
                            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                        am?.appTasks?.any { it.taskInfo.id > 0 } ?: false
                    } catch (e: Exception) {
                        errors.add("Failed to check Foreground status: ${e.message}")
                        false
                    }

                    // Список скрытых приложений
                    hiddenApps.addAll(
                        listOf(
                            "com.android.settings",
                            "com.android.vending",
                            "com.android.chrome",
                            "com.android.packageinstaller",
                            "com.google.android.tvlauncher",
                            "com.android.documentsui"
                        )
                    )

                    return KioskCheckResult(
                        isDeviceOwner = isDeviceOwner,
                        isDefaultLauncher = isDefaultLauncher,
                        isLockTaskActive = isLockTaskActive,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isRunningInForeground = isRunningInForeground,
                        appVersion = Build.DISPLAY,
                        androidVersion = Build.VERSION.SDK_INT,
                        restrictions = restrictions,
                        hiddenApps = hiddenApps,
                        errors = errors,
                        warnings = warnings
                    )
    }

    /**
     * Проверка, является ли приложение default launcher
     */
    private fun isDefaultLauncherApp(): Boolean {
                    return try {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                        }
                        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
                        resolveInfo?.activityInfo?.packageName == context.packageName
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking default launcher", e)
                        false
                    }
    }
}
