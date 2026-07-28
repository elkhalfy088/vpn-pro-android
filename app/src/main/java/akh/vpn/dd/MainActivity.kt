package akh.vpn.dd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import akh.vpn.dd.ui.screens.HomeScreen
import akh.vpn.dd.ui.screens.SetupScreen
import akh.vpn.dd.ui.theme.VpnProTheme
import akh.vpn.dd.viewmodel.VpnViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VpnProTheme(darkTheme = true) {
                VpnProApp()
            }
        }
    }
}

@Composable
fun VpnProApp() {
    val viewModel: VpnViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.isSetupRequired -> {
            // First run: show setup screen
            SetupScreen(viewModel = viewModel)
        }
        else -> {
            // Config exists: show main VPN screen
            HomeScreen(viewModel = viewModel)
        }
    }
}
