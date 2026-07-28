package akh.vpn.dd.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import akh.vpn.dd.MainActivity
import akh.vpn.dd.R
import akh.vpn.dd.tunnel.SniTunnel
import akh.vpn.dd.viewmodel.VpnStatus
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong

class VpnProService : VpnService() {

    companion object {
        const val ACTION_CONNECT    = "akh.vpn.dd.CONNECT"
        const val ACTION_DISCONNECT = "akh.vpn.dd.DISCONNECT"
        const val EXTRA_HOST   = "host"
        const val EXTRA_PORT   = "port"
        const val EXTRA_DECOY  = "decoy"

        const val NOTIF_CHANNEL = "vpn_channel"
        const val NOTIF_ID = 1

        @Volatile var currentStatus: VpnStatus = VpnStatus.DISCONNECTED
        @Volatile var bytesIn: Long  = 0L
        @Volatile var bytesOut: Long = 0L
        @Volatile var latencyMs: Long = 0L

        private const val TAG = "VpnProService"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnel: SniTunnel? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val host  = intent.getStringExtra(EXTRA_HOST) ?: return START_NOT_STICKY
                val port  = intent.getIntExtra(EXTRA_PORT, 8443)
                val decoy = intent.getStringExtra(EXTRA_DECOY) ?: "www.facebook.com"
                startVpn(host, port, decoy)
            }
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(host: String, port: Int, decoy: String) {
        currentStatus = VpnStatus.CONNECTING
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("جاري الاتصال..."))

        scope.launch {
            try {
                // Build VPN interface
                val builder = Builder()
                    .addAddress("10.8.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .setMtu(1400)
                    .setSession("Vpn Pro")
                    .setBlocking(true)

                vpnInterface = builder.establish()
                    ?: throw Exception("Failed to establish VPN interface")

                SniTunnel.reset()

                // Connect SNI-spoofed tunnel to VPS
                tunnel = SniTunnel(host, port, decoy)
                var tunnelReady = false

                tunnel!!.connectWebSocket(
                    onOpen = {
                        tunnelReady = true
                        currentStatus = VpnStatus.CONNECTED
                        updateNotification("متصل ✓ — SNI: $decoy")
                        Log.d(TAG, "Tunnel open — ISP sees traffic to $decoy")
                    },
                    onMessage = { data ->
                        // Data from VPS → write to TUN (device gets response)
                        bytesIn += data.size
                        vpnInterface?.fileDescriptor?.let { fd ->
                            FileOutputStream(fd).write(data)
                        }
                    },
                    onClose = {
                        currentStatus = VpnStatus.DISCONNECTED
                        updateNotification("غير متصل")
                        stopSelf()
                    }
                )

                // Wait for tunnel to open (max 15s)
                var waited = 0
                while (!tunnelReady && waited < 150) {
                    delay(100)
                    waited++
                }
                if (!tunnelReady) {
                    throw Exception("Tunnel connection timed out")
                }

                // Read packets from TUN → forward to VPS via tunnel
                val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
                val buf = ByteArray(32_768)
                while (currentStatus == VpnStatus.CONNECTED) {
                    val n = inputStream.read(buf)
                    if (n > 0) {
                        val packet = buf.copyOf(n)
                        bytesOut += n
                        tunnel!!.sendData(packet)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "VPN error: ${e.message}")
                currentStatus = VpnStatus.ERROR
                updateNotification("خطأ في الاتصال")
                stopVpn()
            }
        }

        // Ping loop for latency stats
        scope.launch {
            while (currentStatus == VpnStatus.CONNECTED || currentStatus == VpnStatus.CONNECTING) {
                delay(5000)
                try {
                    val start = System.currentTimeMillis()
                    val reachable = InetAddress.getByName("1.1.1.1").isReachable(2000)
                    if (reachable) latencyMs = System.currentTimeMillis() - start
                } catch (_: Exception) {}
            }
        }

        // Sync stats from tunnel
        scope.launch {
            while (currentStatus == VpnStatus.CONNECTED) {
                delay(1000)
                bytesIn  = SniTunnel.totalBytesIn.get()
                bytesOut = SniTunnel.totalBytesOut.get()
                latencyMs = SniTunnel.lastLatencyMs.get()
            }
        }
    }

    private fun stopVpn() {
        scope.coroutineContext.cancelChildren()
        tunnel?.disconnect()
        tunnel = null
        vpnInterface?.close()
        vpnInterface = null
        currentStatus = VpnStatus.DISCONNECTED
        bytesIn = 0; bytesOut = 0; latencyMs = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIF_CHANNEL, "VPN Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Vpn Pro حالة الاتصال" }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val disconnectIntent = Intent(this, VpnProService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPending = PendingIntent.getService(
            this, 0, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val mainIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("Vpn Pro")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(mainIntent)
            .addAction(android.R.drawable.ic_delete, "قطع الاتصال", disconnectPending)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(text))
    }
}
