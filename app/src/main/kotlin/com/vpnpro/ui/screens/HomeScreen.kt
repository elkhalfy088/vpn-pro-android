package com.vpnpro.ui.screens

import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.ui.theme.*
import com.vpnpro.ui.viewmodel.MainViewModel
import com.vpnpro.utils.FormatUtils
import com.vpnpro.vpn.VpnState

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context        = LocalContext.current
    val vpnState       by vm.vpnState.collectAsState()
    val vpnStats       by vm.vpnStats.collectAsState()
    val selectedServer by vm.selectedServer.collectAsState()
    val lastError      by vm.lastError.collectAsState()

    // Elapsed timer
    val elapsed by produceState(0L, vpnStats.connectedSince) {
        if (vpnStats.connectedSince == 0L) { value = 0L; return@produceState }
        while (true) {
            value = System.currentTimeMillis() - vpnStats.connectedSince
            kotlinx.coroutines.delay(1000)
        }
    }

    // VPN permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.connect() }

    // Pulsing animation for CONNECTING
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val buttonScale = if (vpnState == VpnState.CONNECTING) pulseScale else 1f

    val ringColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.CONNECTED  -> AccentGreen
            VpnState.CONNECTING -> AccentCyan
            VpnState.ERROR      -> AccentRed
            else                -> Surface3
        },
        animationSpec = tween(600),
        label = "ringColor"
    )

    val stateLabel = when (vpnState) {
        VpnState.CONNECTED     -> "Connected"
        VpnState.CONNECTING    -> "Connecting..."
        VpnState.DISCONNECTING -> "Disconnecting..."
        VpnState.ERROR         -> "Connection Failed"
        else                   -> "Not Connected"
    }
    val stateColor = when (vpnState) {
        VpnState.CONNECTED  -> AccentGreen
        VpnState.CONNECTING -> AccentCyan
        VpnState.ERROR      -> AccentRed
        else                -> OnSurfaceMuted
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, BgMid)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ───────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "VPN Pro",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, null, tint = OnSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Big connect button ────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                Surface(
                    shape = CircleShape,
                    color = ringColor.copy(alpha = 0.10f),
                    modifier = Modifier.size(200.dp).scale(buttonScale)
                ) {}
                // Middle ring
                Surface(
                    shape = CircleShape,
                    color = ringColor.copy(alpha = 0.20f),
                    modifier = Modifier.size(168.dp).scale(buttonScale)
                ) {}
                // Main button
                Button(
                    onClick = {
                        when (vpnState) {
                            VpnState.CONNECTED,
                            VpnState.CONNECTING -> vm.disconnect()
                            else -> {
                                if (selectedServer == null) {
                                    onOpenServers()
                                } else {
                                    val permIntent = VpnService.prepare(context)
                                    if (permIntent != null) {
                                        permLauncher.launch(permIntent)
                                    } else {
                                        vm.connect()
                                    }
                                }
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (vpnState) {
                            VpnState.CONNECTED,
                            VpnState.CONNECTING -> AccentRed.copy(alpha = 0.85f)
                            VpnState.ERROR      -> AccentOrange.copy(alpha = 0.85f)
                            else                -> AccentCyan.copy(alpha = 0.9f)
                        }
                    ),
                    modifier = Modifier.size(132.dp).scale(buttonScale),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (vpnState) {
                                VpnState.CONNECTED,
                                VpnState.CONNECTING -> Icons.Default.Stop
                                else -> Icons.Default.PowerSettingsNew
                            },
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (vpnState) {
                                VpnState.CONNECTED  -> "Stop"
                                VpnState.CONNECTING -> "Cancel"
                                else                -> "Start"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // State label
            Text(
                stateLabel,
                color = stateColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )

            // Timer when connected
            if (vpnState == VpnState.CONNECTED) {
                Spacer(Modifier.height(4.dp))
                Text(
                    FormatUtils.formatDuration(elapsed),
                    color = OnSurfaceVariant,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Error message
            if (vpnState == VpnState.ERROR && lastError != null) {
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AccentRed.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline, null,
                            tint = AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            lastError ?: "",
                            color = AccentRed,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Stats row (connected only) ────────────────────
            if (vpnState == VpnState.CONNECTED) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Upload",
                        value = FormatUtils.formatBytes(vpnStats.bytesOut),
                        icon  = Icons.Default.Upload,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Download",
                        value = FormatUtils.formatBytes(vpnStats.bytesIn),
                        icon  = Icons.Default.Download,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Selected server card ──────────────────────────
            Card(
                onClick = onOpenServers,
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Surface2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedServer != null) {
                        Text(selectedServer!!.flag, fontSize = 34.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                selectedServer!!.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = OnSurface
                            )
                            Text(
                                selectedServer!!.location.ifBlank { selectedServer!!.endpoint },
                                fontSize = 13.sp,
                                color = OnSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = AccentCyan.copy(alpha = 0.12f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "  WireGuard  ",
                                    fontSize = 10.sp,
                                    color = AccentCyan,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    } else {
                        Icon(
                            Icons.Outlined.Dns, null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Select a Server",
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Text(
                                "Tap to choose a VPN server",
                                fontSize = 13.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant)
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))

            // ── Bottom shortcut ───────────────────────────────
            TextButton(
                onClick = onOpenServers,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Dns, null, Modifier.size(16.dp), tint = AccentCyan)
                Spacer(Modifier.width(6.dp))
                Text("Manage Servers", color = AccentCyan, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface1),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(13.dp), tint = AccentCyan)
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, color = OnSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        }
    }
}
