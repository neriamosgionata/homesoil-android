package com.homesoil.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.homesoil.app.data.repository.HomesoilRepository
import com.homesoil.app.ui.screens.dashboard.DashboardScreen
import com.homesoil.app.ui.screens.dashboard.DashboardViewModel
import com.homesoil.app.ui.screens.login.LoginScreen
import com.homesoil.app.ui.screens.login.LoginViewModel
import com.homesoil.app.ui.screens.scripts.ScriptEditorScreen
import com.homesoil.app.ui.screens.scripts.ScriptEditorViewModel
import com.homesoil.app.ui.screens.scripts.ScriptsScreen
import com.homesoil.app.ui.screens.scripts.ScriptsViewModel
import com.homesoil.app.ui.screens.sensor.SensorDetailScreen
import com.homesoil.app.ui.screens.sensor.SensorDetailViewModel
import com.homesoil.app.ui.screens.settings.SettingsScreen
import com.homesoil.app.ui.screens.settings.SettingsViewModel

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val SENSOR_DETAIL = "sensor/{sensorId}"
    const val SCRIPTS = "scripts"
    const val SCRIPT_EDITOR = "script/{scriptId}"
    const val SCRIPT_NEW = "script/new"
    const val SETTINGS = "settings"

    fun sensorDetail(sensorId: Int) = "sensor/$sensorId"
    fun scriptEditor(scriptId: Int) = "script/$scriptId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    repository: HomesoilRepository,
    startDestination: String = Routes.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            val viewModel = remember { LoginViewModel(repository) }
            LoginScreen(
                viewModel = viewModel,
                onConnected = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            val viewModel = remember { DashboardViewModel(repository) }
            DashboardScreen(
                viewModel = viewModel,
                onSensorClick = { sensorId ->
                    navController.navigate(Routes.sensorDetail(sensorId))
                },
                onScriptsClick = {
                    navController.navigate(Routes.SCRIPTS)
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onDisconnected = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.SENSOR_DETAIL,
            arguments = listOf(navArgument("sensorId") { type = NavType.IntType })
        ) { backStackEntry ->
            val sensorId = backStackEntry.arguments?.getInt("sensorId") ?: return@composable
            val viewModel = remember { SensorDetailViewModel(repository, sensorId) }
            SensorDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SCRIPTS) {
            val viewModel = remember { ScriptsViewModel(repository) }
            ScriptsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onScriptClick = { scriptId ->
                    navController.navigate(Routes.scriptEditor(scriptId))
                },
                onNewScript = {
                    navController.navigate(Routes.SCRIPT_NEW)
                }
            )
        }

        composable(
            route = Routes.SCRIPT_EDITOR,
            arguments = listOf(navArgument("scriptId") { type = NavType.IntType })
        ) { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getInt("scriptId") ?: return@composable
            val viewModel = remember { ScriptEditorViewModel(repository, scriptId) }
            ScriptEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SCRIPT_NEW) {
            val viewModel = remember { ScriptEditorViewModel(repository, null) }
            ScriptEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel = remember { SettingsViewModel(repository) }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
