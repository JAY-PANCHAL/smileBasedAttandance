package com.smileattendance.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Routes {
    const val CHECK_IN = "checkin"
    const val HOME = "home"
    const val ENROLL = "enroll"
    const val HISTORY = "history"
}

/**
 * Check-In is the kiosk root screen — the app always lands there and returns there on its own.
 * Home (enroll/history/export) is an admin area reached via a small icon on Check-In, not the default view.
 */
@Composable
fun AppNavHost(viewModel: AttendanceViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CHECK_IN) {
        composable(Routes.CHECK_IN) {
            CheckInScreen(
                viewModel = viewModel,
                onOpenAdminMenu = { navController.navigate(Routes.HOME) }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onEnroll = { navController.navigate(Routes.ENROLL) },
                onCheckIn = { navController.popBackStack() },
                onHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.ENROLL) {
            EnrollScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
