package com.smileattendance.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Routes {
    const val CHECK_IN = "checkin"
    const val HOME = "home"
    const val ENROLL_NEW = "enroll"
    const val ENROLL_REENROLL = "enroll/{empCode}"
    const val EMPLOYEES = "employees"
    const val HISTORY = "history"
    const val SIGN_IN = "signin"
}

/**
 * Check-In is the kiosk root screen — the app always lands there and returns there on its own.
 * Home (enroll/employees/history) is an admin area reached via a small icon on Check-In, not the default view.
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
                onEnroll = { navController.navigate(Routes.ENROLL_NEW) },
                onCheckIn = { navController.popBackStack() },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onEmployees = { navController.navigate(Routes.EMPLOYEES) }
            )
        }
        composable(Routes.ENROLL_NEW) {
            EnrollScreen(
                viewModel = viewModel,
                existingUser = null,
                onDone = { navController.popBackStack() },
                onNeedSignIn = { navController.navigate(Routes.SIGN_IN) }
            )
        }
        composable(
            Routes.ENROLL_REENROLL,
            arguments = listOf(navArgument("empCode") { type = NavType.IntType })
        ) { backStackEntry ->
            val empCode = backStackEntry.arguments?.getInt("empCode") ?: -1
            val users by viewModel.users.collectAsState()
            val existingUser = users.firstOrNull { it.empCode == empCode }
            EnrollScreen(
                viewModel = viewModel,
                existingUser = existingUser,
                onDone = { navController.popBackStack() },
                onNeedSignIn = { navController.navigate(Routes.SIGN_IN) }
            )
        }
        composable(Routes.EMPLOYEES) {
            EmployeesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onReEnroll = { user -> navController.navigate("enroll/${user.empCode}") }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.SIGN_IN) {
            SupervisorSignInScreen(
                viewModel = viewModel,
                onSignedIn = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
