package com.vpnpro.vpn

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Fetches free VMess/VLESS/Trojan configs from public repositories.
 * Sources are updated daily by their maintainers.
 */
object FreeConfigFetcher {

    private const val TAG = "FreeConfigFetcher"

    data class FreeConfig(
        val link: String,
        val name: String,
        val source: String
    )

    // ── Public sources (updated daily) ────────────────────────────────────
    private val SOURCES = listOf(
        // barry-far V2ray Configs — very reliable
        "https://raw.githubusercontent.com/barry-far/V2ray-Configs/main/All_Configs_Sub.txt",
        "https://raw.githubusercontent.com/barry-far/V2ray-Configs/main/Splitted-By-Protocol/vless.txt",
        "https://raw.githubusercontent.com/barry-far/V2ray-Configs/main/Splitted-By-Protocol/vmess.txt",
        "https://raw.githubusercontent.com/barry-far/V2ray-Configs/main/Splitted-By-Protocol/trojan.txt",
        // mahdibland aggregator
        "https://raw.githubusercontent.com/mahdibland/V2RayAggregator/master/Eternity.txt",
        // Pawdroid aggregator
        "https://raw.githubusercontent.com/Pawdroid/Free-servers/main/sub",
        // yebekhe TelegramV2rayCollector
        "https://raw.githubusercontent.com/yebekhe/TelegramV2rayCollector/main/sub/base64/mix",
        // ermaozi
        "https://raw.githubusercontent.com/ermaozi/get_subscribe/main/subscribe/v2ray.txt",
        // freefq
        "https://raw.githubusercontent.com/freefq/free/master/v2",
    )

    // ── Moroccan carrier Bug Host presets ─────────────────────────────────
    // These are zero-rated (free) endpoints known to work on MA carriers.
    // Users can paste their own VMess/VLESS link and apply these bug hosts.
    val MOROCCAN_BUG_HOSTS = listOf(
        BugHostPreset(
            carrier = "Inwi 🇲🇦",
            host    = "graph.facebook.com",
            port    = 443,
            tls     = true,
            description = "Facebook CDN — zero-rated on Inwi"
        ),
        BugHostPreset(
            carrier = "Inwi 🇲🇦",
            host    = "web.facebook.com",
            port    = 80,
            tls     = false,
            description = "Facebook Web — HTTP mode on Inwi"
        ),
        BugHostPreset(
            carrier = "Maroc Telecom (IAM) 🇲🇦",
            host    = "graph.facebook.com",
            port    = 443,
            tls     = true,
            description = "Facebook CDN — zero-rated on IAM"
        ),
        BugHostPreset(
            carrier = "Maroc Telecom (IAM) 🇲🇦",
            host    = "free.facebook.com",
            port    = 443,
            tls     = true,
            description = "Facebook Free Basics — IAM"
        ),
        BugHostPreset(
            carrier = "Orange Maroc 🇲🇦",
            host    = "graph.facebook.com",
            port    = 443,
            tls     = true,
            description = "Facebook CDN — zero-rated on Orange"
        ),
        BugHostPreset(
            carrier = "Orange Maroc 🇲🇦",
            host    = "web.facebook.com",
            port    = 443,
            tls     = true,
            description = "Facebook Web — Orange Maroc"
        ),
        BugHostPreset(
            carrier = "Tous les opérateurs 🇲🇦",
            host    = "zero.facebook.com",
            port    = 443,
            tls     = true,
            description = "Facebook Zero — fonctionne sur tous les opérateurs"
        ),
        BugHostPreset(
            carrier = "Tous les opérateurs 🇲🇦",
            host    = "internet.org",
            port    = 443,
            tls     = true,
            description = "Internet.org (Facebook) — tous opérateurs"
        ),
    )

    data class BugHostPreset(
        val carrier: String,
        val host: String,
        val port: Int,
        val tls: Boolean,
        val description: String
    )

    /**
     * Fetch free configs from all sources and return parsed valid ones.
     * Runs on IO dispatcher. Returns up to [maxConfigs] valid configs.
     */
    suspend fun fetchAll(maxConfigs: Int = 200): List<FreeConfig> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FreeConfig>()
        for (sourceUrl in SOURCES) {
            try {
                val raw = fetchUrl(sourceUrl)
                val parsed = extractConfigs(raw, sourceUrl)
                results.addAll(parsed)
                Log.i(TAG, "Source $sourceUrl → ${parsed.size} configs")
                if (results.size >= maxConfigs) break
            } catch (e: Exception) {
                Log.w(TAG, "Source failed: $sourceUrl — ${e.message}")
            }
        }
        Log.i(TAG, "Total fetched: ${results.size} configs")
        results.take(maxConfigs)
    }

    private fun fetchUrl(url: String): String {
        val conn = URL(url).openConnection()
        conn.connectTimeout = 8_000
        conn.readTimeout    = 12_000
        conn.setRequestProperty("User-Agent", "VPNPro/2.0")
        return conn.getInputStream().bufferedReader().readText()
    }

    private fun extractConfigs(raw: String, source: String): List<FreeConfig> {
        // Try base64 decode first (many aggregators encode their lists)
        val text = try {
            val decoded = android.util.Base64.decode(
                raw.trim(), android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP
            )
            String(decoded)
        } catch (_: Exception) {
            raw
        }

        val results = mutableListOf<FreeConfig>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("vmess://") ||
                trimmed.startsWith("vless://") ||
                trimmed.startsWith("trojan://")
            ) {
                try {
                    val parsed = ConfigParser.parse(trimmed)
                    results.add(FreeConfig(
                        link   = trimmed,
                        name   = parsed.name.take(40),
                        source = extractSourceName(source)
                    ))
                } catch (_: Exception) {
                    // skip invalid
                }
            }
        }
        return results
    }

    private fun extractSourceName(url: String): String = when {
        url.contains("barry-far")     -> "barry-far"
        url.contains("mahdibland")    -> "mahdibland"
        url.contains("Pawdroid")      -> "Pawdroid"
        url.contains("yebekhe")       -> "yebekhe"
        url.contains("ermaozi")       -> "ermaozi"
        url.contains("freefq")        -> "freefq"
        else -> "free"
    }
}
