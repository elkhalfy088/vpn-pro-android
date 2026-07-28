package com.vpnpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpnpro.data.model.Server
import com.vpnpro.data.model.ServerProtocol
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
    val connectedName  by vm.connectedServerName.collectAsState()

    var searchQuery    by remember { mutableStateOf("") }
    var showFavOnly    by remember { mutableStateOf(false) }
    var serverToDelete by remember { mutableStateOf<Server?>(null) }
    var serverToEdit   by remember { mutableStateOf<Server?>(null) }

    // Filter
    val filtered = servers.filter {
        val matchesSearch = searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.location.contains(searchQuery, ignoreCase = true)
        val matchesFav = !showFavOnly || it.isFavorite
        matchesSearch && matchesFav
    }

    // Delete confirmation
    serverToDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = AccentRed) },
            title = { Text("حذف السيرفر") },
            text  = { Text("هل تريد حذف \"${toDelete.name}\"؟ لا يمكن التراجع.", color = OnSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { vm.deleteServer(toDelete.id); serverToDelete = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text("إلغاء", color = OnSurfaceVariant) }
            },
            containerColor = Surface1
        )
    }

    // Edit dialog (basic name/location edit)
    serverToEdit?.let { editing ->
        var editName     by remember { mutableStateOf(editing.name) }
        var editLocation by remember { mutableStateOf(editing.location) }
        AlertDialog(
            onDismissRequest = { serverToEdit = null },
            icon  = { Icon(Icons.Default.Edit, null, tint = AccentCyan) },
            title = { Text("تعديل السيرفر") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it },
                        label = { Text("الاسم") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan, unfocusedBorderColor = Surface3,
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                            focusedContainerColor = Surface1, unfocusedContainerColor = Surface1,
                            focusedLabelColor = AccentCyan
                        )
                    )
                    OutlinedTextField(
                        value = editLocation, onValueChange = { editLocation = it },
                        label = { Text("الموقع") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan, unfocusedBorderColor = Surface3,
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                            focusedContainerColor = Surface1, unfocusedContainerColor = Surface1,
                            focusedLabelColor = AccentCyan
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.updateServer(editing.copy(name = editName.trim(), location = editLocation.trim()))
                        serverToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    enabled = editName.isNotBlank()
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { serverToEdit = null }) { Text("إلغاء", color = OnSurfaceVariant) }
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
            // ── Top bar ───────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = OnSurface)
                }
                Text(
                    "السيرفرات",
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                // ⭐ Toggle favourites filter
                IconButton(onClick = { showFavOnly = !showFavOnly }) {
                    Icon(
                        imageVector = if (showFavOnly) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "المفضلة",
                        tint = if (showFavOnly) AccentOrange else OnSurfaceVariant
                    )
                }
                IconButton(onClick = onAddServer) {
                    Icon(Icons.Default.Add, null, tint = AccentCyan)
                }
            }
            HorizontalDivider(color = Surface3, thickness = 0.5.dp)

            // ── Search ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث بالاسم أو الموقع…", color = OnSurfaceMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp), tint = OnSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = OnSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Surface3,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    focusedContainerColor = Surface1,
                    unfocusedContainerColor = Surface1
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── List ─────────────────────────────────────────────────────
            when {
                filtered.isEmpty() && servers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Dns, null, Modifier.size(48.dp), tint = OnSurfaceMuted)
                            Spacer(Modifier.height(12.dp))
                            Text("لا يوجد سيرفرات بعد", color = OnSurfaceVariant, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("اضغط + لإضافة سيرفر WireGuard أو V2Ray", color = OnSurfaceMuted, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onAddServer,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("إضافة سيرفر")
                            }
                        }
                    }
                }
                filtered.isEmpty() && showFavOnly -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.StarBorder, null, Modifier.size(48.dp), tint = OnSurfaceMuted)
                            Spacer(Modifier.height(12.dp))
                            Text("لا يوجد سيرفرات مفضلة", color = OnSurfaceVariant, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("اضغط ⭐ على أي سيرفر لإضافته للمفضلة", color = OnSurfaceMuted, fontSize = 13.sp)
                        }
                    }
                }
                filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا نتائج لـ \"$searchQuery\"", color = OnSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        // Show favourites first
                        val favServers   = filtered.filter { it.isFavorite }
                        val otherServers = filtered.filter { !it.isFavorite }

                        if (favServers.isNotEmpty()) {
                            item {
                                Text(
                                    "⭐ المفضلة",
                                    fontSize = 11.sp, color = AccentOrange,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                )
                            }
                            items(favServers, key = { it.id + "_fav" }) { server ->
                                ServerCard(
                                    server      = server,
                                    isSelected  = selectedServer?.id == server.id,
                                    isConnected = selectedServer?.id == server.id && vpnState == VpnState.CONNECTED,
                                    onSelect    = { vm.selectServer(server) },
                                    onEdit      = { serverToEdit = server },
                                    onDelete    = { serverToDelete = server },
                                    onToggleStar = { vm.toggleFavorite(server) }
                                )
                            }
                            if (otherServers.isNotEmpty()) {
                                item {
                                    Text(
                                        "كل السيرفرات",
                                        fontSize = 11.sp, color = OnSurfaceMuted,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        items(otherServers, key = { it.id }) { server ->
                            ServerCard(
                                server      = server,
                                isSelected  = selectedServer?.id == server.id,
                                isConnected = selectedServer?.id == server.id && vpnState == VpnState.CONNECTED,
                                onSelect    = { vm.selectServer(server) },
                                onEdit      = { serverToEdit = server },
                                onDelete    = { serverToDelete = server },
                                onToggleStar = { vm.toggleFavorite(server) }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ── Server card ───────────────────────────────────────────────────────────────

@Composable
private fun ServerCard(
    server: Server,
    isSelected: Boolean,
    isConnected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val isXray     = server.serverProtocol == ServerProtocol.XRAY
    val protoLabel = if (isXray) "V2Ray/Xray" else "WireGuard"
    val protoColor = if (isXray) AccentGreen else AccentCyan

    Card(
        shape  = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Surface2 else Surface1
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag
            Text(server.flag.ifBlank { "🌐" }, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))

            // Info
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        server.name,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (server.isPremium) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = AccentOrange.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("  PRO  ", fontSize = 10.sp, color = AccentOrange,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
                Text(
                    server.location.ifBlank { server.endpoint.ifBlank { "V2Ray Server" } },
                    fontSize = 12.sp, color = OnSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(color = protoColor.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
                        Text("  $protoLabel  ", fontSize = 10.sp, color = protoColor,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                    if (server.usageCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, null, Modifier.size(11.dp), tint = OnSurfaceMuted)
                            Spacer(Modifier.width(2.dp))
                            Text("${server.usageCount}", fontSize = 10.sp, color = OnSurfaceMuted)
                        }
                    }
                }
            }

            // ⭐ Star / Favourite button
            IconButton(
                onClick = onToggleStar,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (server.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (server.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                    tint = if (server.isFavorite) AccentOrange else OnSurfaceMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Selected / Connected indicator
            if (isSelected) {
                Icon(
                    if (isConnected) Icons.Default.CheckCircle else Icons.Default.RadioButtonChecked,
                    null,
                    tint = if (isConnected) AccentGreen else AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            // More menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null, Modifier.size(18.dp), tint = OnSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Surface2)
                ) {
                    DropdownMenuItem(
                        text = { Text("تعديل", color = OnSurface, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = AccentCyan) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف", color = AccentRed, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = AccentRed) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}
