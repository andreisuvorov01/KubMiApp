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
        fun createRoute(newsId: String) = "news_detail/$newsId"
    }
    object About : Screen("about")
    object Admin : Screen("admin")
}
