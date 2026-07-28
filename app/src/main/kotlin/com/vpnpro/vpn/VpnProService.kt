package com.vpnpro.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vpnpro.MainActivity
import com.vpnpro.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wireguard.android.backend.GoBackend
import org.wireguard.android.backend.Tunnel
import org.wireguard.config.Config
import org.wireguard.crypto.Key
import java.io.BufferedReader
import java.io.StringReader

enum class VpnState { IDLE, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

data class VpnStats(
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val connectedSince: Long = 0L
)

class VpnProService : VpnService() {

    companion object {
        const val ACTION_CONNECT    = "com.vpnpro.CONNECT"
        const val ACTION_DISCONNECT = "com.vpnpro.DISCONNECT"
        const val EXTRA_CONFIG      = "config"
        const val EXTRA_SERVER_NAME = "server_name"

        private val _state = MutableStateFlow(VpnState.IDLE)
        val state: StateFlow<VpnState> = _state.asStateFlow()

        private val _stats = MutableStateFlow(VpnStats())
        val stats: StateFlow<VpnStats> = _stats.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        private val _connectedServerName = MutableStateFlow<String?>(null)
        val connectedServerName: StateFlow<String?> = _connectedServerName.asStateFlow()

        private const val TAG = "VpnProService"
        private const val CHANNEL_ID = "vpn_pro_channel"
        private const val NOTIF_ID = 1001
    }

    private var backend: GoBackend? = null
    private var tunnel: Tunnel? = null
    private var currentConfig: String? = null
    private var currentServerName: String? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var statsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            backend = GoBackend(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init GoBackend: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val config = intent.getStringExtra(EXTRA_CONFIG) ?: return START_NOT_STICKY
                val name   = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Server"
                currentConfig = config
                currentServerName = name
                serviceScope.launch { doConnect(config, name) }
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch { doDisconnect() }
            }
        }
        return START_STICKY
    }

    private suspend fun doConnect(configText: String, serverName: String) {
        _state.value = VpnState.CONNECTING
        _lastError.value = null
        try {
            startForeground(NOTIF_ID, buildNotification("Connecting to $serverName…"))

            val cfg = Config.parse(BufferedReader(StringReader(configText)))
            val t = object : Tunnel {
                override fun getName() = "vpnpro"
                override fun onStateChange(newState: Tunnel.State) {
                    _state.value = when (newState) {
                        Tunnel.State.UP   -> VpnState.CONNECTED
                        Tunnel.State.DOWN -> VpnState.IDLE
                        else              -> VpnState.IDLE
                    }
                }
            }
            tunnel = t
            backend?.setState(t, Tunnel.State.UP, cfg)

            _state.value = VpnState.CONNECTED
            _connectedServerName.value = serverName
            _stats.value = VpnStats(connectedSince = System.currentTimeMillis())

            updateNotification("Connected — $serverName")
            startStatsPolling(t)

        } catch (e: Exception) {
            Log.e(TAG, "Connect failed: ${e.message}")
            _state.value = VpnState.ERROR
            _lastError.value = e.message ?: "Connection failed"
            updateNotification("Connection failed")
            stopForeground(true)
        }
    }

    private suspend fun doDisconnect() {
        _state.value = VpnState.DISCONNECTING
        statsJob?.cancel()
        try {
            tunnel?.let { backend?.setState(it, Tunnel.State.DOWN, null) }
        } catch (e: Exception) {
            Log.w(TAG, "Disconnect error: ${e.message}")
        }
        tunnel = null
        _state.value = VpnState.IDLE
        _stats.value = VpnStats()
        _connectedServerName.value = null
        stopForeground(true)
        stopSelf()
    }

    private fun startStatsPolling(t: Tunnel) {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            while (isActive) {
                try {
                    val s = backend?.getStatistics(t)
                    if (s != null) {
                        var totalIn = 0L; var totalOut = 0L
                        for (peer in s.peers()) {
                            totalIn  += s.peer(peer)?.rxBytes ?: 0L
                            totalOut += s.peer(peer)?.txBytes ?: 0L
                        }
                        _stats.value = _stats.value.copy(bytesIn = totalIn, bytesOut = totalOut)
                    }
                } catch (_: Exception) {}
                delay(2000)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "VPN Pro Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "VPN connection status" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VPN Pro")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
