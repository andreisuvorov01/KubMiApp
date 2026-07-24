package com.example.kubmi.presentation.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.kubmi.presentation.screens.about.AboutScreen
import com.example.kubmi.presentation.screens.admin.AdminScreen
import com.example.kubmi.presentation.screens.admin.settings.BellScheduleScreen
import com.example.kubmi.presentation.screens.admin.settings.DataSyncScreen
import com.example.kubmi.presentation.screens.admin.settings.KioskModeScreen
import com.example.kubmi.presentation.screens.admin.settings.PermissionsScreen
import com.example.kubmi.presentation.screens.admin.settings.ScreensaverScreen
import com.example.kubmi.presentation.screens.admin.settings.SettingsHubScreen
import com.example.kubmi.presentation.screens.main.MainScreen
import com.example.kubmi.presentation.screens.news.NewsDetailScreen
import com.example.kubmi.presentation.screens.news.NewsScreen
import com.example.kubmi.presentation.screens.schedule.ScheduleDetailScreen
import com.example.kubmi.presentation.screens.schedule.StudentScheduleScreen
import com.example.kubmi.presentation.screens.schedule.TeacherScheduleScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Main.route
) {
    val routeOrder = rememberRouteOrder()
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { full -> full },
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val direction = slideDirection(targetState, initialState, routeOrder)
        // When pushing deeper, current screen gently slides left; when going shallower, slide right.
        if (direction == AnimatedContentTransitionScope.SlideDirection.Left) {
            slideOutHorizontally(
                targetOffsetX = { full -> -full / 3 },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        } else {
            slideOutHorizontally(
                targetOffsetX = { full -> full },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { full -> -full / 3 },
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        // Closing screen leaves left-to-right.
        slideOutHorizontally(
            targetOffsetX = { full -> full },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
            ),
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            val type = it.arguments?.getString("type") ?: ""
            val title = Uri.decode(it.arguments?.getString("title") ?: "")
            val url = Uri.decode(it.arguments?.getString("url") ?: "")
            ScheduleDetailScreen(navController, type, title, url)
        }
        composable(
            Screen.News.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            NewsScreen(navController)
        }
        composable(
            route = Screen.NewsDetail.route,
            arguments = listOf(navArgument("newsId") { type = NavType.StringType })
            ,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            val newsId = Uri.decode(it.arguments?.getString("newsId") ?: "")
            NewsDetailScreen(navController, newsId)
        }
        composable(
            Screen.About.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            AboutScreen(navController)
        }
        composable(
            Screen.Admin.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            AdminScreen(navController)
        }
        composable(
            Screen.SettingsHub.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            SettingsHubScreen(navController)
        }
        composable(
            Screen.SettingsKioskMode.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            KioskModeScreen(navController)
        }
        composable(
            Screen.SettingsPermissions.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            PermissionsScreen(navController)
        }
        composable(
            Screen.SettingsScreensaver.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            ScreensaverScreen(navController)
        }
        composable(
            Screen.SettingsData.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            DataSyncScreen(navController)
        }
        composable(
            Screen.SettingsBellSchedule.route,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            BellScheduleScreen(navController)
        }
    }
}

@Composable
private fun rememberRouteOrder(): Map<String, Int> = remember {
    mapOf(
        Screen.Main.route to 0,
        Screen.News.route to 1,
        Screen.NewsDetail.route to 2,
        Screen.StudentSchedule.route to 1,
        Screen.TeacherSchedule.route to 1,
        Screen.ScheduleDetail.route to 2,
        Screen.About.route to 1,
        Screen.Admin.route to 1,
        Screen.SettingsHub.route to 1,
        Screen.SettingsKioskMode.route to 2,
        Screen.SettingsPermissions.route to 2,
        Screen.SettingsScreensaver.route to 2,
        Screen.SettingsData.route to 2,
        Screen.SettingsBellSchedule.route to 2
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideDirection(
    target: NavBackStackEntry?,
    initial: NavBackStackEntry?,
    order: Map<String, Int>
): AnimatedContentTransitionScope.SlideDirection {
    val targetRoute = target?.destination?.route ?: return AnimatedContentTransitionScope.SlideDirection.Left
    val initialRoute = initial?.destination?.route ?: return AnimatedContentTransitionScope.SlideDirection.Left
    val targetRank = order[targetRoute] ?: 0
    val initialRank = order[initialRoute] ?: 0
    return if (targetRank >= initialRank) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
}
