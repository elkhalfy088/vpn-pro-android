package akh.vpn.dd.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import akh.vpn.dd.viewmodel.VpnStatus
import akh.vpn.dd.viewmodel.VpnViewModel

@Composable
fun HomeScreen(viewModel: VpnViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted()
        }
    }

    // Check VPN permission before connecting
    fun connectWithPermission() {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            viewModel.connectVpn()
        }
    }

    // Pulsing animation for the connect button
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val isConnected = uiState.status == VpnStatus.CONNECTED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Vpn Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "الاتصال آمن ومشفر",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (uiState.status) {
                        VpnStatus.CONNECTED    -> Color(0xFF00E5B0).copy(alpha = 0.15f)
                        VpnStatus.CONNECTING   -> Color(0xFFFFA000).copy(alpha = 0.15f)
                        VpnStatus.ERROR        -> MaterialTheme.colorScheme.errorContainer
                        else                   -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        when (uiState.status) {
                            VpnStatus.CONNECTED  -> "متصل"
                            VpnStatus.CONNECTING -> "جاري..."
                            VpnStatus.ERROR      -> "خطأ"
                            else                 -> "غير متصل"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = when (uiState.status) {
                            VpnStatus.CONNECTED  -> Color(0xFF00E5B0)
                            VpnStatus.CONNECTING -> Color(0xFFFFA000)
                            VpnStatus.ERROR      -> MaterialTheme.colorScheme.error
                            else                 -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Server info card
            uiState.config?.let { config ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Dns, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                config.serverHost,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "منفذ: ${config.serverPort}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Shield, null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    config.decoyDomain.take(12),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // Main Connect Button
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring (animated when connecting)
                if (uiState.status == VpnStatus.CONNECTING) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {}
                }

                // Button
                val buttonColor = when (uiState.status) {
                    VpnStatus.CONNECTED  -> Color(0xFF00E5B0)
                    VpnStatus.CONNECTING -> Color(0xFFFFA000)
                    VpnStatus.ERROR      -> MaterialTheme.colorScheme.error
                    else                 -> MaterialTheme.colorScheme.primary
                }

                Surface(
                    onClick = {
                        if (uiState.status == VpnStatus.DISCONNECTED || uiState.status == VpnStatus.ERROR) {
                            connectWithPermission()
                        } else {
                            viewModel.disconnectVpn()
                        }
                    },
                    shape = CircleShape,
                    color = buttonColor,
                    modifier = Modifier.size(140.dp),
                    shadowElevation = if (isConnected) 12.dp else 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.Power,
                                null,
                                tint = if (isConnected) Color(0xFF003328) else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when (uiState.status) {
                                    VpnStatus.CONNECTED  -> "قطع"
                                    VpnStatus.CONNECTING -> "..."
                                    else                 -> "اتصال"
                                },
                                color = if (isConnected) Color(0xFF003328) else MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Timer
            if (uiState.connectedSeconds > 0) {
                Text(
                    formatTime(uiState.connectedSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF00E5B0),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(40.dp))

            // Stats row
            AnimatedVisibility(
                visible = isConnected,
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("↑", formatBytes(uiState.bytesOut), "رفع")
                    StatCard("↓", formatBytes(uiState.bytesIn), "تنزيل")
                    StatCard("🏓", "${uiState.latencyMs}ms", "البينغ")
                }
            }

            // Error message
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: String, value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTime(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}
