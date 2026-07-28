package akh.vpn.dd.tunnel

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * SNI-spoofing tunnel:
 *  - Connects to VPS via HTTPS/WSS
 *  - TLS ClientHello SNI = decoyDomain (e.g. "www.facebook.com")
 *  - ISP sees: HTTPS traffic to "facebook.com"
 *  - Actually: encrypted tunnel to your VPS
 */
class SniTunnel(
    private val serverHost: String,
    private val serverPort: Int,
    private val decoyDomain: String = "www.facebook.com"
) {
    companion object {
        private const val TAG = "SniTunnel"
        val totalBytesIn  = AtomicLong(0)
        val totalBytesOut = AtomicLong(0)
        val lastLatencyMs = AtomicLong(0)

        fun reset() {
            totalBytesIn.set(0)
            totalBytesOut.set(0)
            lastLatencyMs.set(0)
        }
    }

    private var webSocket: WebSocket? = null
    private var connected = false

    /** Trust-all manager — in production replace with certificate pinning */
    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val sslContext = SSLContext.getInstance("TLS").also {
        it.init(null, arrayOf(trustAllManager), SecureRandom())
    }

    /**
     * Create an SSLSocket connected to serverHost:serverPort
     * but with SNI set to decoyDomain so ISP DPI sees "facebook.com"
     */
    fun createSpoofedSocket(): SSLSocket {
        val rawSocket = Socket()
        rawSocket.connect(InetSocketAddress(serverHost, serverPort), 10_000)

        val sslSocket = sslContext.socketFactory.createSocket(
            rawSocket, decoyDomain, serverPort, true
        ) as SSLSocket

        // Set SNI to decoy domain — this is what ISP sees in TLS ClientHello
        val params = SSLParameters()
        params.serverNames = listOf(javax.net.ssl.SNIHostName(decoyDomain))
        sslSocket.sslParameters = params

        // Don't verify hostname against cert (since SNI is fake)
        sslSocket.useClientMode = true
        sslSocket.startHandshake()

        Log.d(TAG, "Connected with SNI=$decoyDomain to $serverHost:$serverPort")
        return sslSocket
    }

    /**
     * Open WebSocket tunnel to VPS with SNI spoofing.
     * VPS must run a WebSocket proxy server (see README).
     */
    fun connectWebSocket(onOpen: () -> Unit, onMessage: (ByteArray) -> Unit, onClose: () -> Unit) {
        val client = OkHttpClient.Builder()
            .sslSocketFactory(SniSocketFactory(serverHost, serverPort, decoyDomain, sslContext), trustAllManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("wss://$serverHost:$serverPort/tunnel")
            .header("Host", decoyDomain)  // HTTP Host header also shows decoy domain
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                connected = true
                val t = System.currentTimeMillis()
                lastLatencyMs.set(System.currentTimeMillis() - t)
                onOpen()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                totalBytesIn.addAndGet(bytes.size.toLong())
                onMessage(bytes.toByteArray())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                onClose()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                connected = false
                onClose()
            }
        })
    }

    fun sendData(data: ByteArray) {
        webSocket?.send(data.toByteString())
        totalBytesOut.addAndGet(data.size.toLong())
    }

    fun disconnect() {
        connected = false
        webSocket?.close(1000, "Disconnect")
        webSocket = null
    }

    fun isConnected() = connected
}

/** Custom SSLSocketFactory that spoofs the SNI */
class SniSocketFactory(
    private val actualHost: String,
    private val actualPort: Int,
    private val decoyDomain: String,
    private val sslContext: SSLContext
) : javax.net.ssl.SSLSocketFactory() {

    override fun getDefaultCipherSuites(): Array<String> =
        sslContext.socketFactory.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> =
        sslContext.socketFactory.supportedCipherSuites

    override fun createSocket(): Socket = buildSocket()

    override fun createSocket(host: String, port: Int): Socket = buildSocket()

    override fun createSocket(host: String, port: Int, localHost: java.net.InetAddress, localPort: Int): Socket = buildSocket()

    override fun createSocket(host: java.net.InetAddress, port: Int): Socket = buildSocket()

    override fun createSocket(address: java.net.InetAddress, port: Int, localAddress: java.net.InetAddress, localPort: Int): Socket = buildSocket()

    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val ssl = sslContext.socketFactory.createSocket(socket, decoyDomain, port, autoClose) as SSLSocket
        setSni(ssl)
        return ssl
    }

    private fun buildSocket(): SSLSocket {
        val raw = Socket()
        raw.connect(InetSocketAddress(actualHost, actualPort), 10_000)
        val ssl = sslContext.socketFactory.createSocket(raw, decoyDomain, actualPort, true) as SSLSocket
        setSni(ssl)
        ssl.startHandshake()
        return ssl
    }

    private fun setSni(ssl: SSLSocket) {
        val params = ssl.sslParameters
        params.serverNames = listOf(javax.net.ssl.SNIHostName(decoyDomain))
        ssl.sslParameters = params
        ssl.useClientMode = true
    }
}
