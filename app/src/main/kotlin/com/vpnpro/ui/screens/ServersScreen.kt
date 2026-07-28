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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.data.model.Server
import com.vpnpro.ui.theme.*
import com.vpnpro.ui.viewmodel.MainViewModel
import com.vpnpro.vpn.VpnState

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
    val snackMsg       by vm.snackMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); vm.clearSnack() }
    }

    var serverToDelete by remember { mutableStateOf<Server?>(null) }
    var serverToEdit   by remember { mutableStateOf<Server?>(null) }
    var searchQuery    by remember { mutableStateOf("") }

    val filteredServers = remember(servers, searchQuery) {
        if (searchQuery.isBlank()) servers
        else servers.filter { it.name.contains(searchQuery, true) || it.location.contains(searchQuery, true) }
    }

    // Delete confirm dialog
    serverToDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = AccentRed) },
            title = { Text("Delete Server?") },
            text  = { Text("Remove \"${s.name}\" for all users? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteServer(s); serverToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text("Cancel") }
            },
            containerColor = Surface2
        )
    }

    // Edit server bottom sheet
    serverToEdit?.let { s ->
        EditServerDialog(server = s, onDismiss = { serverToEdit = null }, onSave = { updated ->
            vm.updateServer(updated); serverToEdit = null
        })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgDark, BgMid)))
                .padding(innerPadding)
        ) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                // ── Top bar ───────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = OnSurface)
                    }
                    Text(
                        "Servers",
                        fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface,
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

                // ── Search bar ────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search servers…", color = OnSurfaceMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceMuted, modifier = Modifier.size(20.dp)) },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        { IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                        }}
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Surface1, unfocusedContainerColor = Surface1,
                        focusedBorderColor = AccentCyan, unfocusedBorderColor = Surface3,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium
                )

                HorizontalDivider(color = Surface3, thickness = 0.5.dp)

                // ── Server count ──────────────────────────────
                if (filteredServers.isNotEmpty()) {
                    Text(
                        "${filteredServers.size} server${if (filteredServers.size == 1) "" else "s"}",
                        color = OnSurfaceMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // ── Empty state ───────────────────────────────
                if (filteredServers.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Dns, null, Modifier.size(64.dp), tint = OnSurfaceMuted)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isBlank()) "No servers yet" else "No results for "$searchQuery"",
                                fontSize = 18.sp, color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (searchQuery.isBlank()) "Tap 'Add' to add your first server" else "Try a different search term",
                                fontSize = 14.sp, color = OnSurfaceMuted
                            )
                            if (searchQuery.isBlank()) {
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = onAddServer,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Server")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredServers, key = { it.id }) { server ->
                            ServerCard(
                                server = server,
                                isSelected = selectedServer?.id == server.id,
                                isConnected = vpnState == VpnState.CONNECTED && selectedServer?.id == server.id,
                                onSelect = { vm.selectServer(server) },
                                onEdit   = { serverToEdit = server },
                                onDelete = { serverToDelete = server }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: Server,
    isSelected: Boolean,
    isConnected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Surface3 else Surface2
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(1.5.dp, AccentCyan.copy(alpha = 0.6f), MaterialTheme.shapes.large)
                else Modifier
            )
            .clickable(onClick = onSelect)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag + connection dot
            Box(contentAlignment = Alignment.BottomEnd) {
                Text(server.flag, fontSize = 36.sp)
                if (isConnected) {
                    Box(
                        Modifier
                            .size(11.dp)
                            .background(AccentGreen, CircleShape)
                            .border(2.dp, Surface2, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))

            // Info
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        server.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (server.isPremium) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = AccentOrange.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("  PRO  ", fontSize = 10.sp, color = AccentOrange, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
                Text(
                    server.location.ifBlank { server.endpoint },
                    fontSize = 12.sp, color = OnSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Protocol badge
                    Surface(color = AccentCyan.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
                        Text("  WireGuard  ", fontSize = 10.sp, color = AccentCyan,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                    // Usage count
                    if (server.usageCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, null, Modifier.size(11.dp), tint = OnSurfaceMuted)
                            Spacer(Modifier.width(2.dp))
                            Text("${server.usageCount}", fontSize = 10.sp, color = OnSurfaceMuted)
                        }
                    }
                }
            }

            // Connected check or menu
            if (isConnected) {
                Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
            }

            // 3-dot menu
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = Surface2
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = OnSurface) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = AccentCyan) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = AccentRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentRed) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditServerDialog(
    server: Server,
    onDismiss: () -> Unit,
    onSave: (Server) -> Unit
) {
    var name       by remember { mutableStateOf(server.name) }
    var flag       by remember { mutableStateOf(server.flag) }
    var location   by remember { mutableStateOf(server.location) }
    var endpoint   by remember { mutableStateOf(server.endpoint) }
    var addedBy    by remember { mutableStateOf(server.addedBy) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Server", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Name *", name,     { v: String -> name = v }),
                    Triple("Flag Emoji", flag,  { v: String -> flag = v }),
                    Triple("Location",location, { v: String -> location = v }),
                    Triple("Endpoint", endpoint,{ v: String -> endpoint = v }),
                    Triple("Added by", addedBy, { v: String -> addedBy = v }),
                ).forEach { (label, value, onChange) ->
                    OutlinedTextField(
                        value = value, onValueChange = onChange,
                        label = { Text(label, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Surface3,
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                            focusedContainerColor = Surface1, unfocusedContainerColor = Surface1,
                            focusedLabelColor = AccentCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(server.copy(name = name, flag = flag, location = location, endpoint = endpoint, addedBy = addedBy))
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Surface2
    )
}
