package com.vpnpro.ui.screens

import android.net.VpnService
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.ui.theme.*
import com.vpnpro.ui.viewmodel.MainViewModel
import com.vpnpro.utils.FormatUtils
import com.vpnpro.vpn.VpnState

private const val TAG = "HomeScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFree: () -> Unit
) {
    val context        = LocalContext.current
    val vpnState       by vm.vpnState.collectAsState()
    val vpnStats       by vm.vpnStats.collectAsState()
    val selectedServer by vm.selectedServer.collectAsState()
    val lastError      by vm.lastError.collectAsState()

    // Elapsed connection timer
    val elapsed by produceState(0L, vpnStats.connectedSince) {
        if (vpnStats.connectedSince == 0L) { value = 0L; return@produceState }
        while (true) {
            value = System.currentTimeMillis() - vpnStats.connectedSince
            kotlinx.coroutines.delay(1000)
        }
    }

    // ── VPN permission launcher ───────────────────────────────────────────
    // IMPORTANT: wrap vm.connect() in try-catch — any uncaught exception here
    // will crash the Activity (the callback runs on the main thread).
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                vm.connect()
            } catch (e: Exception) {
                Log.e(TAG, "connect() after permission grant threw: ${e.message}", e)
                com.vpnpro.vpn.XrayVpnService.setError("فشل الاتصال: ${e.message}")
                com.vpnpro.vpn.VpnProService.setError("فشل الاتصال: ${e.message}")
            }
        }
        // If result is not OK the user cancelled the VPN permission dialog — do nothing
    }

    // Pulse animation when CONNECTING
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
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
        VpnState.CONNECTED   -> "Connected"
        VpnState.CONNECTING  -> "Connecting..."
        VpnState.ERROR       -> "Connection Failed"
        else                 -> "Not Connected"
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
            // ── Top bar ───────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "VPN Pro",
                    fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenFree) {
                    Icon(Icons.Default.Public, null, tint = AccentGreen)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, null, tint = OnSurfaceVariant)
                }
            }

            // ── Free banner ───────────────────────────────────────────────
            Card(
                shape  = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    Modifier.padding(12.dp).clickableNoRipple(onOpenFree),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Public, null, Modifier.size(18.dp), tint = AccentGreen)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("🆓 سيرفرات مجانية", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 13.sp)
                        Text("اضغط هنا للاتصال مجاناً — Inwi · IAM · Orange 🇲🇦", fontSize = 11.sp, color = OnSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Power button ───────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(200.dp)
                        .scale(buttonScale)
                        .background(ringColor.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    Modifier
                        .size(170.dp)
                        .scale(buttonScale)
                        .background(ringColor.copy(alpha = 0.18f), CircleShape)
                )

                Button(
                    onClick = {
                        try {
                            when {
                                vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING -> {
                                    vm.disconnect()
                                }
                                selectedServer != null -> {
                                    val prepare = VpnService.prepare(context)
                                    if (prepare != null) {
                                        permLauncher.launch(prepare)
                                    } else {
                                        vm.connect()
                                    }
                                }
                                // No server selected — do nothing (hint shown below)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Power button onClick threw: ${e.message}", e)
                            com.vpnpro.vpn.XrayVpnService.setError("فشل: ${e.message}")
                            com.vpnpro.vpn.VpnProService.setError("فشل: ${e.message}")
                        }
                    },
                    shape  = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (vpnState) {
                            VpnState.CONNECTED  -> AccentGreen.copy(alpha = 0.85f)
                            VpnState.CONNECTING -> AccentCyan.copy(alpha = 0.75f)
                            VpnState.ERROR      -> AccentRed.copy(alpha = 0.8f)
                            else                -> Surface3
                        }
                    ),
                    modifier = Modifier.size(140.dp).scale(buttonScale),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = when (vpnState) {
                            VpnState.CONNECTING -> Icons.Default.Sync
                            VpnState.ERROR      -> Icons.Default.Refresh
                            else                -> Icons.Default.PowerSettingsNew
                        },
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── State label ────────────────────────────────────────────────
            Text(stateLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = stateColor)
            if (vpnState == VpnState.CONNECTED) {
                val serverName = vm.connectedServerName.collectAsState().value
                if (!serverName.isNullOrBlank()) {
                    Text(serverName, fontSize = 13.sp, color = OnSurfaceVariant)
                }
            }
            if (vpnState == VpnState.ERROR && !lastError.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.1f)),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        lastError ?: "",
                        fontSize = 12.sp, color = AccentRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            if (selectedServer == null && vpnState == VpnState.DISCONNECTED) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "اختر سيرفراً من الأسفل أو استخدم السيرفرات المجانية 🆓",
                    fontSize = 12.sp, color = OnSurfaceMuted,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Stats ─────────────────────────────────────────────────────
            if (vpnState == VpnState.CONNECTED) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Time",     FormatUtils.formatDuration(elapsed),        Icons.Default.Timer,        Modifier.weight(1f))
                    StatCard("Download", FormatUtils.formatBytes(vpnStats.bytesIn),  Icons.Default.ArrowDownward, Modifier.weight(1f))
                    StatCard("Upload",   FormatUtils.formatBytes(vpnStats.bytesOut), Icons.Default.ArrowUpward,   Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Selected server card ───────────────────────────────────────
            Card(
                shape  = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Surface1),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickableNoRipple(onOpenServers)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedServer != null) {
                        val s = selectedServer!!
                        Text(s.flag, fontSize = 26.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Text(
                                s.location.ifBlank { s.endpoint.ifBlank { s.xrayLink.take(30) } },
                                fontSize = 12.sp, color = OnSurfaceVariant
                            )
                            val protoLabel = if (s.protocol == "XRAY") "Xray/V2Ray" else "WireGuard"
                            val protoColor = if (s.protocol == "XRAY") AccentGreen else AccentCyan
                            Text(protoLabel, fontSize = 10.sp, color = protoColor,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    } else {
                        Icon(Icons.Outlined.Dns, null, tint = OnSurfaceVariant, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("اختر سيرفراً", fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Text("اضغط لاختيار سيرفر VPN", fontSize = 13.sp, color = OnSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant)
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(8.dp))

            // ── Bottom buttons ─────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenFree,
                    modifier = Modifier.weight(1f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Icon(Icons.Default.Public, null, Modifier.size(16.dp), tint = AccentGreen)
                    Spacer(Modifier.width(6.dp))
                    Text("مجاني 🆓", color = AccentGreen, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onOpenServers,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Dns, null, Modifier.size(16.dp), tint = AccentCyan)
                    Spacer(Modifier.width(6.dp))
                    Text("سيرفراتي", color = AccentCyan, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            indication        = null,
            interactionSource = interactionSource,
            onClick           = onClick
        )
    )
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        shape  = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface1),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(12.dp), tint = AccentCyan)
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, color = OnSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        }
    }
}
