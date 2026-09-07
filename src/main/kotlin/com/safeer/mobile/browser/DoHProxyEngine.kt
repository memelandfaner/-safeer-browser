package com.safeer.mobile.browser

import android.content.Context
import android.util.Log
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 🛡️ DoHProxyEngine
 * Kriptografski pogon za DNS-over-HTTPS (DoH) in šifrirano usmerjanje prometa (Tor / Proxy) v Safeer Mobile.
 * Preprečuje cenzuro, DNS prisluškovanje in blokade s strani lokalnih internetnih ponudnikov brez potrebe po root pravicah.
 */
object DoHProxyEngine {

    private const val TAG = "SafeerDoH"

    data class DoHProvider(val id: String, val name: String, val url: String)

    val PROVIDERS = mapOf(
        "quad9" to DoHProvider("quad9", "Quad9 Secure DoH (9.9.9.9)", "https://dns.quad9.net/dns-query"),
        "adguard" to DoHProvider("adguard", "AdGuard DNS (dns.adguard-dns.com)", "https://dns.adguard-dns.com/dns-query"),
        "cloudflare" to DoHProvider("cloudflare", "Cloudflare DoH (1.1.1.1)", "https://1.1.1.1/dns-query"),
        "google" to DoHProvider("google", "Google Public DoH (8.8.8.8)", "https://dns.google/dns-query")
    )

    private var currentServer: LocalDoHServer? = null
    private val isRunning = AtomicBoolean(false)
    private var activeProviderId = "cloudflare"

    // --- 1. DoH Razreševalnik z lokalnim predpomnilnikom ---
    class DoHResolver(providerUrl: String, private val transport: (String, ByteArray, Int) -> ByteArray? = Http2DnsTransport::query) {
        @Volatile var providerUrl: String = providerUrl
            set(value) { if (field != value) { field = value; cache.clear() } }
        private val lookupLocks = Array(32) { Any() }
        fun resolve(hostname: String, timeoutMs: Int = 1500): String? =
            synchronized(lookupLocks[(hostname.hashCode() and Int.MAX_VALUE) % lookupLocks.size]) {
                resolveOnce(hostname, timeoutMs)
            }
        private val cache = ConcurrentHashMap<String, Pair<String?, Long>>()

        private fun resolveOnce(hostname: String, timeoutMs: Int): String? {
            val h = hostname.lowercase().trim()
            if (h.isEmpty()) return null
            if (h.contains(":")) return try { InetAddress.getByName(h).hostAddress } catch (_: Exception) { null }
            if (h == "localhost" || h == "127.0.0.1") return "127.0.0.1"
            if (h.endsWith(".local") || h.endsWith(".lan") || h.endsWith(".internal")) {
                return try { InetAddress.getByName(h).hostAddress } catch (_: Throwable) { null }
            }

            // Če gre že za IPv4 naslov
            val parts = h.split(".")
            if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
                return h
            }

            val now = System.currentTimeMillis()
            val cached = cache[h]
            if (cached != null && now < cached.second) {
                return cached.first
            }

            // Pošlji poizvedbo prek DoH
            val resolvedIp = queryDoH(h, timeoutMs)
            if (resolvedIp != null) {
                cache[h] = Pair(resolvedIp, now + 60_000L) // Short cache limits stale DNS answers.
                return resolvedIp
            }

            // An enabled encrypted resolver must not silently leak public queries to ISP DNS
            // or bypass a provider's blocked/NXDOMAIN answer. Briefly cache failures.
            cache[h] = Pair(null, now + 5_000L)
            return null
        }


        private fun buildDnsWireQuery(hostname: String): ByteArray {
            val header = byteArrayOf(
                0x00, 0x01, // ID
                0x01, 0x00, // Flags (RD = 1)
                0x00, 0x01, // QDCOUNT = 1
                0x00, 0x00, // ANCOUNT = 0
                0x00, 0x00, // NSCOUNT = 0
                0x00, 0x00  // ARCOUNT = 0
            )
            val parts = hostname.trim('.').split(".")
            val qname = java.io.ByteArrayOutputStream()
            for (part in parts) {
                val bytes = part.toByteArray(Charsets.US_ASCII)
                qname.write(bytes.size)
                qname.write(bytes)
            }
            qname.write(0)
            val qtypeClass = byteArrayOf(0x00, 0x01, 0x00, 0x01) // A, IN
            return header + qname.toByteArray() + qtypeClass
        }

        private fun parseDnsWireResponse(data: ByteArray): String? {
            if (data.size < 12) return null
            try {
                val qdcount = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
                val ancount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
                if (ancount <= 0) return null

                var idx = 12
                // Skip Questions
                for (i in 0 until qdcount) {
                    while (idx < data.size && data[idx] != 0.toByte()) {
                        if ((data[idx].toInt() and 0xC0) == 0xC0) {
                            idx += 2
                            break
                        }
                        idx += 1 + (data[idx].toInt() and 0xFF)
                    }
                    if (idx < data.size && data[idx] == 0.toByte()) idx += 1
                    idx += 4 // QTYPE + QCLASS
                }

                // Parse Answers
                for (i in 0 until ancount) {
                    if (idx >= data.size) break
                    if ((data[idx].toInt() and 0xC0) == 0xC0) {
                        idx += 2
                    } else {
                        while (idx < data.size && data[idx] != 0.toByte()) {
                            idx += 1 + (data[idx].toInt() and 0xFF)
                        }
                        if (idx < data.size && data[idx] == 0.toByte()) idx += 1
                    }
                    if (idx + 10 > data.size) break
                    val rtype = ((data[idx].toInt() and 0xFF) shl 8) or (data[idx + 1].toInt() and 0xFF)
                    val rdlength = ((data[idx + 8].toInt() and 0xFF) shl 8) or (data[idx + 9].toInt() and 0xFF)
                    idx += 10
                    if (rtype == 1 && rdlength == 4 && idx + 4 <= data.size) { // A record
                        val ip = "${data[idx].toInt() and 0xFF}.${data[idx + 1].toInt() and 0xFF}.${data[idx + 2].toInt() and 0xFF}.${data[idx + 3].toInt() and 0xFF}"
                        return ip
                    }
                    idx += rdlength
                }
            } catch (_: Exception) {}
            return null
        }

        private fun queryDoH(hostname: String, timeoutMs: Int): String? {
            if (!providerUrl.startsWith("https://", true)) return null
            val response = transport(providerUrl, buildDnsWireQuery(hostname), timeoutMs) ?: return null
            return parseDnsWireResponse(response)
        }
    }

    // --- 2. Lokalni CONNECT posredniški strežnik z bazenom niti in zaščito pred puščanjem ---
    class LocalDoHServer(val resolver: DoHResolver) : LocalDnsProxy({ resolver.resolve(it) })

    // Use the supported per-app WebView API. PROXY_CHANGE is a protected broadcast.
    private fun applyProxyToSystemAndChromium(context: Context, host: String, port: Int, onReady: () -> Unit) {
        try {
            check(androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.PROXY_OVERRIDE))
            val config = androidx.webkit.ProxyConfig.Builder()
                .addProxyRule("http://$host:$port")
                .addBypassRule("localhost").addBypassRule("127.0.0.1")
                .addBypassRule("*.local").addBypassRule("*.lan").build()
            androidx.webkit.ProxyController.getInstance().setProxyOverride(config,
                java.util.concurrent.Executor { android.os.Handler(android.os.Looper.getMainLooper()).post(it) },
                Runnable { Log.i(TAG, "WebView proxy active: $host:$port"); onReady() })
        } catch (e: Exception) {
            Log.e(TAG, "WebView proxy could not be enabled", e)
            android.widget.Toast.makeText(context, "Varne povezave ni mogoče vključiti. Posodobite Android System WebView ali preverite omrežne nastavitve.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun clearProxyFromSystemAndChromium(context: Context, onReady: () -> Unit) {
        // Remove properties left by older versions; never use a protected system broadcast.
        for (key in listOf("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort")) System.clearProperty(key)
        if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.PROXY_OVERRIDE)) {
            androidx.webkit.ProxyController.getInstance().clearProxyOverride(
                java.util.concurrent.Executor { android.os.Handler(android.os.Looper.getMainLooper()).post(it) }, Runnable { onReady() })
        } else onReady()
    }

    // --- 4. Javne metode za upravljanje stanja ---
    @Synchronized
    fun applySettings(context: Context, onReady: () -> Unit = {}) {
        val proxyMode = PreferencesManager.getSecureProxyMode(context)
        val dohEnabled = PreferencesManager.isDohEnabled(context)
        val dohProviderId = PreferencesManager.getDohProvider(context)

        when (proxyMode) {
            "custom" -> {
                stopServer()
                val customUrl = PreferencesManager.getSecureProxyUrl(context)
                val endpoint = try {
                    java.net.URI(if (customUrl.contains("://")) customUrl else "http://$customUrl")
                } catch (_: Exception) { null }
                if (endpoint?.scheme == "http" && !endpoint.host.isNullOrEmpty() &&
                    endpoint.port in 1..65535 && endpoint.userInfo == null) {
                    applyProxyToSystemAndChromium(context, endpoint.host, endpoint.port, onReady)
                } else {
                    // A selected privacy proxy must never silently turn into a direct connection.
                    applyProxyToSystemAndChromium(context, "127.0.0.1", 9, onReady)
                    android.widget.Toast.makeText(context,
                        "Preverite proxy: vnesite http://gostitelj:vrata. SOCKS in HTTPS proxy nista podprta.",
                        android.widget.Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                if (dohEnabled && dohProviderId != "disabled") {
                    val customUrl = PreferencesManager.getCustomDohUrl(context)
                    val effectiveUrl = if (dohProviderId == "custom" && customUrl.isNotBlank()) {
                        customUrl
                    } else {
                        (PROVIDERS[dohProviderId] ?: PROVIDERS["cloudflare"]!!).url
                    }
                    activeProviderId = dohProviderId

                    stopServer()
                    val resolver = DoHResolver(effectiveUrl)
                    currentServer = LocalDoHServer(resolver)
                    val port = currentServer!!.start()
                    applyProxyToSystemAndChromium(context, "127.0.0.1", port, onReady)
                } else {
                    stopServer()
                    clearProxyFromSystemAndChromium(context, onReady)
                }
            }
        }
    }

    @Synchronized
    fun stopServer() {
        currentServer?.stop()
        currentServer = null
        isRunning.set(false)
    }
}
