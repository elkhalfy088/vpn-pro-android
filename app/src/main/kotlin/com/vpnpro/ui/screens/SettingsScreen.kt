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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {

    var killSwitchEnabled  by remember { mutableStateOf(false) }
    var autoConnectEnabled by remember { mutableStateOf(false) }
    var startOnBootEnabled by remember { mutableStateOf(false) }
    var splitTunnelEnabled by remember { mutableStateOf(false) }
    var selectedDns        by remember { mutableStateOf("Cloudflare (1.1.1.1)") }
    var showDnsMenu        by remember { mutableStateOf(false) }

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

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = OnSurface)
                }
                Text(
                    "Settings",
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
            Divider(color = Surface3, thickness = 0.5.dp)

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // ── Security ─────────────────────────────────
                SettingsSectionHeader("Security")

                SettingsToggle(
                    Icons.Default.Security, AccentRed,
                    "Kill Switch",
                    "Block all internet if VPN disconnects — prevents IP leaks",
                    killSwitchEnabled
                ) { killSwitchEnabled = it }

                SettingsToggle(
                    Icons.Default.PrivacyTip, AccentPurple,
                    "DNS Leak Protection",
                    "Force all DNS queries through the VPN tunnel",
                    true
                ) {}

                // ── DNS ───────────────────────────────────────
                SettingsSectionHeader("DNS")

                Box {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBox(Icons.Default.Dns, AccentCyan)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Default DNS", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnSurface)
                            Text(selectedDns, fontSize = 12.sp, color = OnSurfaceVariant)
                        }
                        IconButton(onClick = { showDnsMenu = true }) {
                            Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant)
                        }
                    }
                    DropdownMenu(
                        expanded         = showDnsMenu,
                        onDismissRequest = { showDnsMenu = false }
                    ) {
                        dnsOptions.forEach { dns ->
                            DropdownMenuItem(
                                text = { Text(dns, color = if (dns == selectedDns) AccentCyan else OnSurface) },
                                leadingIcon = if (dns == selectedDns) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = AccentCyan) }
                                } else null,
                                onClick = { selectedDns = dns; showDnsMenu = false }
                            )
                        }
                    }
                }

                // ── Connection ────────────────────────────────
                SettingsSectionHeader("Connection")

                SettingsToggle(
                    Icons.Default.Refresh, AccentGreen,
                    "Auto-Connect",
                    "Automatically connect to last server on app start",
                    autoConnectEnabled
                ) { autoConnectEnabled = it }

                SettingsToggle(
                    Icons.Default.PlayArrow, AccentBlue,
                    "Start on Boot",
                    "Connect to last server when device starts",
                    startOnBootEnabled
                ) { startOnBootEnabled = it }

                // ── Advanced ──────────────────────────────────
                SettingsSectionHeader("Advanced")

                SettingsToggle(
                    Icons.Default.DeviceHub, AccentOrange,
                    "Split Tunneling",
                    "Choose which apps use the VPN",
                    splitTunnelEnabled
                ) { splitTunnelEnabled = it }

                // ── About ─────────────────────────────────────
                SettingsSectionHeader("About")

                InfoRow(Icons.Default.Info,        OnSurfaceVariant, "Version",    "2.0.0")
                InfoRow(Icons.Default.Shield,       AccentCyan,       "Protocol",   "WireGuard")
                InfoRow(Icons.Default.LockOpen,     AccentGreen,      "Encryption", "ChaCha20-Poly1305")
                InfoRow(Icons.Default.Straighten,   OnSurfaceVariant, "Handshake",  "Noise_IKpsk2")

                Spacer(Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(alpha = 0.08f)),
                    shape  = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("How to bypass all restrictions?", fontWeight = FontWeight.SemiBold, color = OnSurface, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This VPN routes ALL your traffic through the server — " +
                            "images, videos, any website or app. You need your own VPS " +
                            "with WireGuard. See the README for full setup instructions.",
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
        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentCyan,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun IconBox(icon: ImageVector, tint: Color) {
    Surface(shape = MaterialTheme.shapes.medium, color = tint.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp), tint = tint) }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector, iconTint: Color,
    title: String, subtitle: String,
    checked: Boolean, onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon, iconTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnSurface)
            Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked, onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan,
                checkedTrackColor = AccentCyan.copy(alpha = 0.35f)
            )
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, iconTint: Color, title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon, iconTint)
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnSurface, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = OnSurfaceVariant)
    }
}
