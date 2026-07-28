package akh.vpn.dd.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import akh.vpn.dd.viewmodel.VpnViewModel

@Composable
fun SetupScreen(viewModel: VpnViewModel) {
    var host      by remember { mutableStateOf("") }
    var port      by remember { mutableStateOf("8443") }
    var decoy     by remember { mutableStateOf("www.facebook.com") }
    var hostError by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo / Icon
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "إعداد Vpn Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "أدخل بيانات السيرفر مرة واحدة — ستُحفظ للجميع",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // Server Host
            OutlinedTextField(
                value = host,
                onValueChange = { host = it; hostError = false },
                label = { Text("IP السيرفر") },
                placeholder = { Text("مثال: 45.79.5.148") },
                leadingIcon = { Icon(Icons.Default.Dns, null) },
                isError = hostError,
                supportingText = if (hostError) ({ Text("أدخل IP صحيح") }) else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // Port
            OutlinedTextField(
                value = port,
                onValueChange = { port = it; portError = false },
                label = { Text("المنفذ (Port)") },
                placeholder = { Text("8443") },
                leadingIcon = { Icon(Icons.Default.Hub, null) },
                isError = portError,
                supportingText = if (portError) ({ Text("رقم بين 1 و 65535") }) else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // Decoy Domain
            OutlinedTextField(
                value = decoy,
                onValueChange = { decoy = it },
                label = { Text("دومين التمويه") },
                placeholder = { Text("www.facebook.com") },
                leadingIcon = { Icon(Icons.Default.Shield, null) },
                supportingText = { Text("هذا ما يراه المزود — اتركه كما هو") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(32.dp))

            // Info card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "المزود سيرى اتصالاً بـ ${decoy.ifBlank { "facebook.com" }} فقط",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Error message
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        uiState.errorMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Save button
            Button(
                onClick = {
                    hostError = host.isBlank()
                    portError = port.toIntOrNull()?.let { it !in 1..65535 } ?: true
                    if (!hostError && !portError) {
                        viewModel.saveSetup(host.trim(), port.toInt(), decoy.trim().ifBlank { "www.facebook.com" })
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("حفظ والمتابعة", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
