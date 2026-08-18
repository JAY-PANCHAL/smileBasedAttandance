package com.smileattendance.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Routes {
    const val HOME = "home"
    const val ENROLL = "enroll"
    const val CHECK_IN = "checkin"
    const val HISTORY = "history"
}

@Composable
fun AppNavHost(viewModel: AttendanceViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onEnroll = { navController.navigate(Routes.ENROLL) },
                onCheckIn = { navController.navigate(Routes.CHECK_IN) },
                onHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.ENROLL) {
            EnrollScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
        }
        composable(Routes.CHECK_IN) {
            CheckInScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
