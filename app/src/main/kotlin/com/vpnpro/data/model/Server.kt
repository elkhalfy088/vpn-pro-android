package com.vpnpro.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Server(
    val id: String = "",
    val name: String = "",
    val flag: String = "🌐",
    val location: String = "",
    val endpoint: String = "",
    val serverPublicKey: String = "",
    val clientPrivateKey: String = "",
    val clientAddress: String = "10.0.0.2/32",
    val dns: String = "1.1.1.1, 1.0.0.1",
    val allowedIPs: String = "0.0.0.0/0, ::/0",
    val preSharedKey: String = "",
    val addedBy: String = "",
    val addedAt: Long = 0L,
    val usageCount: Int = 0,
    val isPremium: Boolean = false,
    val mtu: Int = 1420,
    val persistentKeepalive: Int = 25,
    val category: String = "General"
) {
    /** Generate WireGuard config text from this server */
    fun toWireGuardConfig(): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $clientPrivateKey")
        appendLine("Address = $clientAddress")
        appendLine("DNS = ${dns.replace(" ", "")}")
        appendLine("MTU = $mtu")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = $serverPublicKey")
        if (preSharedKey.isNotBlank()) appendLine("PresharedKey = $preSharedKey")
        appendLine("Endpoint = $endpoint")
        appendLine("AllowedIPs = $allowedIPs")
        appendLine("PersistentKeepalive = $persistentKeepalive")
    }

    /** Parse a WireGuard config text into a Server object */
    companion object {
        fun fromWireGuardConfig(config: String, name: String = "Imported Server"): Server? {
            return try {
                val lines = config.lines().map { it.trim() }
                var privateKey = ""
                var address = "10.0.0.2/32"
                var dns = "1.1.1.1, 1.0.0.1"
                var mtu = 1420
                var publicKey = ""
                var psk = ""
                var endpoint = ""
                var allowedIPs = "0.0.0.0/0, ::/0"
                var keepalive = 25

                for (line in lines) {
                    when {
                        line.startsWith("PrivateKey") -> privateKey = line.substringAfter("=").trim()
                        line.startsWith("Address")    -> address    = line.substringAfter("=").trim()
                        line.startsWith("DNS")        -> dns        = line.substringAfter("=").trim()
                        line.startsWith("MTU")        -> mtu        = line.substringAfter("=").trim().toIntOrNull() ?: 1420
                        line.startsWith("PublicKey")  -> publicKey  = line.substringAfter("=").trim()
                        line.startsWith("PresharedKey")-> psk       = line.substringAfter("=").trim()
                        line.startsWith("Endpoint")   -> endpoint   = line.substringAfter("=").trim()
                        line.startsWith("AllowedIPs") -> allowedIPs = line.substringAfter("=").trim()
                        line.startsWith("PersistentKeepalive") -> keepalive = line.substringAfter("=").trim().toIntOrNull() ?: 25
                    }
                }

                if (privateKey.isBlank() || publicKey.isBlank() || endpoint.isBlank()) return null

                Server(
                    id = "imported_${System.currentTimeMillis()}",
                    name = name,
                    flag = "🌐",
                    location = endpoint.substringBefore(":"),
                    endpoint = endpoint,
                    serverPublicKey = publicKey,
                    clientPrivateKey = privateKey,
                    clientAddress = address,
                    dns = dns,
                    allowedIPs = allowedIPs,
                    preSharedKey = psk,
                    mtu = mtu,
                    persistentKeepalive = keepalive,
                    addedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
