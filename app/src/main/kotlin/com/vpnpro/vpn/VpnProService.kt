package com.vpnpro.vpn

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vpnpro.MainActivity
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.StringReader

class VpnProService : VpnService() {

    companion object {
        const val ACTION_CONNECT    = "com.vpnpro.CONNECT"
        const val ACTION_DISCONNECT = "com.vpnpro.DISCONNECT"
        const val EXTRA_CONFIG      = "wireguard_config"
        const val EXTRA_SERVER_NAME = "server_name"
        private const val CHANNEL_ID = "vpnpro_channel"
        private const val NOTIF_ID   = 1001

        private val _state = MutableStateFlow(VpnState.DISCONNECTED)
        val state: StateFlow<VpnState> = _state.asStateFlow()

        private val _stats = MutableStateFlow(VpnStats())
        val stats: StateFlow<VpnStats> = _stats.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        private val _connectedServerName = MutableStateFlow<String?>(null)
        val connectedServerName: StateFlow<String?> = _connectedServerName.asStateFlow()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var backend: GoBackend? = null
    private var activeTunnel: VpnProTunnel? = null
    private var statsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        backend = GoBackend(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT    -> handleConnect(intent)
            ACTION_DISCONNECT -> handleDisconnect()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onRevoke() {
        handleDisconnect()
    }

    private fun handleConnect(intent: Intent) {
        val configText = intent.getStringExtra(EXTRA_CONFIG) ?: return
        val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "VPN Pro"

        _state.value     = VpnState.CONNECTING
        _lastError.value = null
        showNotification("Connecting to $serverName…")

        scope.launch {
            try {
                val config = Config.parse(BufferedReader(StringReader(configText)))
                val tunnel = VpnProTunnel(serverName)
                backend?.setState(tunnel, Tunnel.State.UP, config)
                activeTunnel             = tunnel
                _state.value             = VpnState.CONNECTED
                _connectedServerName.value = serverName
                _stats.value             = VpnStats(connectedSince = System.currentTimeMillis())
                showNotification("Connected — $serverName")
                startStatsPolling()
            } catch (e: Exception) {
                _state.value     = VpnState.ERROR
                _lastError.value = e.message ?: "Connection failed"
                showNotification("Connection failed")
            }
        }
    }

    private fun handleDisconnect() {
        statsJob?.cancel()
        scope.launch {
            try {
                activeTunnel?.let { backend?.setState(it, Tunnel.State.DOWN, null) }
            } catch (_: Exception) {}
            activeTunnel              = null
            _state.value              = VpnState.DISCONNECTED
            _connectedServerName.value = null
            _stats.value              = VpnStats()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (isActive) {
                delay(2000)
                try {
                    val tun  = activeTunnel ?: break
                    val s    = backend?.getStatistics(tun)
                    val rx   = s?.totalRx() ?: 0L
                    val tx   = s?.totalTx() ?: 0L
                    _stats.value = _stats.value.copy(bytesIn = rx, bytesOut = tx)
                } catch (_: Exception) {}
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "VPN Pro", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "VPN connection status" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun showNotification(status: String) {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, VpnProService::class.java).also { it.action = ACTION_DISCONNECT },
            PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VPN Pro")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Disconnect", stopIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

// ── Support types ────────────────────────────────────────────────────────────

enum class VpnState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class VpnStats(
    val bytesIn: Long       = 0L,
    val bytesOut: Long      = 0L,
    val connectedSince: Long = 0L
)

class VpnProTunnel(private val name: String) : Tunnel {
    private var state = Tunnel.State.DOWN
    override fun getName(): String = name
    override fun onStateChange(newState: Tunnel.State) { state = newState }
}
