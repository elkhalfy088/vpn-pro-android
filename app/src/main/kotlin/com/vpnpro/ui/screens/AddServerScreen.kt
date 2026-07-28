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

    // Import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            icon = { Icon(Icons.Default.ContentPaste, null, tint = AccentCyan) },
            title = { Text("Import WireGuard Config") },
            text = {
                Column {
                    Text(
                        "Paste your WireGuard config file here ([Interface] + [Peer] block).",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant
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
                        minLines = 6,
                        maxLines = 10,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = {
                        val clip = clipboard.getText()?.toString() ?: ""
                        if (clip.isNotBlank()) importText = clip
                    }) {
                        Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp), tint = AccentCyan)
                        Spacer(Modifier.width(4.dp))
                        Text("Paste from clipboard", color = AccentCyan, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.importServerFromConfig(importText, name.ifBlank { "Imported Server" })
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    enabled = importText.isNotBlank()
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            },
            containerColor = Surface2
        )
    }

    LaunchedEffect(success) {
        if (success) { vm.clearAddServerResult(); onBack() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, BgMid)))
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            // ── Top bar ────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = OnSurface)
                }
                Text(
                    "Add Server",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                FilledTonalButton(
                    onClick = { showImportDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentCyan.copy(alpha = 0.12f),
                        contentColor = AccentCyan
                    )
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import", fontSize = 13.sp)
                }
                Spacer(Modifier.width(8.dp))
            }
            HorizontalDivider(color = Surface3, thickness = 0.5.dp)

            // ── Error banner ───────────────────────────────────
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AccentRed.copy(alpha = 0.15f)
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(error ?: "", color = AccentRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = vm::clearAddServerResult,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Form ───────────────────────────────────────────
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("Basic Info", Icons.Default.Info)

                ProField("Server Name *",  name,     { name = it },     "e.g. Germany-01",          Icons.Default.Label)
                ProField("Flag Emoji",      flag,     { flag = it },     "🌐",                        Icons.Default.Flag)
                ProField("Location",        location, { location = it }, "e.g. Frankfurt",            Icons.Default.LocationOn)
                ProField("Added by",        addedBy,  { addedBy = it },  "Your name or nickname",     Icons.Default.Person)

                SectionHeader("Connection", Icons.Default.Wifi)

                ProField(
                    "Endpoint (host:port) *", endpoint, { endpoint = it },
                    "e.g. 1.2.3.4:51820 or vpn.example.com:51820",
                    Icons.Outlined.Dns,
                    keyboardType = KeyboardType.Uri
                )

                SectionHeader("WireGuard Keys", Icons.Default.Lock)

                ProField(
                    "Server Public Key *", serverPublicKey, { serverPublicKey = it },
                    "Base64 public key (44 chars)",
                    Icons.Default.VpnKey,
                    maxLines = 2
                )

                // Client private key with show/hide
                OutlinedTextField(
                    value = clientPrivateKey,
                    onValueChange = { clientPrivateKey = it },
                    label = { Text("Client Private Key *", fontSize = 12.sp) },
                    placeholder = {
                        Text("Base64 private key (44 chars)", fontSize = 13.sp, color = OnSurfaceMuted)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Key, null, Modifier.size(20.dp), tint = OnSurfaceVariant)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPrivKey = !showPrivKey }) {
                            Icon(
                                if (showPrivKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, Modifier.size(18.dp), tint = OnSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (showPrivKey) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    maxLines = 2,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Pre-shared key
                OutlinedTextField(
                    value = preSharedKey,
                    onValueChange = { preSharedKey = it },
                    label = { Text("Pre-Shared Key (optional)", fontSize = 12.sp) },
                    placeholder = {
                        Text("Leave blank if not used", fontSize = 13.sp, color = OnSurfaceMuted)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Shield, null, Modifier.size(20.dp), tint = OnSurfaceVariant)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPSK = !showPSK }) {
                            Icon(
                                if (showPSK) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, Modifier.size(18.dp), tint = OnSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (showPSK) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    maxLines = 2,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                SectionHeader("Network Settings", Icons.Default.NetworkCheck)

                ProField("Client Address",  clientAddress, { clientAddress = it },
                    "e.g. 10.0.0.2/32",    Icons.Default.Computer)
                ProField("DNS",             dns,            { dns = it },
                    "e.g. 1.1.1.1, 8.8.8.8", Icons.Default.Language)
                ProField("Allowed IPs",     allowedIPs,    { allowedIPs = it },
                    "0.0.0.0/0, ::/0 = all traffic through VPN", Icons.Default.Share)
                ProField("MTU",             mtuValue,       { mtuValue = it },
                    "1420 (recommended)",   Icons.Default.Tune,
                    keyboardType = KeyboardType.Number)

                Spacer(Modifier.height(8.dp))

                // Tip card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AccentCyan.copy(alpha = 0.07f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info, null,
                            Modifier.size(16.dp), tint = AccentCyan
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Set Allowed IPs to 0.0.0.0/0, ::/0 to route ALL traffic (images, videos, any website) through the VPN.",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Save button ────────────────────────────────────
            Surface(color = BgDark, shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        vm.addServer(
                            Server(
                                name             = name.trim(),
                                flag             = flag.trim().ifBlank { "🌐" },
                                location         = location.trim(),
                                endpoint         = endpoint.trim(),
                                serverPublicKey  = serverPublicKey.trim(),
                                clientPrivateKey = clientPrivateKey.trim(),
                                clientAddress    = clientAddress.trim(),
                                dns              = dns.trim(),
                                allowedIPs       = allowedIPs.trim(),
                                preSharedKey     = preSharedKey.trim(),
                                mtu              = mtuValue.toIntOrNull() ?: 1420,
                                addedBy          = addedBy.trim(),
                                addedAt          = System.currentTimeMillis()
                            )
                        )
                    },
                    enabled = !loading &&
                              name.isNotBlank() &&
                              endpoint.isNotBlank() &&
                              serverPublicKey.isNotBlank() &&
                              clientPrivateKey.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(Icons.Default.CloudUpload, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save & Share Server", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
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
        HorizontalDivider(color = Surface3, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 13.sp, color = OnSurfaceMuted) },
        leadingIcon = { Icon(icon, null, Modifier.size(20.dp), tint = OnSurfaceVariant) },
        singleLine = maxLines == 1,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
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
