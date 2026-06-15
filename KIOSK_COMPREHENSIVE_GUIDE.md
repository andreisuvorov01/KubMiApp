# Kiosk Mode - Комплексная защита для Android TV и Интерактивных панелей

## 📋 Обзор реализованного функционала

Этот документ описывает **многоуровневую архитектуру защиты** Kiosk Mode, реализованную в KubMI приложении.

### Уровни защиты (7 уровней):

1. **Lock Task Mode (Pinning/COSU)** - Блокировка переключения приложений
2. **Device Owner Restrictions** - Системные ограничения через DevicePolicyManager
3. **User Restrictions** - Запреты на действия пользователя  
4. **System Apps Blocking** - Скрытие опасных системных приложений
5. **Lock Task Features** - Управление системными кнопками (Android 9+)
6. **Service Protection** - Фоновые сервисы для постоянного мониторинга
7. **Hardware-level Controls** - Блокировка пультов и аппаратных кнопок

---

## 🔧 Реализованные компоненты

### 1. AdvancedKioskManager (Главный менеджер)

**Файл:** `app/src/main/java/com/example/kubmi/kiosk/AdvancedKioskManager.kt`

**Функции:**
- `setupFullKioskMode()` - Полная инициализация защиты (требует Device Owner)
- `setupLimitedKioskMode()` - Базовая защита без Device Owner
- `isDeviceOwner()` - Проверка статуса Device Owner
- `hideAndroidTvUI()` - Скрытие Android TV интерфейса
- `configureForInteractivePanel(oemType)` - Конфигурация для различных OEM панелей

**Поддерживаемые OEM типы:**
- BENQ, PROMETHEAN, NEWLINE, CLEVERTOUCH, HIKVISION, VIEWSONIC, SMART, ANDROID_TV

**Применяемые ограничения:**
- ✓ Lock Task блокировка
- ✓ Скрытие системных приложений (Google Play, Settings, Chrome и т.д.)
- ✓ User Restrictions (20+ ограничений)
- ✓ Status Bar отключение
- ✓ Keyguard отключение
- ✓ Network Restrictions

### 2. KioskService (Foreground Service)

**Файл:** `app/src/main/java/com/example/kubmi/service/KioskService.kt`

**Функции:**
- Постоянно работающий сервис (не может быть убит системой)
- Проверка каждые 500ms находится ли приложение в foreground
- Автоматический возврат в foreground если пользователь вышел
- Использует 3 метода проверки:
  1. RunningProcesses
  2. AppTasks
  3. UsageStatsManager (самый надёжный)

**Статус:** ⭐ Уже реализован и работает

### 3. KioskJobService (Android 12+)

**Файл:** `app/src/main/java/com/example/kubmi/service/KioskJobService.kt`

**Функции:**
- Периодические проверки каждые 15 минут
- Работает на Android S (12) и выше
- Более надёжен чем AlarmManager
- Сохраняется после перезагрузки

**Инициализация:**
```kotlin
KioskJobService.schedule(context)  // Автоматическое планирование
KioskJobService.cancel(context)    // Отмена
```

### 4. AppMonitorService

**Файл:** `app/src/main/java/com/example/kubmi/kiosk/AppMonitorService.kt`

**Статус:** ⭐ Уже реализован

**Функции:**
- Мониторинг активных приложений
- Блокировка запуска запрещённых приложений
- Настраиваемые списки чёрных приложений

### 5. SettingsBlocker

**Файл:** `app/src/main/java/com/example/kubmi/kiosk/SettingsBlocker.kt`

**Функции:**
- Блокировка Intent-ов на Settings
- Перехват 20+ типов Intent-ов Settings
- Автоматический возврат в приложение
- Поддержка Android 5.0+

**Использование:**
```kotlin
val blocker = SettingsBlocker(context)
blocker.initialize()  // Активировать блокировку

// При завершении
blocker.unregister()  // Отключить блокировку
```

### 6. TvRemoteHandler

**Файл:** `app/src/main/java/com/example/kubmi/kiosk/TvRemoteHandler.kt`

**Функции:**
- Обработка пультов Android TV
- Блокировка системных кнопок
- Разрешение только навигационных кнопок (DPAD)
- Полный контроль над медиа кнопками

**Блокируемые кнопки:**
- HOME, BACK, MENU, SETTINGS, POWER
- TV_INPUT, CHANNEL_UP/DOWN
- VOLUME управление
- Кнопки переключения HDMI входов

**Разрешённые кнопки:**
- DPAD (навигация)
- ENTER/OK
- MEDIA_PLAY, PAUSE, STOP и т.д.

**Использование в Activity:**
```kotlin
private val tvRemoteHandler = TvRemoteHandler(this)

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    tvRemoteHandler.setup()
}

override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    return tvRemoteHandler.handleTvRemoteKey(keyCode) || super.onKeyDown(keyCode, event)
}
```

### 7. KeyEventBlocker

**Файл:** `app/src/main/java/com/example/kubmi/kiosk/KeyEventBlocker.kt`

**Статус:** ⭐ Уже реализован

**Функции:**
- Блокировка системных горячих клавиш
- Захват комбинаций (Ctrl+W, Alt+Tab и т.д.)
- Интеграция с dispatchKeyEvent в Activity

### 8. BootReceiver (улучшенный)

**Файл:** `app/src/main/java/com/example/kubmi/receiver/BootReceiver.kt`

**Функции:**
- Инициализация Kiosk при загрузке устройства
- Запуск KioskService
- Запуск MainActivity
- Планирование AlarmManager watchdog
- Планирование JobService (Android 12+)
- Задержка 3 сек для стабильности загрузки

**События:**
- ACTION_BOOT_COMPLETED
- ACTION_LOCKED_BOOT_COMPLETED
- ACTION_REBOOT
- QUICKBOOT_POWERON (HTC, Xiaomi и т.д.)

### 9. KioskAccessibilityService

**Файл:** `app/src/main/java/com/example/kubmi/service/KioskAccessibilityService.kt`

**Статус:** ⭐ Уже реализован

**Функции:**
- Дополнительный уровень защиты
- Перехват попыток открытия Settings
- Мониторинг смены приложений

⚠️ **ТРЕБУЕТ:** Ручная активация в Settings → Accessibility

---

## 🚀 Быстрый старт

### Инициализация в MainActivity

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    private lateinit var kioskManager: AdvancedKioskManager
    
    private lateinit var tvRemoteHandler: TvRemoteHandler
    private lateinit var settingsBlocker: SettingsBlocker
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализировать Kiosk защиту
        if (kioskManager.isDeviceOwner()) {
            kioskManager.setupFullKioskMode()
            Log.i("Kiosk", kioskManager.getProtectionStatus())
        } else {
            kioskManager.setupLimitedKioskMode()
        }
        
        // Инициализировать обработчик пульта (Android TV)
        tvRemoteHandler = TvRemoteHandler(this)
        tvRemoteHandler.setup()
        
        // Инициализировать блокировку Settings
        settingsBlocker = SettingsBlocker(this)
        settingsBlocker.initialize()
        
        // Начать Lock Task
        kioskManager.startLockTask(this)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Обработка пульта
        return tvRemoteHandler.handleTvRemoteKey(keyCode) || 
               super.onKeyDown(keyCode, event)
    }
    
    override fun onDestroy() {
        settingsBlocker.unregister()
        super.onDestroy()
    }
}
```

---

## 📱 Установка Device Owner

### Вариант 1: На новом устройстве (РЕКОМЕНДУЕТСЯ)

#### Шаг 1: Проверить что устройство не настроено

```bash
adb shell settings get secure user_setup_complete
# Должно вернуть: 0 или пусто
```

Если вернёт `1` - выполните Factory Reset на устройстве.

#### Шаг 2: Установить APK

```bash
adb install -r app-release.apk
```

#### Шаг 3: Установить Device Owner

```bash
adb shell dpm set-device-owner com.example.kubmi/.receiver.DeviceAdminReceiver
```

**Успешный результат:**
```
Success: Device owner set to package com.example.kubmi
Active admin set to component...
```

#### Шаг 4: Проверка

```bash
adb shell dpm list-owners
```

### Вариант 2: На настроенном устройстве (требует root)

```bash
adb root
adb remount

# Создать XML файл Device Owner
adb shell cat > /data/system/device_owner_2.xml << 'EOF'
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<device-owner 
    package="com.example.kubmi"
    name="KubMI Kiosk"
    component="com.example.kubmi/.receiver.DeviceAdminReceiver"
    userRestrictionsMigrated="true" />
EOF

# Установить права
adb shell chmod 0644 /data/system/device_owner_2.xml

# Перезагрузить
adb reboot
```

### Вариант 3: Через NFC (Android 6-9)

1. Выполнить `apm dpm set-device-owner-nfc` на управляющем устройстве
2. Поднести целевое устройство

### Вариант 4: QR код (Android 7+)

Google предоставляет инструмент для генерации QR кодов

---

## 🔒 Таблица применяемых ограничений

### User Restrictions (20+)

| Ограничение | Описание |
|-------------|---------|
| `DISALLOW_INSTALL_APPS` | Запрет установки приложений |
| `DISALLOW_INSTALL_UNKNOWN_SOURCES` | Запрет установки из неизвестных источников |
| `DISALLOW_UNINSTALL_APPS` | Запрет удаления приложений |
| `DISALLOW_FACTORY_RESET` | Запрет Factory Reset |
| `DISALLOW_SAFE_BOOT` | Запрет Safe Boot |
| `DISALLOW_ADD_USER` | Запрет добавления пользователей |
| `DISALLOW_MODIFY_ACCOUNTS` | Запрет изменения аккаунтов |
| `DISALLOW_CONFIG_WIFI` | Запрет настройки WiFi |
| `DISALLOW_CONFIG_BLUETOOTH` | Запрет настройки Bluetooth |
| `DISALLOW_CONFIG_VPN` | Запрет настройки VPN |
| `DISALLOW_CONFIG_TETHERING` | Запрет точки доступа |
| `DISALLOW_CONFIG_MOBILE_NETWORKS` | Запрет настройки мобильных сетей |
| `DISALLOW_USB_FILE_TRANSFER` | Запрет USB передачи |
| `DISALLOW_MOUNT_PHYSICAL_MEDIA` | Запрет SD карт |
| `DISALLOW_DEBUGGING_FEATURES` | Запрет отладки |
| `DISALLOW_NETWORK_RESET` | Запрет сброса сети |
| `DISALLOW_APPS_CONTROL` | Запрет управления приложениями |
| `DISALLOW_CREATE_WINDOWS` | Запрет создания окон |
| `DISALLOW_OUTGOING_CALLS` | Запрет исходящих звонков |
| `DISALLOW_SMS` | Запрет SMS |

### Скрываемые системные приложения

- com.android.settings (Настройки)
- com.android.systemui (SystemUI)
- com.google.android.gms (Google Play Services)
- com.android.vending (Play Store)
- com.android.chrome (Chrome)
- com.android.documentsui (Файловый менеджер)
- com.google.android.tvlauncher (Android TV Launcher)
- com.google.android.tvrecommendations (TV Recommendations)
- И ещё 10+ системных приложений

---

## 🎬 Android TV специфичное

### Поддерживаемые пульты

- ✅ Google Remote (стандартный Android TV)
- ✅ Amazon Fire Stick Remote
- ✅ OEM TV пульты
- ✅ Универсальные IR пульты (с USB адаптером)
- ✅ Клавиатуры по USB

### Конфигурация для OEM панелей

```kotlin
kioskManager.configureForInteractivePanel("BENQ")
// или
kioskManager.configureForInteractivePanel("PROMETHEAN")
```

### Скрытие Android TV элементов

```kotlin
kioskManager.hideAndroidTvUI()
```

---

## 📊 Статус защиты

Получить статус защиты:

```kotlin
val status = kioskManager.getProtectionStatus()
Log.d("Kiosk", status)
```

**Вывод:**
```
=== Kiosk Protection Status ===
Device Owner: ✓ YES
Android Version: 33
Device Model: [Model]
Device Manufacturer: [Manufacturer]

Device Owner Features:
✓ Lock Task Mode
✓ User Restrictions Applied
✓ System Apps Hidden
✓ Status Bar Disabled
✓ Keyguard Disabled
✓ Lock Task Features Configured (Android 9+)
```

---

## ⚠️ Проблемы и решения

### Проблема: Device Owner не устанавливается

**Решение:**
1. Убедитесь что `user_setup_complete = 0`
2. Убедитесь что это новое/reset устройство
3. Проверьте что пакет установлен: `adb shell pm list packages | grep kubmi`
4. Проверьте имя компонента: `adb shell cmd package list receivers | grep kubmi`

### Проблема: Lock Task не работает

**Решение:**
1. Убедитесь что Device Owner установлен
2. Проверьте что приложение в `setLockTaskPackages`
3. Убедитесь что Activity запущена с флагом NEW_TASK

### Проблема: Settings всё равно открывается

**Решение:**
1. Активируйте AccessibilityService
2. Убедитесь что SettingsBlocker инициализирован
3. Используйте Device Owner `setApplicationHidden()`

### Проблема: Невозможно вернуться из приложения

**Решение:**
- На пульте нажмите кнопку которая управляет вашей функциональностью (например, OK)
- Используйте ADB для исправления: `adb shell am force-stop com.android.settings`

---

## 🔐 Уровни безопасности

### Без Device Owner (обычное приложение)

```
Защита: 4/10
├─ AppMonitor Service (Watchdog)
├─ KeyEvent блокировка
├─ AccessibilityService (если включен)
└─ Foreground Service
```

### С Device Owner (рекомендуется)

```
Защита: 9/10
├─ Lock Task Mode
├─ User Restrictions (20+)
├─ System Apps Hidden
├─ Status Bar отключён
├─ Keyguard отключён
├─ Network Restrictions
├─ Watchdog Services
├─ JobService (Android 12+)
└─ OEM Configurations
```

### Системное приложение + root (максимум)

```
Защита: 10/10
├─ Полный контроль над системой
├─ Скрытые API доступны
├─ Невозможно удалить
├─ Можно отключить SystemUI
└─ Полный контроль над Launcher
```

---

## 📚 Дополнительные ресурсы

- Android Enterprise: https://developers.google.com/android/management
- Device Admin API: https://developer.android.com/guide/topics/admin/device-admin
- Lock Task Mode: https://developer.android.com/work/dpm/dedicated-devices
- Accessibility Services: https://developer.android.com/guide/topics/accessibility/accessibility-overview

---

## 📝 Лицензия и примечания

Данная реализация предназначена для киосков и интерактивных панелей, где требуется полный контроль над устройством.

Все компоненты разработаны с учётом лучших практик безопасности Android и совместимости с разными версиями и устройствами.
