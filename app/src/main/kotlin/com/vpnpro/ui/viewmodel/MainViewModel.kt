package com.vpnpro.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vpnpro.data.firebase.FirebaseRepository
import com.vpnpro.data.model.Server
import com.vpnpro.vpn.VpnProService
import com.vpnpro.vpn.VpnState
import com.vpnpro.vpn.VpnStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repo: FirebaseRepository
) : AndroidViewModel(application) {

    private val ctx get() = getApplication<Application>()

    // ── Servers ───────────────────────────────────────────────
    val servers: StateFlow<List<Server>> = repo.serversFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

    // ── VPN state (from service) ───────────────────────────────
    val vpnState: StateFlow<VpnState>  = VpnProService.state
    val vpnStats: StateFlow<VpnStats>  = VpnProService.stats
    val lastError: StateFlow<String?>  = VpnProService.lastError
    val connectedServerName: StateFlow<String?> = VpnProService.connectedServerName

    // ── UI state ──────────────────────────────────────────────
    private val _addServerLoading  = MutableStateFlow(false)
    val addServerLoading: StateFlow<Boolean> = _addServerLoading.asStateFlow()

    private val _addServerError    = MutableStateFlow<String?>(null)
    val addServerError: StateFlow<String?> = _addServerError.asStateFlow()

    private val _addServerSuccess  = MutableStateFlow(false)
    val addServerSuccess: StateFlow<Boolean> = _addServerSuccess.asStateFlow()

    private val _deleteLoading     = MutableStateFlow(false)
    val deleteLoading: StateFlow<Boolean> = _deleteLoading.asStateFlow()

    private val _snackMessage      = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    // ── Actions ────────────────────────────────────────────────
    fun selectServer(server: Server) {
        _selectedServer.value = server
    }

    fun connect() {
        val server = _selectedServer.value ?: return
        val configText = server.toWireGuardConfig()
        val intent = Intent(ctx, VpnProService::class.java).apply {
            action = VpnProService.ACTION_CONNECT
            putExtra(VpnProService.EXTRA_CONFIG, configText)
            putExtra(VpnProService.EXTRA_SERVER_NAME, server.name)
        }
        ctx.startService(intent)
        // Increment usage count in background
        viewModelScope.launch {
            runCatching { repo.incrementUsage(server.id) }
        }
    }

    fun disconnect() {
        val intent = Intent(ctx, VpnProService::class.java).apply {
            action = VpnProService.ACTION_DISCONNECT
        }
        ctx.startService(intent)
    }

    fun toggleConnection() {
        when (vpnState.value) {
            VpnState.CONNECTED, VpnState.CONNECTING -> disconnect()
            else -> connect()
        }
    }

    /** Returns true if VPN permission dialog is needed */
    fun needsVpnPermission(): Boolean = VpnService.prepare(ctx) != null

    fun addServer(server: Server) {
        viewModelScope.launch {
            _addServerLoading.value = true
            _addServerError.value   = null
            try {
                validateServer(server)
                repo.addServer(server.copy(addedAt = System.currentTimeMillis()))
                _addServerSuccess.value = true
            } catch (e: Exception) {
                _addServerError.value = e.message ?: "Unknown error"
            } finally {
                _addServerLoading.value = false
            }
        }
    }

    fun updateServer(server: Server) {
        viewModelScope.launch {
            try {
                validateServer(server)
                repo.updateServer(server)
                _snackMessage.value = "Server updated"
                // If the currently selected server is this one, update it
                if (_selectedServer.value?.id == server.id) {
                    _selectedServer.value = server
                }
            } catch (e: Exception) {
                _snackMessage.value = "Update failed: ${e.message}"
            }
        }
    }

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            _deleteLoading.value = true
            try {
                repo.deleteServer(server.id)
                if (_selectedServer.value?.id == server.id) {
                    _selectedServer.value = null
                }
                _snackMessage.value = "${server.name} deleted"
            } catch (e: Exception) {
                _snackMessage.value = "Delete failed: ${e.message}"
            } finally {
                _deleteLoading.value = false
            }
        }
    }

    fun importServerFromConfig(configText: String, name: String) {
        val server = Server.fromWireGuardConfig(configText, name)
        if (server == null) {
            _addServerError.value = "Invalid WireGuard config — make sure it has [Interface] and [Peer] sections"
            return
        }
        addServer(server)
    }

    fun clearAddServerResult() {
        _addServerSuccess.value = false
        _addServerError.value   = null
    }

    fun clearSnack() {
        _snackMessage.value = null
    }

    private fun validateServer(s: Server) {
        require(s.name.isNotBlank())              { "Server name is required" }
        require(s.endpoint.contains(":"))         { "Endpoint must be in host:port format (e.g. 1.2.3.4:51820)" }
        require(s.serverPublicKey.length >= 40)   { "Server public key is too short / invalid" }
        require(s.clientPrivateKey.length >= 40)  { "Client private key is too short / invalid" }
        require(s.clientAddress.isNotBlank())     { "Client address is required (e.g. 10.0.0.2/32)" }
    }
}
