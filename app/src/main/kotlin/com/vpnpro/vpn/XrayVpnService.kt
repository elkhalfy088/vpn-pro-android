package com.vpnpro.vpn

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vpnpro.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import libv2ray.CoreCallbackHandler

class XrayVpnService : VpnService(), CoreCallbackHandler {

    companion object {
        const val ACTION_CONNECT    = "com.vpnpro.XRAY_CONNECT"
        const val ACTION_DISCONNECT = "com.vpnpro.XRAY_DISCONNECT"
        const val EXTRA_CONFIG_JSON = "xray_config_json"
        const val EXTRA_SERVER_NAME = "xray_server_name"

        private const val CHANNEL_ID = "vpnpro_xray_channel"
        private const val NOTIF_ID   = 1002
        private const val TAG        = "XrayVpnService"

        private val _state = MutableStateFlow(VpnState.DISCONNECTED)
        val state: StateFlow<VpnState> = _state.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        private val _connectedServerName = MutableStateFlow<String?>(null)
        val connectedServerName: StateFlow<String?> = _connectedServerName.asStateFlow()

        private val _stats = MutableStateFlow(VpnStats())
        val stats: StateFlow<VpnStats> = _stats.asStateFlow()

        /** Set error state from outside the service (e.g. config parse failure) */
        fun setError(message: String) {
            _state.value     = VpnState.ERROR
            _lastError.value = message
        }

        /** Force state reset without going through the service (fallback when startService fails) */
        fun forceDisconnect() {
            _state.value               = VpnState.DISCONNECTED
            _connectedServerName.value = null
            _stats.value               = VpnStats()
            _lastError.value           = null
        }
    }

    private val scope      = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunFd: ParcelFileDescriptor? = null
    private var isForeground = false   // track whether startForeground was called

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            XrayController.init(this)
        } catch (e: Exception) {
            Log.e(TAG, "XrayController.init failed: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action")
        when (action) {
            ACTION_CONNECT    -> handleConnect(intent)
            ACTION_DISCONNECT -> handleDisconnect()
            else              -> safeStopSelf()   // null / unknown — stop cleanly
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onRevoke() {
        handleDisconnect()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── CoreCallbackHandler ───────────────────────────────────────────────

    override fun startup(): Long {
        Log.i(TAG, "Xray core startup")
        return 0
    }

    override fun shutdown(): Long {
        Log.i(TAG, "Xray core shutdown")
        scope.launch(Dispatchers.Main) {
            if (_state.value == VpnState.CONNECTED) {
                _state.value = VpnState.DISCONNECTED
            }
        }
        return 0
    }

    override fun onEmitStatus(level: Long, message: String): Long {
        Log.i(TAG, "Xray [$level]: $message")
        return 0
    }

    // ── Connect / Disconnect ──────────────────────────────────────────────

    private fun handleConnect(intent: Intent) {
        val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: run {
            Log.w(TAG, "handleConnect: missing config"); return
        }
        val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "VPN"

        _state.value     = VpnState.CONNECTING
        _lastError.value = null

        // startForeground must be called synchronously before the coroutine
        safeStartForeground("Connecting to $serverName…")

        scope.launch {
            try {
                val tun = Builder()
                    .addAddress("10.0.0.1", 24)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("::", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .setSession("VPN Pro Xray")
                    .setMtu(1500)
                    .establish()
                    ?: throw Exception("فشل إنشاء نفق VPN — تأكد من منح إذن VPN")

                tunFd = tun
                XrayController.start(configJson, tun.fd, this@XrayVpnService)

                delay(1500)

                if (XrayController.isRunning) {
                    _state.value               = VpnState.CONNECTED
                    _connectedServerName.value = serverName
                    _stats.value               = VpnStats(connectedSince = System.currentTimeMillis())
                    safeStartForeground("Connected — $serverName")
                    startStatsPolling()
                } else {
                    throw Exception("فشل تشغيل Xray core — جرب سيرفراً آخر")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connect failed: ${e.message}", e)
                _state.value     = VpnState.ERROR
                _lastError.value = e.message ?: "فشل الاتصال"
                safeStartForeground("Connection failed")
                closeTun()
            }
        }
    }

    private fun handleDisconnect() {
        Log.d(TAG, "handleDisconnect, isForeground=$isForeground")
        scope.launch {
            try { XrayController.stop() } catch (e: Exception) { Log.w(TAG, "XrayController.stop: ${e.message}") }
            closeTun()
            _state.value               = VpnState.DISCONNECTED
            _connectedServerName.value = null
            _stats.value               = VpnStats()
        }
        safeStopSelf()
    }

    private fun closeTun() {
        try { tunFd?.close() } catch (_: Exception) {}
        tunFd = null
    }

    private fun startStatsPolling() {
        scope.launch {
            val start = System.currentTimeMillis()
            while (isActive && _state.value == VpnState.CONNECTED) {
                delay(2000)
                _stats.value = _stats.value.copy(connectedSince = start)
            }
        }
    }

    // ── Safe foreground helpers ───────────────────────────────────────────

    private fun safeStartForeground(status: String) {
        try {
            val notif = buildNotification(status)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, notif)
            }
            isForeground = true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.message}", e)
        }
    }

    private fun safeStopSelf() {
        try {
            if (isForeground) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForeground = false
            }
        } catch (e: Exception) {
            Log.w(TAG, "stopForeground failed: ${e.message}")
        }
        try { stopSelf() } catch (e: Exception) { Log.w(TAG, "stopSelf failed: ${e.message}") }
    }

    // ── Notification ──────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "VPN Pro Xray", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Xray VPN status" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(status: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, XrayVpnService::class.java).also { it.action = ACTION_DISCONNECT },
            PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VPN Pro — Xray")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Disconnect", stopIntent)
            .build()
    }
}
