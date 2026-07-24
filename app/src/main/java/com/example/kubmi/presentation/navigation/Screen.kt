package com.example.kubmi.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object StudentSchedule : Screen("student_schedule")
    object TeacherSchedule : Screen("teacher_schedule")
    object ScheduleDetail : Screen("schedule_detail/{type}/{title}/{url}") {
        fun createRoute(type: String, title: String, url: String): String {
            val safeTitle = Uri.encode(title)
            val safeUrl = Uri.encode(url)
            return "schedule_detail/$type/$safeTitle/$safeUrl"
        }
    }
    object News : Screen("news")
    object NewsDetail : Screen("news_detail/{newsId}") {
        fun createRoute(newsId: String): String {
            // newsId is a URL (contains '/'), so it must be encoded for navigation route segments.
            val safeId = Uri.encode(newsId)
            return "news_detail/$safeId"
        }
    }
    object About : Screen("about")
    object Admin : Screen("admin")

    // Settings hub and category screens
    object SettingsHub : Screen("settings_hub")
    object SettingsKioskMode : Screen("settings_kiosk_mode")
    object SettingsPermissions : Screen("settings_permissions")
    object SettingsScreensaver : Screen("settings_screensaver")
    object SettingsData : Screen("settings_data")
    object SettingsBellSchedule : Screen("settings_bell_schedule")
}
