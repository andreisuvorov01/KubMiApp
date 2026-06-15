# KubMiApp audit: architecture, news, kiosk mode

## Stack and entry points
- Android application written in Kotlin with Jetpack Compose / TV Material UI.
- Hilt is used for dependency injection, Room for local persistence, WorkManager for background sync, Retrofit/OkHttp dependencies are present, and JSoup is used for HTML parsing.
- Main runtime entry point: `MainActivity` (`singleTask`, HOME/LAUNCHER/LEANBACK_LAUNCHER intents).
- Application entry point: `KubMiApplication`.
- Boot re-entry: `BootReceiver` restarts the kiosk flow after boot/package replacement.

## Routing and authorization
- Routing is Compose Navigation via `NavGraph` and the `Screen` sealed routes.
- Admin access is implemented in the admin screen/view model with secure preferences; no server-side authorization layer exists.

## News implementation
- UI: `presentation/screens/news/NewsScreen.kt`, `NewsDetailScreen.kt`, `NewsViewModel.kt`.
- Domain/model/cache: `domain/model/News.kt`, `NewsContentBlock.kt`, `data/local/entity/NewsEntity.kt`, `NewsDao.kt`.
- Repository/cache orchestration: `data/repository/NewsRepository.kt`.
- Scraping/parser: `data/remote/WebScraper.kt`.
- Background sync: `data/worker/DataSyncWorker.kt`.

## Kiosk implementation
- Main activity guards: `MainActivity.kt` blocks Back/Home/Recents-style keys, restores focus, starts guard services and immersive mode.
- Immersive/lock-task helper: `util/KioskManager.kt`.
- Foreground watchdog: `service/KioskService.kt`.
- Global key/window monitoring: `service/KioskAccessibilityService.kt`.
- Boot, device-admin and overlay support: `receiver/BootReceiver.kt`, `receiver/DeviceAdminReceiver.kt`, `OverlayService.kt`.
- User-facing permission controls: `presentation/screens/admin/KioskSettingsScreen.kt`, `util/KioskPermissionManager.kt`.

## External fetching / parsing locations
- `WebScraper.kt`: news list/details, schedules, about page, GO/CHS PDFs.
- `ScheduleRepository.kt`: schedule index pages are fetched directly with JSoup.
- `ImageProcessingUtils.kt`: downloads news images.
- `PdfToImageConverter.kt`: downloads and converts PDF files.
- `DataSyncWorker.kt`: syncs news/articles/images in the background.

## Risks and limitations
- Android cannot reliably block `Ctrl+Alt+Del`, hard power keys, OEM system overlays, or task killing unless the app is installed as Device Owner / dedicated device.
- Lock-task mode is strongest only when Device Owner (or explicitly allow-listed by policy). Device-admin alone is not equivalent to Device Owner on modern Android.
- Accessibility key filtering depends on the user enabling the service and can vary by OEM keyboard/firmware.
- Background activity launch restrictions on newer Android versions can delay recovery if the app loses foreground without HOME/default launcher and usage-access setup.
- News scraping depends on the public WordPress/Elementor HTML structure at `https://kubmi.ru/novosti/`; parser selectors are intentionally broad, but a full site redesign may still require updates.
