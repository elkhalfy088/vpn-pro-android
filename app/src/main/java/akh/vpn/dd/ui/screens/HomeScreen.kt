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
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (uiState.status) {
                        VpnStatus.CONNECTED    -> Color(0xFF00E5B0).copy(alpha = 0.15f)
                        VpnStatus.CONNECTING   -> Color(0xFF4FC3F7).copy(alpha = 0.15f)
                        VpnStatus.ERROR        -> MaterialTheme.colorScheme.errorContainer
                        VpnStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (uiState.status) {
                            VpnStatus.CONNECTED    -> "متصل"
                            VpnStatus.CONNECTING   -> "جاري الاتصال…"
                            VpnStatus.ERROR        -> "خطأ"
                            VpnStatus.DISCONNECTED -> "غير متصل"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when (uiState.status) {
                            VpnStatus.CONNECTED    -> Color(0xFF00E5B0)
                            VpnStatus.CONNECTING   -> Color(0xFF4FC3F7)
                            VpnStatus.ERROR        -> MaterialTheme.colorScheme.error
                            VpnStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Connect button with pulse ring
            Box(contentAlignment = Alignment.Center) {
                // Outer pulse ring (disconnected state)
                if (!isConnected && uiState.status != VpnStatus.CONNECTING) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .scale(pulseScale)
                            .background(
                                Color(0xFF00E5B0).copy(alpha = 0.07f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .background(
                                Color(0xFF00E5B0).copy(alpha = 0.05f),
                                CircleShape
                            )
                    )
                }

                // Main button
                Surface(
                    shape = CircleShape,
                    color = when (uiState.status) {
                        VpnStatus.CONNECTED  -> Color(0xFF00E5B0)
                        VpnStatus.ERROR      -> MaterialTheme.colorScheme.errorContainer
                        else                 -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .size(130.dp)
                        .clickable(enabled = uiState.status != VpnStatus.CONNECTING) {
                            when (uiState.status) {
                                VpnStatus.CONNECTED    -> viewModel.disconnectVpn()
                                VpnStatus.ERROR,
                                VpnStatus.DISCONNECTED -> connectWithPermission()
                                else -> Unit
                            }
                        },
                    shadowElevation = if (isConnected) 14.dp else 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = when (uiState.status) {
                                    VpnStatus.CONNECTED -> Color(0xFF003328)
                                    VpnStatus.ERROR     -> MaterialTheme.colorScheme.error
                                    else                -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = when (uiState.status) {
                                    VpnStatus.CONNECTED  -> "إيقاف"
                                    VpnStatus.CONNECTING -> "..."
                                    VpnStatus.ERROR      -> "إعادة"
                                    else                 -> "اتصال"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (uiState.status) {
                                    VpnStatus.CONNECTED -> Color(0xFF003328)
                                    VpnStatus.ERROR     -> MaterialTheme.colorScheme.error
                                    else                -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                // Connecting progress ring
                if (uiState.status == VpnStatus.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(152.dp),
                        color = Color(0xFF4FC3F7),
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round
                    )
                }

                // Connected glow ring
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .border(2.dp, Color(0xFF00E5B0).copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Server info card
            uiState.config?.let { config ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${config.serverHost}:${config.serverPort}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "SNI: ${config.decoyDomain}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF00E5B0) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stats (visible when connected)
            AnimatedVisibility(
                visible = isConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("↓", formatBytes(uiState.bytesIn), "استقبال")
                    StatCard("↑", formatBytes(uiState.bytesOut), "إرسال")
                    StatCard("⏱", formatTime(uiState.connectedSeconds), "الوقت")
                }
            }

            Spacer(Modifier.weight(1f))

            // Error banner
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = viewModel::clearError,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
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
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
