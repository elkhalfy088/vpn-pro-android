package com.vpnpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
    val loading  by vm.addServerLoading.collectAsState()
    val error    by vm.addServerError.collectAsState()
    val success  by vm.addServerSuccess.collectAsState()

    // Form fields
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
    var addedBy          by remember { mutableStateOf("") }
    var showPrivKey      by remember { mutableStateOf(false) }
    var showPSK          by remember { mutableStateOf(false) }

    // Navigate back on success
    LaunchedEffect(success) {
        if (success) { vm.clearAddServerResult(); onBack() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1526))))
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // ── Top bar ──────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = OnSurface)
                }
                Text(
                    "Add Server",
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
            Divider(color = Surface3, thickness = 0.5.dp)

            // ── Form ──────────────────────────────────────────────
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("Basic Info", Icons.Default.Info)

                ProField(label = "Server Name *", value = name, onValueChange = { name = it },
                    placeholder = "e.g. Germany-01", icon = Icons.Default.Label)
                ProField(label = "Flag Emoji", value = flag, onValueChange = { flag = it },
                    placeholder = "🌐", icon = Icons.Default.Flag)
                ProField(label = "Location", value = location, onValueChange = { location = it },
                    placeholder = "e.g. Frankfurt", icon = Icons.Default.LocationOn)
                ProField(label = "Your Name (optional)", value = addedBy, onValueChange = { addedBy = it },
                    placeholder = "Who added this server", icon = Icons.Default.Person)

                Spacer(Modifier.height(4.dp))
                SectionHeader("WireGuard Config", Icons.Default.Security)

                ProField(
                    label = "Endpoint *",
                    value = endpoint, onValueChange = { endpoint = it },
                    placeholder = "1.2.3.4:51820",
                    icon = Icons.Default.Dns,
                    keyboardType = KeyboardType.Uri
                )
                ProField(
                    label = "Server Public Key *",
                    value = serverPublicKey, onValueChange = { serverPublicKey = it },
                    placeholder = "Base64 public key of the server",
                    icon = Icons.Default.VpnKey
                )
                ProFieldPassword(
                    label = "Client Private Key *",
                    value = clientPrivateKey, onValueChange = { clientPrivateKey = it },
                    placeholder = "Base64 private key for this slot",
                    icon = Icons.Default.Key,
                    visible = showPrivKey,
                    onToggle = { showPrivKey = !showPrivKey }
                )
                ProField(
                    label = "Client Address *",
                    value = clientAddress, onValueChange = { clientAddress = it },
                    placeholder = "10.0.0.2/32",
                    icon = Icons.Default.Router,
                    keyboardType = KeyboardType.Uri
                )
                ProField(
                    label = "DNS",
                    value = dns, onValueChange = { dns = it },
                    placeholder = "1.1.1.1, 1.0.0.1",
                    icon = Icons.Default.Public
                )
                ProField(
                    label = "Allowed IPs",
                    value = allowedIPs, onValueChange = { allowedIPs = it },
                    placeholder = "0.0.0.0/0, ::/0",
                    icon = Icons.Default.FilterList
                )
                ProFieldPassword(
                    label = "Pre-Shared Key (optional)",
                    value = preSharedKey, onValueChange = { preSharedKey = it },
                    placeholder = "Leave empty if not used",
                    icon = Icons.Default.Lock,
                    visible = showPSK,
                    onToggle = { showPSK = !showPSK }
                )

                // Help box
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(0.08f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = AccentCyan)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Generate the client private key on your WireGuard server:\n" +
                            "wg genkey > client.key && wg pubkey < client.key > client.pub\n\n" +
                            "Add the client public key to your server config, then paste " +
                            "the client private key here.",
                            fontSize = 12.sp,
                            color = AccentCyan.copy(0.85f),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Error
                error?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(0.12f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, Modifier.size(18.dp), tint = AccentRed)
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = AccentRed, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        vm.addServer(
                            Server(
                                name            = name.trim(),
                                flag            = flag.trim().ifBlank { "🌐" },
                                location        = location.trim(),
                                endpoint        = endpoint.trim(),
                                serverPublicKey = serverPublicKey.trim(),
                                clientPrivateKey= clientPrivateKey.trim(),
                                clientAddress   = clientAddress.trim(),
                                dns             = dns.trim(),
                                allowedIPs      = allowedIPs.trim(),
                                preSharedKey    = preSharedKey.trim(),
                                addedBy         = addedBy.trim()
                            )
                        )
                    },
                    enabled = !loading && name.isNotBlank() && endpoint.isNotBlank()
                            && serverPublicKey.isNotBlank() && clientPrivateKey.isNotBlank()
                            && clientAddress.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        disabledContainerColor = AccentCyan.copy(alpha = 0.3f)
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp), tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Server", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = AccentCyan)
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AccentCyan)
    }
}

@Composable
private fun ProField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = OnSurfaceMuted, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp), tint = OnSurfaceVariant) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan,
            focusedLabelColor  = AccentCyan,
            cursorColor        = AccentCyan,
            unfocusedBorderColor = Surface3,
            unfocusedLabelColor  = OnSurfaceVariant,
            focusedTextColor     = OnSurface,
            unfocusedTextColor   = OnSurface
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun ProFieldPassword(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String = "", icon: ImageVector, visible: Boolean, onToggle: () -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = OnSurfaceMuted, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp), tint = OnSurfaceVariant) },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, tint = OnSurfaceVariant
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan, focusedLabelColor = AccentCyan,
            cursorColor = AccentCyan, unfocusedBorderColor = Surface3,
            unfocusedLabelColor = OnSurfaceVariant,
            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface
        ),
        shape = MaterialTheme.shapes.medium
    )
}
