@echo off
REM ============================================================================
REM KubMI Kiosk Setup Script for Windows
REM
REM Автоматизирует установку Device Owner и конфигурацию киоска для Windows
REM
REM Использование: setup_kiosk.bat [apk_file]
REM ============================================================================

setlocal enabledelayedexpansion

cls
title KubMI Kiosk Setup - Device Owner Configuration

REM Конфигурация
set PACKAGE_NAME=com.example.kubmi
set ADMIN_RECEIVER=%PACKAGE_NAME%/.receiver.DeviceAdminReceiver
set APK_FILE=%1

color 0B
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║            KubMI Kiosk Setup ^& Configuration              ║
echo ║                    Windows Edition                         ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

REM ============================================================================
REM Проверка ADB
REM ============================================================================

echo [*] Проверка ADB...
adb version >nul 2>&1
if errorlevel 1 (
    color 0C
    echo [X] ADB не найдена!
    echo.
    echo Решение: Установите Android SDK Tools
    echo Скачать: https://developer.android.com/studio/command-line-tools
    echo.
    pause
    exit /b 1
)
echo [+] ADB найдена

REM ============================================================================
REM Проверка подключения устройства
REM ============================================================================

echo [*] Проверка подключения устройства...
for /f "tokens=*" %%A in ('adb devices ^| find /C "device"') do set DEVICE_COUNT=%%A

if %DEVICE_COUNT% lss 1 (
    color 0C
    echo [X] Устройство не подключено!
    echo.
    echo Попробуйте:
    echo   1. Подключите USB кабель
    echo   2. Нажмите "Разрешить USB отладку" на устройстве
    echo   3. Выполните: adb devices
    echo.
    pause
    exit /b 1
)
echo [+] Устройство подключено

:MENU
cls
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║                    ГЛАВНОЕ МЕНЮ                            ║
echo ╚════════════════════════════════════════════════════════════╝
echo.
echo  1) Полная установка Device Owner (РЕКОМЕНДУЕТСЯ)
echo  2) Установить только APK
echo  3) Установить только Device Owner
echo  4) Проверить Device Owner статус
echo  5) Запустить приложение
echo  6) Показать информацию об устройстве
echo  7) Удалить Device Owner
echo  8) Выход
echo.
set /p CHOICE=Выберите действие (1-8): 

if "%CHOICE%"=="1" goto FULL_SETUP
if "%CHOICE%"=="2" goto INSTALL_APK
if "%CHOICE%"=="3" goto SET_DO
if "%CHOICE%"=="4" goto CHECK_DO
if "%CHOICE%"=="5" goto START_APP
if "%CHOICE%"=="6" goto DEVICE_INFO
if "%CHOICE%"=="7" goto REMOVE_DO
if "%CHOICE%"=="8" exit /b 0

color 0C
echo [X] Неправильный выбор!
timeout /t 2 /nobreak
goto MENU

REM ============================================================================
REM ПОЛНАЯ УСТАНОВКА
REM ============================================================================

:FULL_SETUP
cls
echo ╔════════════════════════════════════════════════════════════╗
echo ║          ПОЛНАЯ УСТАНОВКА DEVICE OWNER                     ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

REM Проверка статуса устройства
echo [*] Проверка статуса устройства...
for /f "tokens=*" %%A in ('adb shell settings get secure user_setup_complete') do set USER_SETUP=%%A

if "%USER_SETUP%"=="0" (
    color 0A
    echo [+] Устройство НЕ настроено (оптимально для Device Owner)
) else (
    color 0E
    echo [!] Устройство НАСТРОЕНО (user_setup_complete=%USER_SETUP%)
    echo [!] Device Owner может не работать корректно
    echo.
    set /p CONTINUE=Продолжить? (д/Н): 
    if not "!CONTINUE!"=="д" (
        color 0C
        echo [X] Отменено
        timeout /t 2 /nobreak
        goto MENU
    )
)
echo.

REM Установка APK
echo [*] Поиск APK файла...
if "%APK_FILE%"=="" (
    for /R . %%F in (app-release.apk) do set APK_FILE=%%F
)

if "%APK_FILE%"=="" (
    for /R . %%F in (app-debug.apk) do set APK_FILE=%%F
)

if "%APK_FILE%"=="" (
    color 0C
    echo [X] APK файл не найден!
    echo.
    echo Укажите путь к APK файлу как аргумент:
    echo   setup_kiosk.bat "path\to\app-release.apk"
    echo.
    pause
    goto MENU
)

echo [*] Установка: %APK_FILE%
adb install -r "%APK_FILE%"
if errorlevel 1 (
    color 0C
    echo [X] Ошибка при установке APK
    pause
    goto MENU
)
color 0A
echo [+] APK успешно установлен
echo.

REM Установка Device Owner
echo [*] Установка Device Owner...
echo    Пакет: %PACKAGE_NAME%
echo    Компонент: %ADMIN_RECEIVER%
adb shell dpm set-device-owner "%ADMIN_RECEIVER%"
if errorlevel 1 (
    color 0C
    echo [X] Ошибка при установке Device Owner
    echo.
    echo Возможные причины:
    echo   1. Устройство уже настроено (user_setup_complete=1)
    echo   2. Уже есть другой Device Owner
    echo   3. Пакет не установлен
    echo.
    echo Решение: Выполните Factory Reset и повторите
    pause
    goto MENU
)
color 0A
echo [+] Device Owner установлен успешно!
echo.

REM Проверка
echo [*] Проверка Device Owner...
adb shell dpm list-owners
echo.

REM Запуск приложения
echo [*] Запуск приложения...
adb shell am start -n "%PACKAGE_NAME%/.MainActivity"
color 0A
echo [+] Приложение запущено
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║              УСТАНОВКА ЗАВЕРШЕНА УСПЕШНО!                  ║
echo ╚════════════════════════════════════════════════════════════╝
echo.
echo Дополнительно рекомендуется:
echo   1. Активировать Accessibility Service:
echo      adb shell settings put secure enabled_accessibility_services ^
echo      %PACKAGE_NAME%/.service.KioskAccessibilityService
echo.
echo   2. Проверить статус: adb shell dpm list-owners
echo.
pause
goto MENU

REM ============================================================================
REM УСТАНОВКА APK
REM ============================================================================

:INSTALL_APK
cls
echo [*] Поиск APK файла...
if "%APK_FILE%"=="" (
    for /R . %%F in (app-release.apk) do set APK_FILE=%%F
)

if "%APK_FILE%"=="" (
    color 0C
    echo [X] APK файл не найден!
    pause
    goto MENU
)

echo [*] Установка: %APK_FILE%
adb install -r "%APK_FILE%"
if errorlevel 1 (
    color 0C
    echo [X] Ошибка при установке APK
) else (
    color 0A
    echo [+] APK успешно установлен
)
echo.
pause
goto MENU

REM ============================================================================
REM УСТАНОВКА DEVICE OWNER
REM ============================================================================

:SET_DO
cls
echo [*] Проверка установки пакета...
adb shell pm list packages | find "%PACKAGE_NAME%" >nul
if errorlevel 1 (
    color 0C
    echo [X] Пакет не установлен!
    echo [*] Пожалуйста, сначала установите APK (опция 2)
    pause
    goto MENU
)
color 0A
echo [+] Пакет установлен
echo.

echo [*] Установка Device Owner...
adb shell dpm set-device-owner "%ADMIN_RECEIVER%"
if errorlevel 1 (
    color 0C
    echo [X] Ошибка при установке Device Owner
) else (
    color 0A
    echo [+] Device Owner установлен успешно!
)
echo.
pause
goto MENU

REM ============================================================================
REM ПРОВЕРКА DEVICE OWNER
REM ============================================================================

:CHECK_DO
cls
echo [*] Проверка Device Owner...
echo.
adb shell dpm list-owners
echo.
pause
goto MENU

REM ============================================================================
REM ЗАПУСК ПРИЛОЖЕНИЯ
REM ============================================================================

:START_APP
cls
echo [*] Запуск приложения %PACKAGE_NAME%...
adb shell am start -n "%PACKAGE_NAME%/.MainActivity"
if errorlevel 1 (
    color 0C
    echo [X] Ошибка при запуске приложения
) else (
    color 0A
    echo [+] Приложение запущено
)
echo.
pause
goto MENU

REM ============================================================================
REM ИНФОРМАЦИЯ ОБ УСТРОЙСТВЕ
REM ============================================================================

:DEVICE_INFO
cls
echo ╔════════════════════════════════════════════════════════════╗
echo ║            ИНФОРМАЦИЯ ОБ УСТРОЙСТВЕ                        ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

echo [*] Device Owner статус:
adb shell dpm list-owners
echo.

echo [*] Версия Android:
adb shell getprop ro.build.version.release
echo.

echo [*] Уровень API:
adb shell getprop ro.build.version.sdk
echo.

echo [*] Модель устройства:
adb shell getprop ro.model
echo.

echo [*] Производитель:
adb shell getprop ro.manufacturer
echo.

echo [*] Статус USB отладки:
adb shell getprop ro.debuggable
echo.

echo [*] Серийный номер:
adb shell getprop ro.serialno
echo.

pause
goto MENU

REM ============================================================================
REM УДАЛЕНИЕ DEVICE OWNER
REM ============================================================================

:REMOVE_DO
cls
color 0C
echo ╔════════════════════════════════════════════════════════════╗
echo ║                  ВНИМАНИЕ!                                 ║
echo ║  Это удалит ВСЕ Device Owner ограничения!                 ║
echo ╚════════════════════════════════════════════════════════════╝
echo.
set /p CONFIRM=Вы уверены? (да/Нет): 
if not "!CONFIRM!"=="да" (
    echo [X] Отменено
    timeout /t 2 /nobreak
    goto MENU
)

echo [*] Удаление Device Owner...
adb shell dpm remove-active-admin "%ADMIN_RECEIVER%"
if errorlevel 1 (
    color 0C
    echo [X] Ошибка при удалении Device Owner
) else (
    color 0A
    echo [+] Device Owner удалён
)
echo.
pause
goto MENU

REM ============================================================================
REM КОНЕЦ СКРИПТА
REM ============================================================================

endlocal
