package com.vpnpro.data.model

data class Server(
    val id: String = "",
    val name: String = "",
    val flag: String = "🌐",
    val location: String = "",
    // WireGuard connection info
    val endpoint: String = "",          // host:port e.g. "1.2.3.4:51820"
    val serverPublicKey: String = "",   // server's WireGuard public key
    val clientPrivateKey: String = "",  // client private key (shared for this server slot)
    val clientAddress: String = "",     // e.g. "10.0.0.2/32"
    val dns: String = "1.1.1.1, 1.0.0.1",
    val allowedIPs: String = "0.0.0.0/0, ::/0",
    val preSharedKey: String = "",      // optional
    val addedBy: String = "",
    val addedAt: Long = 0L,
    val enabled: Boolean = true,
    val ping: Int = -1                  // -1 = not measured
) {
    /** Convert to WireGuard config file format */
    fun toWireGuardConfig(): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $clientPrivateKey")
        appendLine("Address = $clientAddress")
        appendLine("DNS = $dns")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = $serverPublicKey")
        if (preSharedKey.isNotBlank()) appendLine("PresharedKey = $preSharedKey")
        appendLine("Endpoint = $endpoint")
        appendLine("AllowedIPs = $allowedIPs")
        appendLine("PersistentKeepalive = 25")
    }

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "flag" to flag,
        "location" to location,
        "endpoint" to endpoint,
        "serverPublicKey" to serverPublicKey,
        "clientPrivateKey" to clientPrivateKey,
        "clientAddress" to clientAddress,
        "dns" to dns,
        "allowedIPs" to allowedIPs,
        "preSharedKey" to preSharedKey,
        "addedBy" to addedBy,
        "addedAt" to addedAt,
        "enabled" to enabled
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Server = Server(
            id              = id,
            name            = map["name"] as? String ?: "",
            flag            = map["flag"] as? String ?: "🌐",
            location        = map["location"] as? String ?: "",
            endpoint        = map["endpoint"] as? String ?: "",
            serverPublicKey = map["serverPublicKey"] as? String ?: "",
            clientPrivateKey= map["clientPrivateKey"] as? String ?: "",
            clientAddress   = map["clientAddress"] as? String ?: "",
            dns             = map["dns"] as? String ?: "1.1.1.1",
            allowedIPs      = map["allowedIPs"] as? String ?: "0.0.0.0/0",
            preSharedKey    = map["preSharedKey"] as? String ?: "",
            addedBy         = map["addedBy"] as? String ?: "",
            addedAt         = map["addedAt"] as? Long ?: 0L,
            enabled         = map["enabled"] as? Boolean ?: true
        )
    }
}
