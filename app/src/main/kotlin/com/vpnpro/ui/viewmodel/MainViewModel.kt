package com.vpnpro.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vpnpro.data.firebase.FirebaseRepository
import com.vpnpro.data.model.Server
import com.vpnpro.data.model.ServerProtocol
import com.vpnpro.vpn.ConfigParser
import com.vpnpro.vpn.FreeConfigFetcher
import com.vpnpro.vpn.VpnProService
import com.vpnpro.vpn.VpnState
import com.vpnpro.vpn.VpnStats
import com.vpnpro.vpn.XrayVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repo: FirebaseRepository
) : AndroidViewModel(application) {

    private val ctx get() = getApplication<Application>()

    // ── Servers ───────────────────────────────────────────────────────────
    val servers: StateFlow<List<Server>> = repo.serversFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

    // ── VPN state (merged from both services) ─────────────────────────────
    val vpnState: StateFlow<VpnState> = combine(
        VpnProService.state,
        XrayVpnService.state
    ) { wgState, xrayState ->
        when {
            wgState   == VpnState.CONNECTED   -> VpnState.CONNECTED
            xrayState == VpnState.CONNECTED   -> VpnState.CONNECTED
            wgState   == VpnState.CONNECTING  -> VpnState.CONNECTING
            xrayState == VpnState.CONNECTING  -> VpnState.CONNECTING
            wgState   == VpnState.ERROR       -> VpnState.ERROR
            xrayState == VpnState.ERROR       -> VpnState.ERROR
            else                              -> VpnState.DISCONNECTED
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VpnState.DISCONNECTED)

    val vpnStats: StateFlow<VpnStats> = combine(
        VpnProService.stats,
        XrayVpnService.stats
    ) { wgStats, xrayStats ->
        if (wgStats.connectedSince > 0) wgStats else xrayStats
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VpnStats())

    val lastError: StateFlow<String?> = combine(
        VpnProService.lastError,
        XrayVpnService.lastError
    ) { wgErr, xrayErr -> wgErr ?: xrayErr }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val connectedServerName: StateFlow<String?> = combine(
        VpnProService.connectedServerName,
        XrayVpnService.connectedServerName
    ) { wgName, xrayName -> wgName ?: xrayName }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Add-server UI state ────────────────────────────────────────────────
    private val _addServerLoading = MutableStateFlow(false)
    val addServerLoading: StateFlow<Boolean> = _addServerLoading.asStateFlow()

    private val _addServerError = MutableStateFlow<String?>(null)
    val addServerError: StateFlow<String?> = _addServerError.asStateFlow()

    private val _addServerSuccess = MutableStateFlow(false)
    val addServerSuccess: StateFlow<Boolean> = _addServerSuccess.asStateFlow()

    // ── Free configs state ────────────────────────────────────────────────
    private val _freeConfigs = MutableStateFlow<List<FreeConfigFetcher.FreeConfig>>(emptyList())
    val freeConfigs: StateFlow<List<FreeConfigFetcher.FreeConfig>> = _freeConfigs.asStateFlow()

    private val _freeConfigsLoading = MutableStateFlow(false)
    val freeConfigsLoading: StateFlow<Boolean> = _freeConfigsLoading.asStateFlow()

    private val _freeConfigsError = MutableStateFlow<String?>(null)
    val freeConfigsError: StateFlow<String?> = _freeConfigsError.asStateFlow()

    // ── VPN permission pending state ──────────────────────────────────────
    private val _pendingFreeConfig = MutableStateFlow<FreeConfigFetcher.FreeConfig?>(null)
    val pendingFreeConfig: StateFlow<FreeConfigFetcher.FreeConfig?> = _pendingFreeConfig.asStateFlow()

    // ── Actions ────────────────────────────────────────────────────────────

    fun selectServer(server: Server) {
        _selectedServer.value = server
    }

    fun connect() {
        val server = _selectedServer.value ?: return
        try {
            // ── 1. Stop only the service that is currently active ──────────
            // Never start a service just to stop it — that causes crashes on
            // Android 12+ (ForegroundServiceStartNotAllowedException).
            disconnectAll()

            // ── 2. Start the appropriate service ──────────────────────────
            when (server.serverProtocol) {
                ServerProtocol.WIREGUARD -> connectWireGuard(server)
                ServerProtocol.XRAY      -> connectXray(server)
            }
        } catch (e: Exception) {
            Log.e(TAG, "connect() threw: ${e.message}", e)
            // Surface the error to the UI so the user sees it instead of a crash
            VpnProService.setError("فشل الاتصال: ${e.message}")
            XrayVpnService.setError("فشل الاتصال: ${e.message}")
        }
    }

    private fun connectWireGuard(server: Server) {
        runCatching {
            val intent = Intent(ctx, VpnProService::class.java).apply {
                action = VpnProService.ACTION_CONNECT
                putExtra(VpnProService.EXTRA_CONFIG, server.toWireGuardConfig())
                putExtra(VpnProService.EXTRA_SERVER_NAME, server.name)
            }
            ctx.startService(intent)
            viewModelScope.launch { runCatching { repo.incrementUsage(server.id) } }
        }.onFailure { e ->
            Log.e(TAG, "connectWireGuard failed: ${e.message}", e)
            VpnProService.setError("فشل بدء WireGuard: ${e.message}")
        }
    }

    private fun connectXray(server: Server) {
        runCatching {
            val intent = Intent(ctx, XrayVpnService::class.java).apply {
                action = XrayVpnService.ACTION_CONNECT
                putExtra(XrayVpnService.EXTRA_CONFIG_JSON, server.xrayConfigJson)
                putExtra(XrayVpnService.EXTRA_SERVER_NAME, server.name)
            }
            ctx.startService(intent)
            viewModelScope.launch { runCatching { repo.incrementUsage(server.id) } }
        }.onFailure { e ->
            Log.e(TAG, "connectXray failed: ${e.message}", e)
            XrayVpnService.setError("فشل بدء Xray: ${e.message}")
        }
    }

    fun disconnect() = disconnectAll()

    /**
     * Stops active VPN services.
     * IMPORTANT: Only sends DISCONNECT to a service that is currently
     * running (not DISCONNECTED). Starting a VPN service just to stop it
     * throws ForegroundServiceStartNotAllowedException on Android 12+.
     */
    private fun disconnectAll() {
        // WireGuard service
        if (VpnProService.state.value != VpnState.DISCONNECTED) {
            runCatching {
                ctx.startService(Intent(ctx, VpnProService::class.java).apply {
                    action = VpnProService.ACTION_DISCONNECT
                })
            }.onFailure { e ->
                Log.w(TAG, "WireGuard disconnect failed: ${e.message}")
                VpnProService.forceDisconnect()
            }
        }
        // Xray service
        if (XrayVpnService.state.value != VpnState.DISCONNECTED) {
            runCatching {
                ctx.startService(Intent(ctx, XrayVpnService::class.java).apply {
                    action = XrayVpnService.ACTION_DISCONNECT
                })
            }.onFailure { e ->
                Log.w(TAG, "Xray disconnect failed: ${e.message}")
                XrayVpnService.forceDisconnect()
            }
        }
    }

    fun needsVpnPermission(): Boolean = VpnService.prepare(ctx) != null

    /**
     * Attempt to connect to a free config.
     * Returns true if VPN permission is still needed (caller must show dialog).
     */
    fun connectFreeConfig(freeConfig: FreeConfigFetcher.FreeConfig): Boolean {
        val needsPermission = VpnService.prepare(ctx) != null
        if (needsPermission) {
            _pendingFreeConfig.value = freeConfig
            return true
        }
        doConnectFreeConfig(freeConfig)
        return false
    }

    fun onVpnPermissionGranted() {
        val config = _pendingFreeConfig.value ?: return
        _pendingFreeConfig.value = null
        doConnectFreeConfig(config)
    }

    fun clearPendingFreeConfig() {
        _pendingFreeConfig.value = null
    }

    private fun doConnectFreeConfig(freeConfig: FreeConfigFetcher.FreeConfig) {
        viewModelScope.launch {
            try {
                val result = ConfigParser.parse(freeConfig.link)
                val server = Server.fromXrayLink(freeConfig.link, result.json, freeConfig.name)
                _selectedServer.value = server
                // Disconnect only Xray if it's already running
                if (XrayVpnService.state.value != VpnState.DISCONNECTED) {
                    runCatching {
                        ctx.startService(Intent(ctx, XrayVpnService::class.java).apply {
                            action = XrayVpnService.ACTION_DISCONNECT
                        })
                    }
                }
                connectXray(server)
            } catch (e: Exception) {
                Log.e(TAG, "doConnectFreeConfig failed: ${e.message}", e)
                XrayVpnService.setError("فشل تحليل الرابط: ${e.message}")
            }
        }
    }

    fun addServer(server: Server) {
        viewModelScope.launch {
            _addServerLoading.value = true
            _addServerError.value   = null
            try {
                if (server.serverProtocol == ServerProtocol.WIREGUARD) {
                    validateWireGuardServer(server)
                }
                repo.addServer(server.copy(addedAt = System.currentTimeMillis()))
                _addServerSuccess.value = true
            } catch (e: Exception) {
                _addServerError.value = e.message ?: "Unknown error"
            } finally {
                _addServerLoading.value = false
            }
        }
    }

    fun importServerFromConfig(configText: String, name: String) {
        val server = Server.fromWireGuardConfig(configText, name)
        if (server == null) {
            _addServerError.value =
                "Invalid WireGuard config — check that it contains [Interface] and [Peer] sections"
            return
        }
        addServer(server)
    }

    fun importXrayLink(link: String) {
        viewModelScope.launch {
            _addServerLoading.value = true
            _addServerError.value   = null
            try {
                val result = ConfigParser.parse(link)
                val server = Server.fromXrayLink(link, result.json, result.name)
                repo.addServer(server)
                _addServerSuccess.value = true
            } catch (e: Exception) {
                _addServerError.value = "فشل تحليل الرابط: ${e.message}"
            } finally {
                _addServerLoading.value = false
            }
        }
    }

    fun fetchFreeConfigs() {
        viewModelScope.launch {
            _freeConfigsLoading.value = true
            _freeConfigsError.value   = null
            try {
                val configs = FreeConfigFetcher.fetchAll(maxConfigs = 150)
                _freeConfigs.value = configs
                if (configs.isEmpty()) {
                    _freeConfigsError.value = "لم يتم العثور على سيرفرات مجانية. تحقق من الاتصال بالإنترنت."
                }
            } catch (e: Exception) {
                _freeConfigsError.value = "خطأ في الجلب: ${e.message}"
            } finally {
                _freeConfigsLoading.value = false
            }
        }
    }

    fun updateServer(server: Server) {
        viewModelScope.launch { runCatching { repo.updateServer(server) } }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch { runCatching { repo.deleteServer(serverId) } }
    }

    fun toggleFavorite(server: Server) {
        updateServer(server.copy(isFavorite = !server.isFavorite))
    }

    fun clearAddServerResult() {
        _addServerSuccess.value = false
        _addServerError.value   = null
    }

    private fun validateWireGuardServer(s: Server) {
        require(s.name.isNotBlank())              { "Server name is required" }
        require(s.endpoint.contains(":"))         { "Endpoint must be host:port (e.g. 1.2.3.4:51820)" }
        require(s.serverPublicKey.length >= 40)   { "Server public key is too short or invalid" }
        require(s.clientPrivateKey.length >= 40)  { "Client private key is too short or invalid" }
        require(s.clientAddress.isNotBlank())     { "Client address is required (e.g. 10.0.0.2/32)" }
    }
}
