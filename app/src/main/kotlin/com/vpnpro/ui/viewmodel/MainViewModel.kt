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
    val vpnState: StateFlow<VpnState> = VpnProService.state
    val vpnStats: StateFlow<VpnStats> = VpnProService.stats

    // ── UI state ──────────────────────────────────────────────
    private val _addServerLoading = MutableStateFlow(false)
    val addServerLoading: StateFlow<Boolean> = _addServerLoading.asStateFlow()

    private val _addServerError = MutableStateFlow<String?>(null)
    val addServerError: StateFlow<String?> = _addServerError.asStateFlow()

    private val _addServerSuccess = MutableStateFlow(false)
    val addServerSuccess: StateFlow<Boolean> = _addServerSuccess.asStateFlow()

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
    }

    fun disconnect() {
        val intent = Intent(ctx, VpnProService::class.java).apply {
            action = VpnProService.ACTION_DISCONNECT
        }
        ctx.startService(intent)
    }

    /** Returns true if VPN permission is already granted, false = show permission dialog */
    fun needsVpnPermission(): Boolean =
        VpnService.prepare(ctx) != null

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

    fun clearAddServerResult() {
        _addServerSuccess.value = false
        _addServerError.value   = null
    }

    private fun validateServer(s: Server) {
        require(s.name.isNotBlank())           { "Server name is required" }
        require(s.endpoint.contains(":"))      { "Endpoint must be host:port" }
        require(s.serverPublicKey.length >= 40){ "Server public key is invalid" }
        require(s.clientPrivateKey.length >= 40){ "Client private key is invalid" }
        require(s.clientAddress.contains("/")) { "Client address must include subnet (e.g. 10.0.0.2/32)" }
    }
}
