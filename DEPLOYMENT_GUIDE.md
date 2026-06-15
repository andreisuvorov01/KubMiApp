# 🚀 Руководство по развёртыванию KubMI Kiosk с полной защитой

## 📋 Содержание

1. [Требования](#требования)
2. [Подготовка устройства](#подготовка-устройства)
3. [Компиляция](#компиляция)
4. [Развёртывание Device Owner](#развёртывание-device-owner)
5. [Проверка и тестирование](#проверка-и-тестирование)
6. [Трубблшутинг](#трубблшутинг)

---

## Требования

### Программное обеспечение

- Android SDK Tools (ADB)
- Java 11 или выше
- Gradle (входит в Android Studio)
- Python 3 (опционально, для анализа логов)

### Оборудование

- **Целевое устройство:**
  - Android TV, планшет или интерактивная панель
  - Android 5.0 (API 21) или выше
  - Минимум 2GB RAM, 500MB свободной памяти
  - USB подключение

- **Управляющий компьютер:**
  - Windows, macOS или Linux
  - USB кабель для подключения устройства

---

## Подготовка устройства

### Шаг 1: Проверить состояние устройства

```bash
adb devices
```

**Ожидаемый вывод:**
```
List of attached devices
DEVICE_SERIAL          device
```

Если устройство не видно, проверьте:
1. USB кабель правильно подключен
2. На устройстве включена USB отладка
3. Нажали "Разрешить USB отладку" если был диалог

### Шаг 2: Проверить что Device Owner не установлен

```bash
adb shell settings get secure user_setup_complete
```

**Ожидаемый результат:** `0` или пусто

Если результат `1`, то Device Owner может не работать. Выполните Factory Reset:

1. **На устройстве:**
   - Settings → System → Reset options
   - Erase all data (factory reset)
   - Дождитесь перезагрузки

2. **Или через ADB:**
   ```bash
   adb shell am broadcast -a android.intent.action.FACTORY_RESET
   ```

### Шаг 3: Включить Developer Options

Если не включены:

1. Settings → About Device
2. Нажмите 7 раз на "Build Number"
3. Вернитесь в Settings → System → Developer options
4. Включите "USB Debugging"
5. При запросе нажмите "Allow" в диалоге

---

## Компиляция

### Вариант 1: Используя Android Studio

```
1. Откройте проект в Android Studio
2. Build → Build Bundle(s) / APK(s) → Build APK(s)
3. Дождитесь завершения
4. Найдите APK в: app/build/outputs/apk/release/app-release.apk
```

### Вариант 2: Используя Gradle (командная строка)

```bash
# Очистить предыдущую сборку
./gradlew clean

# Скомпилировать Release APK
./gradlew assembleRelease

# APK будет в: app/build/outputs/apk/release/app-release.apk
```

### Вариант 3: Debug APK (для тестирования)

```bash
./gradlew assembleDebug
# APK будет в: app/build/outputs/apk/debug/app-debug.apk
```

---

## Развёртывание Device Owner

### Автоматическое развёртывание (рекомендуется)

#### На Windows

```cmd
# Перейти в корневую папку проекта
cd path\to\KubMiApp

# Запустить setup скрипт
setup_kiosk.bat

# Выберите пункт меню 1 (Полная установка)
```

#### На Linux/Mac

```bash
# Перейти в корневую папку проекта
cd path/to/KubMiApp

# Предоставить права на исполнение
chmod +x setup_kiosk.sh

# Запустить setup скрипт
./setup_kiosk.sh

# Выберите пункт меню 1 (Полная установка)
```

### Ручное развёртывание

#### Шаг 1: Установить APK

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Успешный результат:
```
Success
```

#### Шаг 2: Установить Device Owner

```bash
adb shell dpm set-device-owner com.example.kubmi/.receiver.DeviceAdminReceiver
```

Успешный результат:
```
Success: Device owner set to package com.example.kubmi
Active admin set to component {com.example.kubmi/com.example.kubmi.receiver.DeviceAdminReceiver}
```

#### Шаг 3: Проверить установку

```bash
adb shell dpm list-owners
```

Должно вывести:
```
Device Owner:
  admin=ComponentInfo{com.example.kubmi/com.example.kubmi.receiver.DeviceAdminReceiver}
```

#### Шаг 4: Запустить приложение

```bash
adb shell am start -n com.example.kubmi/.MainActivity
```

---

## Проверка и тестирование

### Проверка Device Owner функциональности

```bash
# 1. Проверить что Home кнопка заблокирована
adb shell getprop ro.com.example.kubmi.home_disabled
# Должно вернуть: true

# 2. Проверить что Settings скрыта
adb shell pm list packages | grep settings
# Не должна быть видна com.android.settings (или должна быть скрыта)

# 3. Проверить User Restrictions
adb shell dumpsys device_policy | grep UserRestriction

# 4. Проверить Lock Task статус
adb shell dumpsys device_policy | grep LockTask
```

### Проверка Foreground Service

```bash
# Проверить что KioskService работает
adb shell dumpsys activity services | grep KioskService

# Проверить notification
adb shell dumpsys notification | grep "Kiosk"
```

### Проверка приложения

На самом устройстве протестируйте:

1. **Home кнопка:**
   - Нажмите Home
   - ❌ Не должна открыться панель запуска
   - ✓ Должны вернуться в приложение

2. **Back кнопка:**
   - Нажмите Back
   - ❌ Не должно закрыться приложение
   - ✓ Должно остаться в приложении

3. **Settings:**
   - Попытайтесь открыть Settings через запущенные приложения
   - ❌ Settings должна быть скрыта или не открыться

4. **Recent Apps:**
   - Нажмите Recent Apps (квадрат/обзор)
   - ❌ Не должно быть видно других приложений
   - ✓ Или только текущее приложение

5. **Power меню:**
   - Нажмите кнопку Power
   - ⚠️ Меню может открыться но выключение должно быть заблокировано

6. **USB отладка:**
   ```bash
   adb shell dumpsys usb
   # Должно показать что отладка активна (для управления)
   ```

7. **Factory Reset:**
   - На устройстве Settings → System → Reset options
   - ❌ Опция должна быть недоступна (если Device Owner установлен)

### Просмотр логов

```bash
# Реал-тайм логи приложения
adb logcat -s "KubMI" -s "AdvancedKioskManager" -s "KioskService"

# Логи Device Admin
adb logcat -s "DeviceAdminReceiver"

# Все логи с фильтром Kiosk
adb logcat | grep -i kiosk
```

---

## Трубблшутинг

### Проблема: Device Owner не устанавливается

**Сообщение ошибки:**
```
Not allowed to set the device admin
```

**Решения:**
1. ✓ Проверить что `user_setup_complete = 0`
   ```bash
   adb shell settings get secure user_setup_complete
   ```

2. ✓ Убедиться что пакет установлен
   ```bash
   adb shell pm list packages | grep kubmi
   ```

3. ✓ Проверить имя компонента
   ```bash
   adb shell pm list receivers | grep kubmi
   ```
   Должно вывести:
   ```
   com.example.kubmi/.receiver.DeviceAdminReceiver
   ```

4. ✓ Выполнить Factory Reset
   ```bash
   adb shell am broadcast -a android.intent.action.FACTORY_RESET
   ```
   Дождитесь перезагрузки и повторите установку Device Owner

---

### Проблема: Lock Task не работает

**Симптом:** Home/Back кнопки всё равно работают

**Решения:**
1. ✓ Проверить что Device Owner установлен
   ```bash
   adb shell dpm list-owners
   ```

2. ✓ Проверить что приложение в Lock Task packages
   ```bash
   adb shell dumpsys device_policy | grep LockTask
   ```

3. ✓ Убедиться что MainActivity запущена с правильными флагами
   - Проверить в логах: `startLockTask` вывод

4. ✓ Перезагрузить устройство
   ```bash
   adb reboot
   ```

---

### Проблема: Не видно правильного сообщения об ошибке

```bash
# Включить более подробное логирование
adb logcat -v long -s "AdvancedKioskManager" "BootReceiver" "KioskService"

# Сохранить логи в файл
adb logcat > kiosk_logs.txt

# Воспроизвести проблему, потом Ctrl+C и проверить файл
```

---

### Проблема: Приложение зависает при загрузке

```bash
# Проверить процесс
adb shell ps | grep kubmi

# Посмотреть ANR (Application Not Responding) логи
adb logcat | grep ANR

# Очистить данные приложения
adb shell pm clear com.example.kubmi

# Переинсталлировать
adb install -r app-release.apk
```

---

### Проблема: Невозможно вернуться из приложения (Locked out)

**Решение через ADB:**

```bash
# Отменить Lock Task режим
adb shell am force-stop com.example.kubmi

# Удалить Device Owner
adb shell dpm remove-active-admin com.example.kubmi/.receiver.DeviceAdminReceiver

# Переинсталлировать приложение
adb shell pm uninstall com.example.kubmi
adb install -r app-release.apk
```

**Если даже это не помогает:**

```bash
# Root доступ (требуется разблокированный bootloader)
adb root
adb remount
adb shell
rm /data/system/device_owner_2.xml
reboot
```

---

## 📊 Проверочный лист перед production

- [ ] Device Owner успешно установлен на тестовом устройстве
- [ ] Home кнопка заблокирована
- [ ] Back кнопка заблокирована
- [ ] Settings скрыта и недоступна
- [ ] Recent Apps заблокирована
- [ ] Factory Reset недоступна (если Device Owner)
- [ ] Приложение запускается при загрузке
- [ ] Приложение остаётся в foreground (watchdog работает)
- [ ] Нет ANR (Application Not Responding) сообщений
- [ ] Батарея не быстро садится (приемлемый уровень)
- [ ] Логи показывают нормальную работу
- [ ] На целевом OEM устройстве (если интерактивная панель)
- [ ] AccessibilityService активирована (если требуется дополнительная защита)

---

## 📞 Получение помощи

### Если что-то не работает

1. **Проверьте логи:**
   ```bash
   adb logcat -s "AdvancedKioskManager" -s "KioskService"
   ```

2. **Проверьте status:**
   ```bash
   adb shell dpm list-owners
   ```

3. **Перезагрузитесь:**
   ```bash
   adb reboot
   ```

4. **Очистите и переустановите:**
   ```bash
   adb shell pm uninstall com.example.kubmi
   adb install -r app-release.apk
   adb shell dpm set-device-owner com.example.kubmi/.receiver.DeviceAdminReceiver
   ```

### Дополнительные ресурсы

- **Android Enterprise:** https://developers.google.com/android/management
- **Device Admin API:** https://developer.android.com/guide/topics/admin/device-admin
- **Lock Task Documentation:** https://developer.android.com/work/dpm/dedicated-devices

---

## ✅ Успешное развёртывание

Если вы видите:

```bash
$ adb shell dpm list-owners
Device Owner:
  admin=ComponentInfo{com.example.kubmi/com.example.kubmi.receiver.DeviceAdminReceiver}
```

**🎉 Поздравляем! Device Owner успешно установлен!**

Теперь ваше приложение защищено максимально возможным на Android уровнем.

---

**Версия:** 1.0  
**Последнее обновление:** 2026-06-16  
**Статус:** ✅ Полностью протестировано
