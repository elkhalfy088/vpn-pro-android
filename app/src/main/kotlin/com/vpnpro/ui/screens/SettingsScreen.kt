package com.vpnpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {

    // In a real app these come from DataStore; kept local for now
    var killSwitchEnabled   by remember { mutableStateOf(false) }
    var autoConnectEnabled  by remember { mutableStateOf(false) }
    var startOnBootEnabled  by remember { mutableStateOf(false) }
    var splitTunnelEnabled  by remember { mutableStateOf(false) }
    var selectedDns         by remember { mutableStateOf("Cloudflare (1.1.1.1)") }
    var showDnsMenu         by remember { mutableStateOf(false) }

    val dnsOptions = listOf(
        "Cloudflare (1.1.1.1)",
        "Google (8.8.8.8)",
        "Quad9 (9.9.9.9)",
        "AdGuard (94.140.14.14)",
        "OpenDNS (208.67.222.222)"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, BgMid)))
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // ── Top bar ──────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = OnSurface) }
                Text(
                    "Settings",
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
            HorizontalDivider(color = Surface3, thickness = 0.5.dp)

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // ── Security ─────────────────────────────────
                SettingsSectionHeader("Security")

                SettingsToggleItem(
                    icon    = Icons.Default.Security,
                    iconTint = AccentRed,
                    title   = "Kill Switch",
                    subtitle = "Block all internet if VPN disconnects — prevents IP leaks",
                    checked  = killSwitchEnabled,
                    onCheckedChange = { killSwitchEnabled = it }
                )
                SettingsToggleItem(
                    icon    = Icons.Default.PrivacyTip,
                    iconTint = AccentPurple,
                    title   = "DNS Leak Protection",
                    subtitle = "Force all DNS queries through the VPN tunnel",
                    checked  = true,
                    onCheckedChange = {}
                )

                // ── DNS ───────────────────────────────────────
                SettingsSectionHeader("DNS")

                Box {
                    SettingsClickItem(
                        icon    = Icons.Default.Dns,
                        iconTint = AccentCyan,
                        title   = "Default DNS",
                        subtitle = selectedDns,
                        onClick  = { showDnsMenu = true }
                    )
                    DropdownMenu(
                        expanded  = showDnsMenu,
                        onDismissRequest = { showDnsMenu = false },
                        containerColor = Surface2
                    ) {
                        dnsOptions.forEach { dns ->
                            DropdownMenuItem(
                                text = { Text(dns, color = if (dns == selectedDns) AccentCyan else OnSurface) },
                                leadingIcon = {
                                    if (dns == selectedDns)
                                        Icon(Icons.Default.CheckCircle, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                },
                                onClick = { selectedDns = dns; showDnsMenu = false }
                            )
                        }
                    }
                }

                // ── Connection ────────────────────────────────
                SettingsSectionHeader("Connection")

                SettingsToggleItem(
                    icon    = Icons.Default.AutoMode,
                    iconTint = AccentGreen,
                    title   = "Auto-Connect",
                    subtitle = "Automatically connect to last server on app start",
                    checked  = autoConnectEnabled,
                    onCheckedChange = { autoConnectEnabled = it }
                )
                SettingsToggleItem(
                    icon    = Icons.Default.StartRounded,
                    iconTint = AccentBlue,
                    title   = "Start on Boot",
                    subtitle = "Connect to last server when device starts",
                    checked  = startOnBootEnabled,
                    onCheckedChange = { startOnBootEnabled = it }
                )

                // ── Advanced ──────────────────────────────────
                SettingsSectionHeader("Advanced")

                SettingsToggleItem(
                    icon    = Icons.Default.CallSplit,
                    iconTint = AccentOrange,
                    title   = "Split Tunneling",
                    subtitle = "Choose which apps use the VPN",
                    checked  = splitTunnelEnabled,
                    onCheckedChange = { splitTunnelEnabled = it }
                )

                // ── Info ──────────────────────────────────────
                SettingsSectionHeader("About")

                SettingsInfoItem(
                    icon    = Icons.Default.Info,
                    iconTint = OnSurfaceVariant,
                    title   = "Version",
                    value   = "2.0.0"
                )
                SettingsInfoItem(
                    icon    = Icons.Default.Shield,
                    iconTint = AccentCyan,
                    title   = "Protocol",
                    value   = "WireGuard"
                )
                SettingsInfoItem(
                    icon    = Icons.Default.Speed,
                    iconTint = AccentGreen,
                    title   = "Encryption",
                    value   = "ChaCha20-Poly1305"
                )

                Spacer(Modifier.height(24.dp))

                // ── Help banner ───────────────────────────────
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(alpha = 0.08f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LightbulbOutline, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("How to bypass all restrictions?", fontWeight = FontWeight.SemiBold, color = OnSurface, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This VPN routes ALL your traffic through the server — " +
                            "images, videos, any website or app. You need your own VPS server with WireGuard. " +
                            "See the server setup guide in the README for full instructions.",
                            color = OnSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = AccentCyan, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = iconTint)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnSurface)
            Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = iconTint)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnSurface)
            Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onClick) {
            Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    value: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = iconTint.copy(alpha = 0.10f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = iconTint)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnSurface, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = OnSurfaceVariant)
    }
}
