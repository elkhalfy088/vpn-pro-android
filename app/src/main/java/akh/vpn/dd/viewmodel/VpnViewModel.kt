package akh.vpn.dd.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import akh.vpn.dd.data.FirebaseManager
import akh.vpn.dd.data.VpnConfig
import akh.vpn.dd.service.VpnProService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class VpnUiState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val config: VpnConfig? = null,
    val isSetupRequired: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val latencyMs: Long = 0L,
    val connectedSeconds: Long = 0L
)

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    private var statsJob: kotlinx.coroutines.Job? = null

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            try {
                val config = FirebaseManager.readConfig()
                _uiState.value = _uiState.value.copy(
                    config = config,
                    isSetupRequired = config == null || !config.setupDone || !config.isValid(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "لا يمكن الاتصال بـ Firebase: ${e.message}"
                )
            }
        }
    }

    fun saveSetup(host: String, port: Int, decoyDomain: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val config = VpnConfig(serverHost = host, serverPort = port, decoyDomain = decoyDomain)
                FirebaseManager.saveConfig(config)
                _uiState.value = _uiState.value.copy(
                    config = config,
                    isSetupRequired = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "فشل الحفظ: ${e.message}"
                )
            }
        }
    }

    fun toggleVpn() {
        val currentStatus = _uiState.value.status
        if (currentStatus == VpnStatus.CONNECTED || currentStatus == VpnStatus.CONNECTING) {
            disconnectVpn()
        } else {
            connectVpn()
        }
    }

    fun connectVpn() {
        val config = _uiState.value.config ?: return
        if (!config.isValid()) {
            _uiState.value = _uiState.value.copy(errorMessage = "إعدادات السيرفر غير مكتملة")
            return
        }
        // Check VPN permission
        val intent = VpnService.prepare(getApplication())
        if (intent != null) {
            // Need permission — signal to Activity to request it
            _uiState.value = _uiState.value.copy(status = VpnStatus.CONNECTING)
            return
        }
        startVpnService(config)
    }

    fun onVpnPermissionGranted() {
        val config = _uiState.value.config ?: return
        startVpnService(config)
    }

    private fun startVpnService(config: VpnConfig) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, VpnProService::class.java).apply {
            action = VpnProService.ACTION_CONNECT
            putExtra(VpnProService.EXTRA_HOST, config.serverHost)
            putExtra(VpnProService.EXTRA_PORT, config.serverPort)
            putExtra(VpnProService.EXTRA_DECOY, config.decoyDomain)
        }
        ctx.startForegroundService(intent)
        _uiState.value = _uiState.value.copy(status = VpnStatus.CONNECTING, errorMessage = null)
        observeServiceStatus()
    }

    fun disconnectVpn() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, VpnProService::class.java).apply {
            action = VpnProService.ACTION_DISCONNECT
        }
        ctx.startService(intent)
        statsJob?.cancel()
        _uiState.value = _uiState.value.copy(
            status = VpnStatus.DISCONNECTED,
            bytesIn = 0, bytesOut = 0, latencyMs = 0, connectedSeconds = 0
        )
    }

    private fun observeServiceStatus() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            var seconds = 0L
            while (true) {
                kotlinx.coroutines.delay(1000)
                val status = VpnProService.currentStatus
                seconds = if (status == VpnStatus.CONNECTED) seconds + 1 else 0L
                _uiState.value = _uiState.value.copy(
                    status = status,
                    bytesIn = VpnProService.bytesIn,
                    bytesOut = VpnProService.bytesOut,
                    latencyMs = VpnProService.latencyMs,
                    connectedSeconds = seconds
                )
                if (status == VpnStatus.DISCONNECTED || status == VpnStatus.ERROR) break
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
