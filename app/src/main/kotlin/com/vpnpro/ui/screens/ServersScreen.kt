package com.vpnpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.data.model.Server
import com.vpnpro.ui.theme.*
import com.vpnpro.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onAddServer: () -> Unit
) {
    val servers        by vm.servers.collectAsState()
    val selectedServer by vm.selectedServer.collectAsState()
    val vpnState       by vm.vpnState.collectAsState()

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
                    "Servers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                FilledTonalButton(
                    onClick = onAddServer,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentCyan.copy(alpha = 0.15f),
                        contentColor = AccentCyan
                    )
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
                Spacer(Modifier.width(8.dp))
            }

            Divider(color = Surface3, thickness = 0.5.dp)

            if (servers.isEmpty()) {
                // Empty state
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Dns,
                            null,
                            Modifier.size(64.dp),
                            tint = OnSurfaceMuted
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No servers yet", fontSize = 18.sp, color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text("Tap 'Add' to add the first server", fontSize = 14.sp, color = OnSurfaceMuted)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onAddServer,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp), tint = Color.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Server", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(servers, key = { it.id }) { server ->
                        ServerCard(
                            server = server,
                            isSelected = server.id == selectedServer?.id,
                            onClick = {
                                vm.selectServer(server)
                                onBack()
                            }
                        )
                    }
                    item { Spacer(Modifier.height(60.dp)) }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddServer,
            containerColor = AccentCyan,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Icon(Icons.Default.Add, null)
        }
    }
}

@Composable
private fun ServerCard(
    server: Server,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentCyan else Color.Transparent
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Surface2 else Surface1
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag
            Box(
                Modifier.size(48.dp).background(Surface3, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(server.flag, fontSize = 24.sp)
            }
            Spacer(Modifier.width(14.dp))
            // Info
            Column(Modifier.weight(1f)) {
                Text(server.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnSurface)
                Text(
                    server.location.ifBlank { server.endpoint },
                    fontSize = 12.sp, color = OnSurfaceVariant
                )
                if (server.addedBy.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Added by ${server.addedBy}",
                        fontSize = 11.sp, color = OnSurfaceMuted
                    )
                }
            }
            // Selected indicator
            if (isSelected) {
                Box(
                    Modifier.size(24.dp).background(AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = Color.Black)
                }
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceMuted)
            }
        }
    }
}
