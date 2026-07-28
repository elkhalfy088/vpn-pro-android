package com.vpnpro.data.model

import com.google.firebase.database.IgnoreExtraProperties

enum class ServerProtocol { WIREGUARD, XRAY }

@IgnoreExtraProperties
data class Server(
    val id: String = "",
    val name: String = "",
    val flag: String = "🌐",
    val location: String = "",
    // ── WireGuard fields ─────────────────────────────────────────────────────
    val endpoint: String = "",
    val serverPublicKey: String = "",
    val clientPrivateKey: String = "",
    val clientAddress: String = "10.0.0.2/32",
    val dns: String = "1.1.1.1, 1.0.0.1",
    val allowedIPs: String = "0.0.0.0/0, ::/0",
    val preSharedKey: String = "",
    val mtu: Int = 1420,
    val persistentKeepalive: Int = 25,
    // ── Xray / V2Ray fields ──────────────────────────────────────────────────
    val protocol: String = "WIREGUARD",   // "WIREGUARD" | "XRAY"
    val xrayConfigJson: String = "",      // Full Xray JSON config (when protocol=XRAY)
    val xrayLink: String = "",            // Original vmess:// vless:// trojan:// link
    // ── Shared fields ────────────────────────────────────────────────────────
    val addedBy: String = "",
    val addedAt: Long = 0L,
    val usageCount: Int = 0,
    val isPremium: Boolean = false,
    val category: String = "General"
) {
    val serverProtocol: ServerProtocol
        get() = if (protocol == "XRAY") ServerProtocol.XRAY else ServerProtocol.WIREGUARD

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

    companion object {
        /** Build a Server from an Xray share link (vmess:// vless:// trojan://) */
        fun fromXrayLink(link: String, xrayJson: String, parsedName: String): Server {
            return Server(
                id              = "xray_${System.currentTimeMillis()}",
                name            = parsedName.take(40),
                flag            = "🌐",
                location        = "",
                protocol        = "XRAY",
                xrayLink        = link,
                xrayConfigJson  = xrayJson,
                addedAt         = System.currentTimeMillis()
            )
        }

        /** Parse a WireGuard config text into a Server object */
        fun fromWireGuardConfig(config: String, name: String = "Imported Server"): Server? {
            return try {
                val lines = config.lines().map { it.trim() }
                var privateKey = ""
                var address    = "10.0.0.2/32"
                var dns        = "1.1.1.1, 1.0.0.1"
                var mtu        = 1420
                var publicKey  = ""
                var psk        = ""
                var endpoint   = ""
                var allowedIPs = "0.0.0.0/0, ::/0"
                var keepalive  = 25

                for (line in lines) {
                    when {
                        line.startsWith("PrivateKey")          -> privateKey = line.substringAfter("=").trim()
                        line.startsWith("Address")             -> address    = line.substringAfter("=").trim()
                        line.startsWith("DNS")                 -> dns        = line.substringAfter("=").trim()
                        line.startsWith("MTU")                 -> mtu        = line.substringAfter("=").trim().toIntOrNull() ?: 1420
                        line.startsWith("PublicKey")           -> publicKey  = line.substringAfter("=").trim()
                        line.startsWith("PresharedKey")        -> psk        = line.substringAfter("=").trim()
                        line.startsWith("Endpoint")            -> endpoint   = line.substringAfter("=").trim()
                        line.startsWith("AllowedIPs")          -> allowedIPs = line.substringAfter("=").trim()
                        line.startsWith("PersistentKeepalive") -> keepalive  = line.substringAfter("=").trim().toIntOrNull() ?: 25
                    }
                }

                if (privateKey.isBlank() || publicKey.isBlank() || endpoint.isBlank()) return null

                Server(
                    id              = "wg_${System.currentTimeMillis()}",
                    name            = name,
                    flag            = "🌐",
                    location        = endpoint.substringBefore(":"),
                    endpoint        = endpoint,
                    serverPublicKey = publicKey,
                    clientPrivateKey= privateKey,
                    clientAddress   = address,
                    dns             = dns,
                    allowedIPs      = allowedIPs,
                    preSharedKey    = psk,
                    mtu             = mtu,
                    persistentKeepalive = keepalive,
                    protocol        = "WIREGUARD",
                    addedAt         = System.currentTimeMillis()
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
