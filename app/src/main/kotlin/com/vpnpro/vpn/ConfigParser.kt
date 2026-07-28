package com.vpnpro.vpn

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI

/**
 * Parses VMess / VLESS / Trojan / SS share links into Xray JSON config.
 */
object ConfigParser {

    data class ParseResult(val name: String, val json: String)

    fun parse(link: String): ParseResult {
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vmess://")  -> parseVmess(trimmed)
            trimmed.startsWith("vless://")  -> parseVless(trimmed)
            trimmed.startsWith("trojan://") -> parseTrojan(trimmed)
            else -> throw IllegalArgumentException("رابط غير مدعوم. يجب أن يبدأ بـ vmess:// أو vless:// أو trojan://")
        }
    }

    // ── VMess ────────────────────────────────────────────────────────────────

    private fun parseVmess(link: String): ParseResult {
        val b64 = link.removePrefix("vmess://").trim()
        val json = try {
            String(Base64.decode(b64, Base64.DEFAULT or Base64.NO_WRAP))
        } catch (_: Exception) {
            String(Base64.decode(b64, Base64.URL_SAFE or Base64.NO_PADDING))
        }
        val obj  = Gson().fromJson(json, JsonObject::class.java)

        val add  = obj.get("add")?.asString  ?: throw IllegalArgumentException("VMess: missing add")
        val port = obj.get("port")?.asString?.toIntOrNull()
            ?: obj.get("port")?.asInt
            ?: throw IllegalArgumentException("VMess: missing port")
        val id   = obj.get("id")?.asString   ?: throw IllegalArgumentException("VMess: missing id")
        val net  = obj.get("net")?.asString  ?: "tcp"
        val tls  = obj.get("tls")?.asString  ?: ""
        val path = obj.get("path")?.asString ?: ""
        val host = obj.get("host")?.asString ?: ""
        val sni  = obj.get("sni")?.asString  ?: host
        val alterId = obj.get("aid")?.asString?.toIntOrNull() ?: obj.get("aid")?.asInt ?: 0
        val ps   = obj.get("ps")?.asString   ?: add

        val config = buildXrayConfig(
            protocol = "vmess",
            address  = add,
            port     = port,
            vmessId  = id,
            alterId  = alterId,
            network  = net,
            tlsType  = if (tls == "tls") "tls" else "none",
            path     = path,
            host     = host,
            sni      = sni
        )
        return ParseResult(ps, config)
    }

    // ── VLESS ─────────────────────────────────────────────────────────────────

    private fun parseVless(link: String): ParseResult {
        val uri = URI(link)
        val id      = uri.userInfo ?: throw IllegalArgumentException("VLESS: missing uuid")
        val address = uri.host     ?: throw IllegalArgumentException("VLESS: missing host")
        val port    = if (uri.port > 0) uri.port else 443
        val name    = if (uri.fragment != null) java.net.URLDecoder.decode(uri.fragment, "UTF-8") else address

        val params   = parseQuery(uri.rawQuery)
        val network  = params["type"]     ?: "tcp"
        val security = params["security"] ?: "none"
        val sni      = params["sni"]      ?: params["peer"] ?: ""
        val path     = params["path"]?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
        val host     = params["host"]     ?: ""
        val flow     = params["flow"]     ?: ""

        val config = buildXrayConfig(
            protocol = "vless",
            address  = address,
            port     = port,
            vlessId  = id,
            flow     = flow,
            network  = network,
            tlsType  = security,
            path     = path,
            host     = host,
            sni      = sni
        )
        return ParseResult(name, config)
    }

    // ── Trojan ────────────────────────────────────────────────────────────────

    private fun parseTrojan(link: String): ParseResult {
        val uri      = URI(link)
        val password = uri.userInfo ?: throw IllegalArgumentException("Trojan: missing password")
        val address  = uri.host     ?: throw IllegalArgumentException("Trojan: missing host")
        val port     = if (uri.port > 0) uri.port else 443
        val name     = if (uri.fragment != null) java.net.URLDecoder.decode(uri.fragment, "UTF-8") else address

        val params   = parseQuery(uri.rawQuery)
        val sni      = params["sni"] ?: params["peer"] ?: ""
        val network  = params["type"] ?: "tcp"
        val path     = params["path"]?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
        val host     = params["host"] ?: ""

        val config = buildXrayConfig(
            protocol      = "trojan",
            address       = address,
            port          = port,
            trojanPassword = password,
            network       = network,
            tlsType       = "tls",
            path          = path,
            host          = host,
            sni           = sni
        )
        return ParseResult(name, config)
    }

    // ── Xray JSON builder ─────────────────────────────────────────────────────

    @Suppress("LongParameterList")
    private fun buildXrayConfig(
        protocol: String,
        address: String,
        port: Int,
        vmessId: String = "",
        alterId: Int    = 0,
        vlessId: String = "",
        flow: String    = "",
        trojanPassword: String = "",
        network: String = "tcp",
        tlsType: String = "none",
        path: String    = "",
        host: String    = "",
        sni: String     = ""
    ): String {

        val outboundSettings = when (protocol) {
            "vmess" -> """
                {
                  "vnext": [{
                    "address": "$address",
                    "port": $port,
                    "users": [{"id": "$vmessId","alterId": $alterId,"security": "auto"}]
                  }]
                }"""
            "vless" -> {
                val flowJson = if (flow.isNotBlank()) ""","flow": "$flow"""" else ""
                """
                {
                  "vnext": [{
                    "address": "$address",
                    "port": $port,
                    "users": [{"id": "$vlessId","encryption": "none"$flowJson}]
                  }]
                }"""
            }
            "trojan" -> """
                {
                  "servers": [{
                    "address": "$address",
                    "port": $port,
                    "password": "$trojanPassword"
                  }]
                }"""
            else -> throw IllegalArgumentException("Unknown protocol: $protocol")
        }

        val streamSettings = buildStreamSettings(network, tlsType, path, host, sni)

        return """
{
  "log": {"loglevel": "warning"},
  "dns": {
    "servers": ["1.1.1.1", "8.8.8.8", "https+local://cloudflare-dns.com/dns-query"]
  },
  "inbounds": [
    {
      "tag": "socks",
      "port": 10808,
      "listen": "127.0.0.1",
      "protocol": "socks",
      "sniffing": {"enabled": true,"destOverride": ["http","tls"]},
      "settings": {"auth": "noauth","udp": true}
    },
    {
      "tag": "http",
      "port": 10809,
      "listen": "127.0.0.1",
      "protocol": "http",
      "sniffing": {"enabled": true,"destOverride": ["http","tls"]}
    }
  ],
  "outbounds": [
    {
      "tag": "proxy",
      "protocol": "$protocol",
      "settings": $outboundSettings,
      "streamSettings": $streamSettings
    },
    {
      "tag": "direct",
      "protocol": "freedom"
    },
    {
      "tag": "block",
      "protocol": "blackhole"
    }
  ],
  "routing": {
    "domainStrategy": "IPIfNonMatch",
    "rules": [
      {"type": "field","ip": ["geoip:private"],"outboundTag": "direct"},
      {"type": "field","domain": ["geosite:cn"],"outboundTag": "direct"}
    ]
  }
}
        """.trimIndent()
    }

    private fun buildStreamSettings(
        network: String,
        tlsType: String,
        path: String,
        host: String,
        sni: String
    ): String {
        val transportSettings = when (network) {
            "ws" -> {
                val headers = if (host.isNotBlank()) ""","headers": {"Host": "$host"}""" else ""
                """
                "wsSettings": {
                  "path": "${path.ifBlank { "/" }}"
                  $headers
                }"""
            }
            "grpc" -> """
                "grpcSettings": {
                  "serviceName": "${path.ifBlank { "GrpcService" }}"
                }"""
            "http", "h2" -> """
                "httpSettings": {
                  "host": ["$host"],
                  "path": "${path.ifBlank { "/" }}"
                }"""
            else -> ""
        }

        val tlsSettings = when (tlsType) {
            "tls" -> """
                ,"tlsSettings": {
                  "serverName": "${sni.ifBlank { host }}",
                  "allowInsecure": false
                }"""
            "xtls" -> """
                ,"xtlsSettings": {
                  "serverName": "${sni.ifBlank { host }}",
                  "allowInsecure": false
                }"""
            "reality" -> """
                ,"realitySettings": {
                  "serverName": "${sni.ifBlank { host }}"
                }"""
            else -> ""
        }

        return """
        {
          "network": "$network"
          ${if (transportSettings.isNotBlank()) ",$transportSettings" else ""}
          ,"security": "$tlsType"
          $tlsSettings
        }"""
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split("&").mapNotNull { param ->
            val idx = param.indexOf('=')
            if (idx < 0) null else param.substring(0, idx) to param.substring(idx + 1)
        }.toMap()
    }
}
