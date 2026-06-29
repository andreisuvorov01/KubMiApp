#!/bin/bash

###############################################################################
# KubMI Kiosk Setup Script
# 
# Автоматизирует установку Device Owner и конфигурацию киоска
# Поддерживает различные варианты установки
#
# Использование: ./setup_kiosk.sh [опции]
###############################################################################

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Конфигурация
PACKAGE_NAME="com.example.kubmi"
ADMIN_RECEIVER="${PACKAGE_NAME}/.receiver.DeviceAdminReceiver"
APK_FILE=""
DEVICE_SERIAL=""

###############################################################################
# Функции помощи
###############################################################################

print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}            KubMI Kiosk Setup & Configuration              ${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_section() {
    echo
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}${1}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

###############################################################################
# Проверки
###############################################################################

check_adb() {
    if ! command -v adb &> /dev/null; then
        print_error "ADB не найден. Пожалуйста, установите Android SDK tools."
        exit 1
    fi
    print_success "ADB найден: $(adb version | head -n1)"
}

check_device_connected() {
    local devices=$(adb devices | grep -c "device$")
    if [ "$devices" -eq 0 ]; then
        print_error "Устройство не подключено или ADB недоступен."
        echo "Попробуйте:"
        echo "  1. Подключите USB кабель"
        echo "  2. Нажмите 'Разрешить USB отладку' на устройстве"
        echo "  3. Выполните: adb devices"
        exit 1
    fi
    print_success "Устройство подключено"
}

check_user_setup_complete() {
    print_section "Проверка статуса устройства"
    
    local status=$(adb shell settings get secure user_setup_complete)
    
    if [ "$status" = "0" ] || [ -z "$status" ]; then
        print_success "Устройство НЕ настроено (оптимально для Device Owner)"
        return 0
    else
        print_warning "Устройство НАСТРОЕНО (user_setup_complete=$status)"
        print_warning "Device Owner может не работать корректно"
        echo
        read -p "Продолжить? (д/Н) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Дд]$ ]]; then
            print_error "Отменено"
            exit 1
        fi
        return 1
    fi
}

###############################################################################
# Установка APK
###############################################################################

install_apk() {
    print_section "Установка APK"
    
    if [ -z "$APK_FILE" ]; then
        # Найти APK в build директории
        APK_FILE=$(find . -name "app-release.apk" -o -name "app-debug.apk" | head -n1)
        
        if [ -z "$APK_FILE" ]; then
            print_error "APK файл не найден"
            print_info "Укажите путь к APK файлу как первый аргумент"
            exit 1
        fi
    fi
    
    if [ ! -f "$APK_FILE" ]; then
        print_error "Файл $APK_FILE не существует"
        exit 1
    fi
    
    print_info "Установка: $APK_FILE"
    adb install -r "$APK_FILE"
    
    if [ $? -eq 0 ]; then
        print_success "APK успешно установлен"
    else
        print_error "Ошибка при установке APK"
        exit 1
    fi
}

###############################################################################
# Установка Device Owner
###############################################################################

set_device_owner() {
    print_section "Установка Device Owner"
    
    print_info "Пакет: $PACKAGE_NAME"
    print_info "Компонент: $ADMIN_RECEIVER"
    echo
    
    adb shell dpm set-device-owner "$ADMIN_RECEIVER"
    
    if [ $? -eq 0 ]; then
        print_success "Device Owner установлен успешно!"
        return 0
    else
        print_error "Ошибка при установке Device Owner"
        print_warning "Возможные причины:"
        echo "  1. Устройство уже настроено (user_setup_complete=1)"
        echo "  2. На устройстве уже есть другой Device Owner"
        echo "  3. Пакет не установлен"
        echo
        print_info "Решение: Выполните Factory Reset и повторите"
        return 1
    fi
}

###############################################################################
# Проверка Device Owner
###############################################################################

verify_device_owner() {
    print_section "Проверка Device Owner"
    
    local owners=$(adb shell dpm list-owners)
    
    if echo "$owners" | grep -q "$PACKAGE_NAME"; then
        print_success "Device Owner установлен корректно!"
        echo "$owners" | grep -A1 "Device Owner:"
        return 0
    else
        print_error "Device Owner не установлен"
        return 1
    fi
}

###############################################################################
# Применение конфигурации
###############################################################################

start_kiosk_app() {
    print_section "Запуск приложения Kiosk"
    
    adb shell am start -n "${PACKAGE_NAME}/.MainActivity"
    
    if [ $? -eq 0 ]; then
        print_success "Приложение запущено"
    else
        print_error "Ошибка при запуске приложения"
    fi
}

enable_accessibility_service() {
    print_section "Включение Accessibility Service (опционально)"
    
    echo "Accessibility Service предоставляет дополнительный уровень защиты"
    read -p "Включить? (д/Н) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Дд]$ ]]; then
        print_info "Используйте ADB для включения:"
        echo "  adb shell settings put secure enabled_accessibility_services $PACKAGE_NAME/.service.KioskAccessibilityService"
        echo
        print_info "ИЛИ вручную:"
        echo "  1. Откройте Settings → Accessibility"
        echo "  2. Найдите 'KubMI Kiosk Service'"
        echo "  3. Включите переключатель"
    fi
}

###############################################################################
# Factory Reset (с подтверждением)
###############################################################################

factory_reset() {
    print_section "Factory Reset"
    
    print_warning "Это удалит ВСЕ данные на устройстве!"
    echo
    read -p "Вы уверены? Введите 'да' для подтверждения: " confirm
    
    if [ "$confirm" = "да" ]; then
        print_info "Выполняем Factory Reset..."
        adb shell am broadcast -a android.intent.action.FACTORY_RESET
        
        print_warning "Устройство перезагружается..."
        echo "Дождитесь загрузки и повторите установку Device Owner"
    else
        print_error "Factory Reset отменён"
    fi
}

###############################################################################
# Вывод статуса
###############################################################################

show_status() {
    print_section "Статус системы"
    
    echo "Device Owner:"
    adb shell dpm list-owners
    echo
    
    echo "Приложение KubMI:"
    adb shell pm list packages | grep kubmi
    echo
    
    echo "Версия Android:"
    adb shell getprop ro.build.version.release
    echo
    
    echo "Модель устройства:"
    adb shell getprop ro.model
    echo
    
    echo "Производитель:"
    adb shell getprop ro.manufacturer
}

###############################################################################
# Интерактивное меню
###############################################################################

show_menu() {
    echo
    echo "Выберите действие:"
    echo "  1) Полная установка Device Owner (рекомендуется)"
    echo "  2) Установить APK и Device Owner отдельно"
    echo "  3) Проверить Device Owner"
    echo "  4) Запустить приложение"
    echo "  5) Включить Accessibility Service"
    echo "  6) Показать статус"
    echo "  7) Factory Reset (ОСТОРОЖНО!)"
    echo "  8) Удалить Device Owner"
    echo "  0) Выход"
    echo
}

remove_device_owner() {
    print_section "Удаление Device Owner"
    
    print_warning "Это удалит все Device Owner ограничения!"
    read -p "Продолжить? (д/Н) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Дд]$ ]]; then
        adb shell dpm remove-active-admin "$ADMIN_RECEIVER"
        
        if [ $? -eq 0 ]; then
            print_success "Device Owner удалён"
        else
            print_error "Ошибка при удалении Device Owner"
        fi
    fi
}

###############################################################################
# Главная функция
###############################################################################

main() {
    print_header
    
    check_adb
    check_device_connected
    
    # Если аргумент передан, используем как APK файл
    if [ -n "$1" ] && [ "$1" != "-i" ]; then
        APK_FILE="$1"
    fi
    
    # Интерактивный режим по умолчанию
    if [ "$1" != "-a" ]; then
        while true; do
            show_menu
            read -p "Ваш выбор (0-8): " choice
            
            case $choice in
                1)
                    check_user_setup_complete
                    install_apk
                    set_device_owner
                    verify_device_owner
                    start_kiosk_app
                    enable_accessibility_service
                    ;;
                2)
                    install_apk
                    read -p "Device Owner установлен? (д/Н) " -n 1 -r
                    if [[ $REPLY =~ ^[Дд]$ ]]; then
                        verify_device_owner
                    fi
                    ;;
                3)
                    verify_device_owner
                    ;;
                4)
                    start_kiosk_app
                    ;;
                5)
                    enable_accessibility_service
                    ;;
                6)
                    show_status
                    ;;
                7)
                    factory_reset
                    ;;
                8)
                    remove_device_owner
                    ;;
                0)
                    print_info "Выход"
                    break
                    ;;
                *)
                    print_error "Неправильный выбор"
                    ;;
            esac
        done
    fi
}

# Запуск
main "$@"
