package com.focusguard.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focusguard.app.ui.battles.BattlesScreen
import com.focusguard.app.ui.blocker.BlockerScreen
import com.focusguard.app.ui.focus.FocusScreen
import com.focusguard.app.ui.home.HomeScreen
import com.focusguard.app.ui.settings.SettingsScreen
import com.focusguard.app.ui.stats.StatsScreen

sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Focus    : Screen("focus")
    object Stats    : Screen("stats")
    object Blocker  : Screen("blocker")
    object Battles  : Screen("battles")
    object Settings : Screen("settings")
}

@Composable
fun FocusGuardNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToFocus    = { navController.navigate(Screen.Focus.route) },
                onNavigateToStats    = { navController.navigate(Screen.Stats.route) },
                onNavigateToBlocker  = { navController.navigate(Screen.Blocker.route) },
                onNavigateToBattles  = { navController.navigate(Screen.Battles.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Focus.route)    { FocusScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Stats.route)    { StatsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Blocker.route)  { BlockerScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Battles.route)  { BattlesScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
