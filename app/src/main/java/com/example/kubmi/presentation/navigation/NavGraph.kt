package com.example.kubmi.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.kubmi.presentation.screens.about.AboutScreen
import com.example.kubmi.presentation.screens.admin.AdminScreen
import com.example.kubmi.presentation.screens.main.MainScreen
import com.example.kubmi.presentation.screens.news.NewsDetailScreen
import com.example.kubmi.presentation.screens.news.NewsScreen
import com.example.kubmi.presentation.screens.schedule.StudentScheduleScreen
import com.example.kubmi.presentation.screens.schedule.TeacherScheduleScreen
import com.example.kubmi.presentation.screens.schedule.ScheduleDetailScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController)
        }
        composable(Screen.StudentSchedule.route) {
            StudentScheduleScreen(navController)
        }
        composable(Screen.TeacherSchedule.route) {
            TeacherScheduleScreen(navController)
        }
        composable(
            route = Screen.ScheduleDetail.route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) {
            val type = it.arguments?.getString("type") ?: ""
            val title = Uri.decode(it.arguments?.getString("title") ?: "")
            val url = Uri.decode(it.arguments?.getString("url") ?: "")
            ScheduleDetailScreen(navController, type, title, url)
        }
        composable(Screen.News.route) {
            NewsScreen(navController)
        }
        composable(
            route = Screen.NewsDetail.route,
            arguments = listOf(navArgument("newsId") { type = NavType.StringType })
        ) {
            NewsDetailScreen(navController, it.arguments?.getString("newsId") ?: "")
        }
        composable(Screen.About.route) {
            AboutScreen(navController)
        }
        composable(Screen.Admin.route) {
            AdminScreen(navController)
        }
    }
}
