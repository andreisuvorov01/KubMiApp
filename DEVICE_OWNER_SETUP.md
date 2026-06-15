# Настройка Device Owner для KubMI Kiosk App

## Требования
- Android устройство (планшет, интерактивная панель, Android TV)
- ADB установлен на компьютере
- USB кабель

## Вариант 1: На новом устройстве (рекомендуется)

### Шаг 1: Сброс устройства к заводским настройкам
```bash
# Убедитесь что устройство НЕ настроено
adb shell settings get secure user_setup_complete
# Должно вернуть: 0 или пусто
```

Если возвращает 1, выполните Factory Reset:
- Settings → System → Reset options → Erase all data (factory reset)

### Шаг 2: Пропустить первоначальную настройку
- Подключите устройство к ADB
- Включите USB Debugging в Developer Options
- НЕ добавляйте Google аккаунт!

### Шаг 3: Установить APK
```bash
adb install app-release.apk
```

### Шаг 4: Установить Device Owner
```bash
adb shell dpm set-device-owner com.example.kubmi/.receiver.DeviceAdminReceiver
```

Успешный вывод:
```
Success: Device owner set to package com.example.kubmi
Active admin set to component {com.example.kubmi/com.example.kubmi.receiver.DeviceAdminReceiver}
```

### Шаг 5: Проверка
```bash
adb shell dpm list-owners
```

Должно вывести:
```
Device Owner: 
  admin=ComponentInfo{com.example.kubmi/com.example.kubmi.receiver.DeviceAdminReceiver}
```

### Шаг 6: Запуск приложения
```bash
adb shell am start -n com.example.kubmi/.MainActivity
```

---

## Вариант 2: На уже настроенном устройстве (требуется root)

### Шаг 1: Получить root доступ
```bash
adb root
# Если ошибка - установите Magisk или используйте кастомную прошивку
```

### Шаг 2: Remount /data
```bash
adb root
adb remount
```

### Шаг 3: Установить APK
```bash
adb install app-release.apk
```

### Шаг 4: Создать device_owner_2.xml
```bash
adb shell
cat > /data/system/device_owner_2.xml << 'EOF'
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<device-owner 
    package="com.example.kubmi" 
    name="KubMI Kiosk" 
    component="com.example.kubmi/.receiver.DeviceAdminReceiver" 
    userRestrictionsMigrated="true" />
EOF
```

### Шаг 5: Установить права
```bash
chmod 600 /data/system/device_owner_2.xml
chown system:system /data/system/device_owner_2.xml
```

### Шаг 6: Перезагрузка
```bash
reboot
```

### Шаг 7: Проверка после перезагрузки
```bash
adb shell dpm list-owners
```

---

## Вариант 3: Через NFC (Android 6-9)

### Требования:
- Второе устройство Android с NFC
- NFC Provisioning приложение

### Процесс:
1. Factory Reset целевого устройства
2. На втором устройстве установите NFC provisioning app
3. Создайте provisioning payload с данными:
   - Package: com.example.kubmi
   - Component: com.example.kubmi/.receiver.DeviceAdminReceiver
4. При первой загрузке поднесите устройства друг к другу
5. Устройство автоматически станет Device Owner

---

## Вариант 4: Через QR код (Android 7+)

### Шаг 1: Factory Reset устройства

### Шаг 2: Создать QR код provisioning
Используйте сервис: https://developers.google.com/android/work/prov-devices

Минимальные данные:
```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.kubmi/.receiver.DeviceAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://yourserver.com/app-release.apk",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true
}
```

### Шаг 3: При первой загрузке
- Нажмите 6 раз на экране приветствия
- Сканируйте QR код
- Устройство автоматически установит приложение и станет Device Owner

---

## Удаление Device Owner

### Через приложение (если реализовано)
```bash
adb shell am start -n com.example.kubmi/.AdminSettingsActivity
# В приложении: Settings → Admin → Remove Device Owner
```

### Через ADB
```bash
adb shell dpm remove-active-admin com.example.kubmi/.receiver.DeviceAdminReceiver
```

### Принудительно (root)
```bash
adb root
adb shell rm /data/system/device_owner*.xml
adb shell rm /data/system/device_policies.xml
adb reboot
```

### Factory Reset (всегда работает)
```bash
adb shell am broadcast -a android.intent.action.FACTORY_RESET
# Или через Settings → System → Reset
```

---

## Проверка возможностей Device Owner

```bash
# Проверка статуса
adb shell dpm list-owners

# Проверка ограничений
adb shell dumpsys user_restrictions

# Проверка Lock Task
adb shell dumpsys activity activities | grep -i lock

# Проверка скрытых приложений
adb shell pm list packages -u | grep hidden
```

---

## Troubleshooting

### Ошибка: "Not allowed to set the device owner"
**Причина:** Устройство уже настроено или есть Google аккаунт

**Решение:**
1. Factory Reset
2. Пропустите настройку Google
3. НЕ добавляйте аккаунты до установки Device Owner

### Ошибка: "java.lang.IllegalStateException: Not allowed"
**Причина:** Уже есть другой Device Owner или Profile Owner

**Решение:**
```bash
# Проверить существующих владельцев
adb shell dpm list-owners

# Удалить всех
adb shell dpm remove-active-admin <component>
```

### Ошибка: "Trying to set device owner but device is already provisioned"
**Причина:** user_setup_complete = 1

**Решение (root):**
```bash
adb root
adb shell settings put secure user_setup_complete 0
adb shell settings put global device_provisioned 0
adb reboot
```

### Lock Task не работает
**Причина:** Пакет не добавлен в whitelist

**Решение:**
```bash
# Проверить
adb shell dumpsys activity recents | grep -i lock

# Переустановить через приложение
# MainActivity уже содержит код: advancedKioskManager.setupFullKioskMode()
```

---

## Рекомендации по безопасности

1. **Отключите ADB после настройки:**
   ```bash
   adb shell settings put global adb_enabled 0
   ```

2. **Скройте Developer Options:**
   ```bash
   adb shell settings put global development_settings_enabled 0
   ```

3. **Заблокируйте USB:**
   - Device Owner автоматически блокирует через UserManager.DISALLOW_USB_FILE_TRANSFER

4. **Установите пароль на Recovery:**
   - Зависит от производителя устройства

5. **Отключите физические кнопки:**
   - Только для специализированных устройств с OEM API

---

## Автоматизация для массового развёртывания

### Скрипт для Windows (PowerShell)
```powershell
# setup-device-owner.ps1
$package = "com.example.kubmi"
$component = "$package/.receiver.DeviceAdminReceiver"

Write-Host "Checking device..."
$setupComplete = adb shell settings get secure user_setup_complete
if ($setupComplete -eq "1") {
    Write-Host "ERROR: Device already provisioned. Factory reset required!"
    exit 1
}

Write-Host "Installing APK..."
adb install -r app-release.apk

Write-Host "Setting device owner..."
adb shell dpm set-device-owner $component

Write-Host "Verifying..."
adb shell dpm list-owners

Write-Host "Starting app..."
adb shell am start -n $package/.MainActivity

Write-Host "Done! Device is now in kiosk mode."
```

### Скрипт для Linux/Mac (Bash)
```bash
#!/bin/bash
# setup-device-owner.sh

PACKAGE="com.example.kubmi"
COMPONENT="$PACKAGE/.receiver.DeviceAdminReceiver"

echo "Checking device..."
SETUP_COMPLETE=$(adb shell settings get secure user_setup_complete | tr -d '\r')
if [ "$SETUP_COMPLETE" = "1" ]; then
    echo "ERROR: Device already provisioned. Factory reset required!"
    exit 1
fi

echo "Installing APK..."
adb install -r app-release.apk

echo "Setting device owner..."
adb shell dpm set-device-owner "$COMPONENT"

echo "Verifying..."
adb shell dpm list-owners

echo "Starting app..."
adb shell am start -n "$PACKAGE/.MainActivity"

echo "Done! Device is now in kiosk mode."
```

---

## Уровни защиты

| Функция | Без Device Owner | С Device Owner | Уровень защиты |
|---------|------------------|----------------|----------------|
| Блокировка Home | ❌ Частично | ✅ Полностью | 9/10 |
| Блокировка Settings | ❌ Нет | ✅ Да | 10/10 |
| Блокировка Recent Apps | ❌ Нет | ✅ Да | 9/10 |
| Скрытие Status Bar | ❌ Нет | ✅ Да | 8/10 |
| Lock Task Mode | ⚠️ Легко обойти | ✅ Невозможно | 10/10 |
| Блокировка установки APK | ❌ Нет | ✅ Да | 10/10 |
| Скрытие системных приложений | ❌ Нет | ✅ Да | 9/10 |

**Итоговая оценка:**
- Без Device Owner: 3/10
- С Device Owner: 9.5/10
- С Device Owner + Root: 10/10
