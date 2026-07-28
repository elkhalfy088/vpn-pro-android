package com.vpnpro.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.ui.theme.*
import com.vpnpro.ui.viewmodel.MainViewModel
import com.vpnpro.vpn.FreeConfigFetcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeServersScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val context  = LocalContext.current
    val configs  by vm.freeConfigs.collectAsState()
    val loading  by vm.freeConfigsLoading.collectAsState()
    val error    by vm.freeConfigsError.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }  // 0=سيرفرات مجانية 1=Bug Hosts

    // ── VPN permission handling ────────────────────────────────────────────
    // When connectFreeConfig returns true (needs permission), this launcher
    // shows the system VPN consent dialog. After the user grants permission,
    // we call onVpnPermissionGranted() to complete the connection.
    val vpnPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.onVpnPermissionGranted()
        } else {
            vm.clearPendingFreeConfig()
        }
    }

    LaunchedEffect(Unit) {
        if (configs.isEmpty()) vm.fetchFreeConfigs()
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgDark, BgMid)))) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            // ── Top bar ─────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = OnSurface)
                }
                Text(
                    "🆓 سيرفرات مجانية",
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                if (!loading) {
                    IconButton(onClick = { vm.fetchFreeConfigs() }) {
                        Icon(Icons.Default.Refresh, null, tint = AccentCyan)
                    }
                }
            }
            HorizontalDivider(color = Surface3, thickness = 0.5.dp)

            // ── Tabs ────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = activeTab,
                containerColor   = BgDark,
                contentColor     = AccentCyan
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("سيرفرات مجانية", fontSize = 13.sp)
                    }
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bug Hosts 🇲🇦", fontSize = 13.sp)
                    }
                }
            }

            when (activeTab) {
                0 -> FreeConfigsList(
                    configs   = configs,
                    loading   = loading,
                    error     = error,
                    onConnect = { config ->
                        // Check VPN permission before connecting — prevents instant crash/exit
                        val needsPermission = vm.connectFreeConfig(config)
                        if (needsPermission) {
                            val prepare = VpnService.prepare(context)
                            if (prepare != null) {
                                vpnPermLauncher.launch(prepare)
                            } else {
                                vm.onVpnPermissionGranted()
                            }
                        }
                    },
                    onRetry   = { vm.fetchFreeConfigs() }
                )
                1 -> BugHostsTab()
            }
        }
    }
}

// ── Free Configs Tab ─────────────────────────────────────────────────────────

@Composable
private fun FreeConfigsList(
    configs: List<FreeConfigFetcher.FreeConfig>,
    loading: Boolean,
    error: String?,
    onConnect: (FreeConfigFetcher.FreeConfig) -> Unit,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when {
            loading -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("جاري جلب السيرفرات المجانية...", color = OnSurfaceVariant, fontSize = 14.sp)
                    Text("من مستودعات barry-far, mahdibland...", color = OnSurfaceMuted, fontSize = 12.sp)
                }
            }
            error != null && configs.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CloudOff, null, Modifier.size(48.dp), tint = AccentRed)
                    Spacer(Modifier.height(12.dp))
                    Text("فشل الجلب", fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = OnSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors  = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("إعادة المحاولة")
                    }
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = AccentCyan)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${configs.size} سيرفر مجاني — اضغط Connect للاتصال مباشرة",
                                    fontSize = 12.sp, color = AccentCyan
                                )
                            }
                        }
                    }
                    items(configs, key = { it.link }) { config ->
                        FreeConfigCard(config = config, onConnect = { onConnect(config) })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FreeConfigCard(
    config: FreeConfigFetcher.FreeConfig,
    onConnect: () -> Unit
) {
    // Prevent double-tap: disable button immediately after first press
    var pressed by remember { mutableStateOf(false) }

    Card(
        shape  = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface1),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
        // No .clickable on the Card — the Button inside handles the tap.
        // Having both causes double-invocation of onConnect.
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val proto = when {
                config.link.startsWith("vmess://")  -> "VMess"
                config.link.startsWith("vless://")  -> "VLESS"
                config.link.startsWith("trojan://") -> "Trojan"
                else                                -> "V2Ray"
            }
            val protoColor = when (proto) {
                "VMess"  -> AccentCyan
                "VLESS"  -> AccentGreen
                "Trojan" -> AccentOrange
                else     -> AccentPurple
            }

            Column(Modifier.weight(1f)) {
                Text(
                    config.name.ifBlank { "Unknown Server" },
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = protoColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                        Text("  $proto  ", fontSize = 10.sp, color = protoColor,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                    Surface(color = AccentPurple.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
                        Text("  ${config.source}  ", fontSize = 10.sp, color = AccentPurple,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (!pressed) {
                        pressed = true
                        onConnect()
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.8f)),
                enabled = !pressed
            ) {
                if (pressed) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(14.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Bug Hosts Tab ─────────────────────────────────────────────────────────────

@Composable
private fun BugHostsTab() {
    val presets = FreeConfigFetcher.MOROCCAN_BUG_HOSTS

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = AccentOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("ما هو Bug Host؟", fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Bug Host هو نطاق مجاني (لا تُفوتر عليه الشرائح). يمكنك استخدامه مع V2Ray/Xray عبر WebSocket لتمرير ترافيكك مجاناً.\n\n" +
                        "الطريقة: أضف سيرفر V2Ray بـ WebSocket وضع Host Header = أحد هذه النطاقات.",
                        fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 18.sp
                    )
                }
            }
        }

        items(presets) { preset ->
            BugHostCard(preset)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, null, Modifier.size(16.dp), tint = AccentGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("طريقة الاستخدام مع السيرفرات المجانية", fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "1. ابحث عن رابط VLESS/VMess مجاني (تبويب السيرفرات المجانية)",
                        "2. أضفه من Servers → Add → الصق رابط vmess:// أو vless://",
                        "3. أو اضغط مباشرة على Connect في تبويب السيرفرات المجانية",
                        "4. إذا لم يشتغل السيرفر، جرّب سيرفراً آخر من القائمة",
                        "5. للاستخدام مع Bug Host: تحتاج سيرفر VPS خاص بك"
                    ).forEach { step ->
                        Text(step, fontSize = 12.sp, color = OnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BugHostCard(preset: FreeConfigFetcher.BugHostPreset) {
    Card(
        shape  = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface1),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    preset.carrier,
                    fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, Modifier.size(13.dp), tint = AccentCyan)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        preset.host,
                        fontSize = 13.sp, color = AccentCyan, fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Port: ${preset.port}  •  ${if (preset.tls) "TLS/HTTPS" else "HTTP"}",
                    fontSize = 11.sp, color = OnSurfaceVariant
                )
                Text(preset.description, fontSize = 11.sp, color = OnSurfaceMuted)
            }

            Surface(
                color = if (preset.tls) AccentGreen.copy(alpha = 0.15f) else AccentCyan.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    if (preset.tls) "  TLS  " else "  HTTP  ",
                    fontSize = 10.sp,
                    color = if (preset.tls) AccentGreen else AccentCyan,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
