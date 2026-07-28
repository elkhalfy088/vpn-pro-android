package com.vpnpro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vpnpro.ui.screens.*
import com.vpnpro.ui.viewmodel.MainViewModel

object Routes {
    const val HOME       = "home"
    const val SERVERS    = "servers"
    const val ADD_SERVER = "add_server"
    const val SETTINGS   = "settings"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val vm: MainViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                vm              = vm,
                onOpenServers   = { navController.navigate(Routes.SERVERS) },
                onOpenSettings  = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SERVERS) {
            ServersScreen(
                vm          = vm,
                onBack      = { navController.popBackStack() },
                onAddServer = { navController.navigate(Routes.ADD_SERVER) }
            )
        }
        composable(Routes.ADD_SERVER) {
            AddServerScreen(
                vm     = vm,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
