package com.vpnpro.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.ui.theme.*
import com.vpnpro.ui.viewmodel.MainViewModel
import com.vpnpro.utils.FormatUtils
import com.vpnpro.vpn.VpnState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ctx            = LocalContext.current
    val vpnState       by vm.vpnState.collectAsState()
    val vpnStats       by vm.vpnStats.collectAsState()
    val selectedServer by vm.selectedServer.collectAsState()
    val servers        by vm.servers.collectAsState()

    // Auto-select first server
    LaunchedEffect(servers) {
        if (selectedServer == null && servers.isNotEmpty()) {
            vm.selectServer(servers.first())
        }
    }

    // Connection timer
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(vpnState) {
        if (vpnState == VpnState.CONNECTED) {
            elapsed = 0L
            while (isActive) { delay(1000); elapsed++ }
        } else { elapsed = 0L }
    }

    // VPN permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.connect()
    }

    fun onConnectClick() {
        when (vpnState) {
            VpnState.CONNECTED, VpnState.CONNECTING -> vm.disconnect()
            else -> {
                val intent = VpnService.prepare(ctx)
                if (intent != null) permissionLauncher.launch(intent)
                else vm.connect()
            }
        }
    }

    val stateColor = when (vpnState) {
        VpnState.CONNECTED    -> AccentGreen
        VpnState.CONNECTING   -> AccentYellow
        VpnState.ERROR        -> AccentRed
        VpnState.DISCONNECTED -> Color(0xFF4A5568)
    }

    val stateLabel = when (vpnState) {
        VpnState.CONNECTED    -> "Protected"
        VpnState.CONNECTING   -> "Connecting…"
        VpnState.ERROR        -> "Failed"
        VpnState.DISCONNECTED -> "Not Protected"
    }

    // Pulse animation
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.12f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0E1A), Color(0xFF0D1526), Color(0xFF0A0E1A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "VPN Pro",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = AccentCyan
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, null, tint = OnSurfaceVariant)
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Big connect button ────────────────────────────────
            val btnScale = if (vpnState == VpnState.CONNECTED) pulseScale else 1f
            Box(
                Modifier.size(220.dp).scale(btnScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Surface(
                    shape = CircleShape,
                    color = stateColor.copy(alpha = 0.08f),
                    modifier = Modifier.size(220.dp)
                ) {}
                // Middle ring
                Surface(
                    shape = CircleShape,
                    color = stateColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(176.dp)
                ) {}
                // Inner button
                Button(
                    onClick = ::onConnectClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = stateColor),
                    modifier = Modifier.size(136.dp),
                    elevation = ButtonDefaults.buttonElevation(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (vpnState) {
                                VpnState.CONNECTED, VpnState.CONNECTING -> Icons.Default.Stop
                                else -> Icons.Default.PowerSettingsNew
                            },
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING)
                                "Stop" else "Start",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // State label
            Text(stateLabel, color = stateColor, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)

            // Timer
            if (vpnState == VpnState.CONNECTED) {
                Spacer(Modifier.height(4.dp))
                Text(
                    FormatUtils.formatDuration(elapsed),
                    color = OnSurfaceVariant, fontSize = 20.sp, fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Stats row (only when connected) ───────────────────
            if (vpnState == VpnState.CONNECTED) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        "↑ Upload",
                        FormatUtils.formatBytes(vpnStats.bytesOut),
                        Modifier.weight(1f)
                    )
                    StatCard(
                        "↓ Download",
                        FormatUtils.formatBytes(vpnStats.bytesIn),
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Selected server card ──────────────────────────────
            Card(
                onClick = onOpenServers,
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Surface2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedServer != null) {
                        Text(selectedServer!!.flag, fontSize = 32.sp)
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
                        }
                    } else {
                        Icon(Icons.Default.DnsOutlined, null, tint = OnSurfaceVariant, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Select a Server", fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Text("Tap to choose", fontSize = 13.sp, color = OnSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant)
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Bottom shortcut ────────────────────────────────────
            TextButton(onClick = onOpenServers, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Default.Dns, null, Modifier.size(16.dp), tint = AccentCyan)
                Spacer(Modifier.width(6.dp))
                Text("Manage Servers", color = AccentCyan, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface1),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        }
    }
}
