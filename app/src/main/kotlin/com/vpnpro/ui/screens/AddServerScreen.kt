package com.vpnpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.data.model.Server
import com.vpnpro.ui.theme.*
import com.vpnpro.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val loading by vm.addServerLoading.collectAsState()
    val error   by vm.addServerError.collectAsState()
    val success by vm.addServerSuccess.collectAsState()

    val clipboard = LocalClipboardManager.current

    // Tab: 0=WireGuard, 1=V2Ray/Xray
    var activeTab by remember { mutableIntStateOf(1) }  // default to Xray (more useful)

    // ── WireGuard fields ────────────────────────────────────────────────────
    var name             by remember { mutableStateOf("") }
    var flag             by remember { mutableStateOf("🌐") }
    var location         by remember { mutableStateOf("") }
    var endpoint         by remember { mutableStateOf("") }
    var serverPublicKey  by remember { mutableStateOf("") }
    var clientPrivateKey by remember { mutableStateOf("") }
    var clientAddress    by remember { mutableStateOf("10.0.0.2/32") }
    var dns              by remember { mutableStateOf("1.1.1.1, 1.0.0.1") }
    var allowedIPs       by remember { mutableStateOf("0.0.0.0/0, ::/0") }
    var preSharedKey     by remember { mutableStateOf("") }
    var mtuValue         by remember { mutableStateOf("1420") }
    var addedBy          by remember { mutableStateOf("") }
    var showPrivKey      by remember { mutableStateOf(false) }
    var showPSK          by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText       by remember { mutableStateOf("") }

    // ── Xray fields ─────────────────────────────────────────────────────────
    var xrayLink         by remember { mutableStateOf("") }

    LaunchedEffect(success) {
        if (success) {
            vm.clearAddServerResult()
            onBack()
        }
    }

    // WireGuard import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            icon  = { Icon(Icons.Default.ContentPaste, null, tint = AccentCyan) },
            title = { Text("Import WireGuard Config") },
            text  = {
                Column {
                    Text(
                        "Paste your WireGuard config file ([Interface] + [Peer] block).",
                        fontSize = 13.sp, color = OnSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = {
                            Text(
                                "[Interface]\nPrivateKey = ...\n\n[Peer]\nPublicKey = ...",
                                fontSize = 12.sp, color = OnSurfaceMuted
                            )
                        },
                        minLines = 6, maxLines = 10,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = {
                        val clip = clipboard.getText()?.toString() ?: ""
                        if (clip.isNotBlank()) importText = clip
                    }) {
                        Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp), tint = AccentCyan)
                        Spacer(Modifier.width(6.dp))
                        Text("Paste from Clipboard", color = AccentCyan, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportDialog = false
                        vm.importServerFromConfig(importText, "Imported Server")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    enabled = importText.isNotBlank()
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = OnSurfaceVariant) }
            },
            containerColor = Surface1
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, BgMid)))
    ) {
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
                    "إضافة سيرفر",
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
            Divider(color = Surface3, thickness = 0.5.dp)

            // ── Tabs ─────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = activeTab,
                containerColor   = BgDark,
                contentColor     = AccentCyan
            ) {
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("V2Ray / Xray", fontSize = 13.sp)
                    }
                }
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("WireGuard", fontSize = 13.sp)
                    }
                }
            }

            // ── Error banner ──────────────────────────────────────────────
            if (!error.isNullOrBlank()) {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, Modifier.size(16.dp), tint = AccentRed)
                        Spacer(Modifier.width(8.dp))
                        Text(error ?: "", fontSize = 13.sp, color = AccentRed, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.clearAddServerResult() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (activeTab) {
                    // ── V2Ray / Xray tab ───────────────────────────────────
                    1 -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = AccentGreen)
                                    Spacer(Modifier.width(8.dp))
                                    Text("كيف تحصل على رابط مجاني؟", fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "• تبويب 'سيرفرات مجانية' في الصفحة الرئيسية\n" +
                                    "• https://github.com/barry-far/V2ray-Configs\n" +
                                    "• https://freefq.com/v2ray/\n" +
                                    "• تيليجرام: @v2rayng_config",
                                    fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 18.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = xrayLink,
                            onValueChange = { xrayLink = it },
                            label = { Text("رابط V2Ray / Xray", fontSize = 12.sp) },
                            placeholder = {
                                Text(
                                    "vmess://... أو vless://... أو trojan://...",
                                    fontSize = 12.sp, color = OnSurfaceMuted
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Link, null, Modifier.size(20.dp), tint = OnSurfaceVariant) },
                            trailingIcon = {
                                if (xrayLink.isNotBlank()) {
                                    IconButton(onClick = { xrayLink = "" }) {
                                        Icon(Icons.Default.Clear, null, tint = OnSurfaceVariant)
                                    }
                                }
                            },
                            minLines = 3, maxLines = 5,
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clip = clipboard.getText()?.toString() ?: ""
                                    if (clip.isNotBlank()) xrayLink = clip.trim()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp), tint = AccentCyan)
                                Spacer(Modifier.width(6.dp))
                                Text("لصق", color = AccentCyan, fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (xrayLink.isNotBlank()) vm.importXrayLink(xrayLink.trim())
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled  = xrayLink.isNotBlank() && !loading,
                            colors   = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Save, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("حفظ السيرفر", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    // ── WireGuard tab ─────────────────────────────────────
                    0 -> {
                        SectionHeader("أساسيات", Icons.Default.Info)
                        ProField("اسم السيرفر", name, { name = it }, "Germany-01", Icons.Default.Label)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProField("Emoji العلم", flag, { flag = it.take(2) }, "🌍", Icons.Default.Flag, modifier = Modifier.width(90.dp))
                            ProField("الموقع", location, { location = it }, "Frankfurt", Icons.Default.LocationOn, modifier = Modifier.weight(1f))
                        }
                        ProField("المضيف (Endpoint)", endpoint, { endpoint = it }, "1.2.3.4:51820", Icons.Default.Router, keyboardType = KeyboardType.Uri)

                        SectionHeader("مفاتيح WireGuard", Icons.Default.VpnKey)
                        OutlinedTextField(
                            value = serverPublicKey,
                            onValueChange = { serverPublicKey = it },
                            label = { Text("Server Public Key", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Key, null, Modifier.size(20.dp), tint = OnSurfaceVariant) },
                            colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = clientPrivateKey,
                            onValueChange = { clientPrivateKey = it },
                            label = { Text("Client Private Key", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, Modifier.size(20.dp), tint = OnSurfaceVariant) },
                            visualTransformation = if (showPrivKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPrivKey = !showPrivKey }) {
                                    Icon(if (showPrivKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null, tint = OnSurfaceVariant)
                                }
                            },
                            colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                        )
                        ProField("Client Address", clientAddress, { clientAddress = it }, "10.0.0.2/32", Icons.Default.Smartphone, keyboardType = KeyboardType.Uri)

                        SectionHeader("إعدادات متقدمة", Icons.Default.Tune)
                        ProField("DNS", dns, { dns = it }, "1.1.1.1, 1.0.0.1", Icons.Outlined.Dns)
                        ProField("Allowed IPs", allowedIPs, { allowedIPs = it }, "0.0.0.0/0, ::/0", Icons.Default.Route)
                        ProField("MTU", mtuValue, { mtuValue = it }, "1420", Icons.Default.SettingsEthernet, keyboardType = KeyboardType.Number)
                        ProField("أضيف بواسطة", addedBy, { addedBy = it }, "اسمك (اختياري)", Icons.Default.Person)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showImportDialog = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp), tint = AccentCyan)
                                Spacer(Modifier.width(6.dp))
                                Text("Import Config", color = AccentCyan, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val server = Server(
                                    name            = name.trim(),
                                    flag            = flag.ifBlank { "🌐" },
                                    location        = location.trim(),
                                    endpoint        = endpoint.trim(),
                                    serverPublicKey = serverPublicKey.trim(),
                                    clientPrivateKey= clientPrivateKey.trim(),
                                    clientAddress   = clientAddress.trim(),
                                    dns             = dns.trim(),
                                    allowedIPs      = allowedIPs.trim(),
                                    preSharedKey    = preSharedKey.trim(),
                                    mtu             = mtuValue.toIntOrNull() ?: 1420,
                                    addedBy         = addedBy.trim(),
                                    protocol        = "WIREGUARD"
                                )
                                vm.addServer(server)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled  = name.isNotBlank() && endpoint.isNotBlank() && !loading,
                            colors   = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("حفظ ومشاركة السيرفر", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = AccentCyan)
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentCyan)
        Spacer(Modifier.width(8.dp))
        Divider(color = Surface3, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String, icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text, maxLines: Int = 1,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 13.sp, color = OnSurfaceMuted) },
        leadingIcon = { Icon(icon, null, Modifier.size(20.dp), tint = OnSurfaceVariant) },
        singleLine = maxLines == 1, maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = fieldColors(), modifier = modifier
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = AccentCyan,
    unfocusedBorderColor    = Surface3,
    focusedTextColor        = OnSurface,
    unfocusedTextColor      = OnSurface,
    focusedContainerColor   = Surface1,
    unfocusedContainerColor = Surface1,
    focusedLabelColor       = AccentCyan
)
