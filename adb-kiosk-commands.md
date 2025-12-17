# ADB helpers for закрепления лаунчера/киоска (без Device Owner)

Подходит, если есть доступ к ADB по USB или `adb connect <tv_ip>`.

## Проверка и выбор домашнего экрана
- Подключиться: `adb connect <tv_ip>`
- Посмотреть, кто сейчас выбран как дом/home:  
  `adb shell cmd package get-home-activity`
- Открыть системный выбор лаунчера:  
  `adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME`

## Принудительно выбрать это приложение как HOME
- Установить home-activity:  
  `adb shell cmd package set-home-activity com.example.kubmi/.MainActivity`

## Отключить штатный лаунчер (аккуратно, можно вернуть)
- Узнать пакет штатного лаунчера (часто `com.android.tv.launcher` или `com.google.android.tvlauncher`):  
  `adb shell pm list packages | findstr launcher`
- Временно отключить:  
  `adb shell pm disable-user --user 0 <stock.launcher.pkg>`
- Вернуть обратно при необходимости:  
  `adb shell pm enable <stock.launcher.pkg>`

## Вернуть приложение на передний план вручную
- Запустить нашу активность:  
  `adb shell am start -n com.example.kubmi/.MainActivity`
- Или через monkey:  
  `adb shell monkey -p com.example.kubmi -c android.intent.category.LAUNCHER 1`

