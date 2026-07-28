package com.vpnpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1526))))
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
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Divider(color = Surface3, thickness = 0.5.dp)

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("VPN")
                SettingItem(Icons.Default.Security, "Kill Switch", "Block traffic if VPN drops", subtitle2 = "Coming soon")
                SettingItem(Icons.Default.Speed, "Protocol", "WireGuard (UDP)")
                SettingItem(Icons.Default.Dns, "DNS", "1.1.1.1, 1.0.0.1")

                Spacer(Modifier.height(8.dp))
                SectionTitle("About")
                SettingItem(Icons.Default.Info, "Version", "VPN Pro 1.0.0")
                SettingItem(Icons.Default.Code, "Protocol", "WireGuard® open source")
                SettingItem(Icons.Default.Shield, "Encryption", "ChaCha20-Poly1305 + Curve25519")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 12.sp, color = AccentCyan, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
}

@Composable
private fun SettingItem(icon: ImageVector, title: String, subtitle: String, subtitle2: String = "") {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), tint = AccentCyan)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = OnSurface)
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
                if (subtitle2.isNotBlank())
                    Text(subtitle2, fontSize = 11.sp, color = OnSurfaceMuted)
            }
        }
    }
}
