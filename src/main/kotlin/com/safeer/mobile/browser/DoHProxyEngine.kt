package com.safeer.mobile.browser

import android.content.Context
import android.content.Intent
import android.net.ProxyInfo
import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
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
        "google" to DoHProvider("google", "Google Public DoH (8.8.8.8)", "https://dns.google/resolve")
    )

    private var currentServer: LocalDoHServer? = null
    private val isRunning = AtomicBoolean(false)
    private var activeProviderId = "quad9"

    // --- 1. DoH Razreševalnik z lokalnim predpomnilnikom ---
    class DoHResolver(var providerUrl: String) {
        private val cache = ConcurrentHashMap<String, Pair<String, Long>>()

        fun resolve(hostname: String, timeoutMs: Int = 3500): String? {
            val h = hostname.lowercase().trim()
            if (h.isEmpty()) return null

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
                cache[h] = Pair(resolvedIp, now + 300_000L) // 5 minut TTL
                return resolvedIp
            }

            // Varen fallback na sistemski DNS
            return try {
                InetAddress.getByName(h).hostAddress
            } catch (_: Exception) {
                null
            }
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
            // 1. IETF RFC 8484 Binarni POST (AdGuard, Quad9, Cloudflare)
            try {
                val wireQuery = buildDnsWireQuery(hostname)
                val conn = URL(providerUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/dns-message")
                conn.setRequestProperty("Accept", "application/dns-message")
                conn.setRequestProperty("User-Agent", "Safeer-Mobile-DoH/1.0")
                conn.outputStream.use { it.write(wireQuery) }

                if (conn.responseCode == 200) {
                    val respBytes = conn.inputStream.use { it.readBytes() }
                    val ip = parseDnsWireResponse(respBytes)
                    if (!ip.isNullOrEmpty()) return ip
                }
            } catch (_: Exception) {}

            // 2. Varen nadomestni poizvedovalnik prek JSON DoH API (Google, Cloudflare)
            return try {
                val encodedHost = URLEncoder.encode(hostname, "UTF-8")
                val requestUrl = if (providerUrl.contains("?")) {
                    "$providerUrl&name=$encodedHost&type=A"
                } else {
                    "$providerUrl?name=$encodedHost&type=A"
                }
                val conn = URL(requestUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.setRequestProperty("Accept", "application/dns-json")
                conn.setRequestProperty("User-Agent", "Safeer-Mobile-DoH/1.0")

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val answers = json.optJSONArray("Answer")
                    if (answers != null) {
                        for (i in 0 until answers.length()) {
                            val ans = answers.getJSONObject(i)
                            if (ans.optInt("type") == 1) { // Type 1 = A zapis
                                val ip = ans.optString("data", "")
                                if (ip.isNotEmpty()) return ip
                            }
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.d(TAG, "DoH poizvedba za $hostname ni uspela: ${e.message}")
                null
            }
        }
    }

    // --- 2. Lokalni CONNECT posredniški strežnik ---
    class LocalDoHServer(val resolver: DoHResolver) {
        private var serverSocket: ServerSocket? = null
        var actualPort: Int = 0
            private set
        val active = AtomicBoolean(false)

        fun start(): Int {
            if (active.get()) return actualPort
            serverSocket = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
            actualPort = serverSocket!!.localPort
            active.set(true)

            Thread({
                while (active.get()) {
                    try {
                        val clientSock = serverSocket?.accept() ?: break
                        Thread({ handleClient(clientSock) }, "SafeerDoHWorker").start()
                    } catch (_: Exception) {
                        if (!active.get()) break
                    }
                }
            }, "SafeerDoHAcceptor").start()

            Log.i(TAG, "Lokalni DoH strežnik posluša na 127.0.0.1:$actualPort")
            return actualPort
        }

        fun stop() {
            active.set(false)
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
            serverSocket = null
        }

        private fun handleClient(clientSock: Socket) {
            var remoteSock: Socket? = null
            try {
                clientSock.soTimeout = 10000
                val inStream = clientSock.getInputStream()
                val outStream = clientSock.getOutputStream()

                val buffer = ByteArray(4096)
                var readCount = 0
                val reqBuilder = StringBuilder()

                while (readCount < 8192) {
                    val r = inStream.read(buffer)
                    if (r <= 0) break
                    readCount += r
                    reqBuilder.append(String(buffer, 0, r, Charsets.ISO_8859_1))
                    if (reqBuilder.contains("\r\n\r\n")) break
                }

                val reqStr = reqBuilder.toString()
                if (reqStr.isEmpty()) {
                    clientSock.close()
                    return
                }

                val firstLine = reqStr.substringBefore("\r\n")
                val parts = firstLine.split(" ")
                if (parts.size < 2) {
                    clientSock.close()
                    return
                }

                val method = parts[0].uppercase()
                val target = parts[1]

                if (method == "CONNECT") {
                    val host: String
                    val port: Int
                    if (target.contains(":")) {
                        host = target.substringBefore(":")
                        port = target.substringAfter(":").toIntOrNull() ?: 443
                    } else {
                        host = target
                        port = 443
                    }

                    val resolvedIp = resolver.resolve(host) ?: host
                    remoteSock = Socket(resolvedIp, port)
                    remoteSock.soTimeout = 0
                    clientSock.soTimeout = 0

                    outStream.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                    outStream.flush()

                    pipeSockets(clientSock, remoteSock)
                } else {
                    // Standardni HTTP zahtevek
                    val host = if (target.startsWith("http://")) {
                        val withoutScheme = target.substring(7)
                        withoutScheme.substringBefore("/").substringBefore(":")
                    } else {
                        target.substringBefore("/").substringBefore(":")
                    }
                    val port = 80
                    val resolvedIp = resolver.resolve(host) ?: host

                    remoteSock = Socket(resolvedIp, port)
                    remoteSock.soTimeout = 0
                    clientSock.soTimeout = 0

                    remoteSock.getOutputStream().write(reqStr.toByteArray(Charsets.ISO_8859_1))
                    remoteSock.getOutputStream().flush()

                    pipeSockets(clientSock, remoteSock)
                }
            } catch (_: Exception) {
            } finally {
                try { clientSock.close() } catch (_: Exception) {}
                try { remoteSock?.close() } catch (_: Exception) {}
            }
        }

        private fun pipeSockets(s1: Socket, s2: Socket) {
            val t1 = Thread({ copyStream(s1.getInputStream(), s2.getOutputStream()) }, "Pipe-1")
            val t2 = Thread({ copyStream(s2.getInputStream(), s1.getOutputStream()) }, "Pipe-2")
            t1.start()
            t2.start()
            try { t1.join() } catch (_: Exception) {}
            try { t2.join() } catch (_: Exception) {}
        }

        private fun copyStream(inStream: InputStream, outStream: OutputStream) {
            val buf = ByteArray(32768)
            try {
                while (true) {
                    val r = inStream.read(buf)
                    if (r <= 0) break
                    outStream.write(buf, 0, r)
                    outStream.flush()
                }
            } catch (_: Exception) {}
        }
    }

    // --- 3. Posodobitev proxy nastavitev za WebView ---
    private fun applyProxyToSystemAndChromium(context: Context, host: String, port: Int) {
        try {
            System.setProperty("http.proxyHost", host)
            System.setProperty("http.proxyPort", port.toString())
            System.setProperty("https.proxyHost", host)
            System.setProperty("https.proxyPort", port.toString())

            // Pošlji uradni Android sistemski intent za spremembo proxyja
            val intent = Intent(android.net.Proxy.PROXY_CHANGE_ACTION)
            val proxyInfo = ProxyInfo.buildDirectProxy(host, port)
            intent.putExtra("android.intent.extra.PROXY_INFO", proxyInfo)
            context.sendBroadcast(intent)

            // Posodobi Chromium WebView neposredno prek internega ProxyChangeListener poslušalca
            notifyChromiumProxyChange(context, intent)
            Log.i(TAG, "Proxy uspešno uveljavljen: $host:$port")
        } catch (e: Exception) {
            Log.w(TAG, "Opozorilo pri uveljavitvi proxyja: ${e.message}")
        }
    }

    private fun clearProxyFromSystemAndChromium(context: Context) {
        try {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")

            val intent = Intent(android.net.Proxy.PROXY_CHANGE_ACTION)
            context.sendBroadcast(intent)
            notifyChromiumProxyChange(context, intent)
            Log.i(TAG, "Proxy odstranjen, vzpostavljena direktna povezava.")
        } catch (e: Exception) {
            Log.w(TAG, "Opozorilo pri odstranjevanju proxyja: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun notifyChromiumProxyChange(context: Context, intent: Intent) {
        try {
            val appClass = Class.forName("android.app.Application")
            val mLoadedApkField = appClass.getDeclaredField("mLoadedApk")
            mLoadedApkField.isAccessible = true
            val mLoadedApk = mLoadedApkField.get(context.applicationContext) ?: return

            val loadedApkClass = Class.forName("android.app.LoadedApk")
            val mReceiversField = loadedApkClass.getDeclaredField("mReceivers")
            mReceiversField.isAccessible = true
            val receivers = mReceiversField.get(mLoadedApk) as? Map<*, *> ?: return

            for (receiverMap in receivers.values) {
                val map = receiverMap as? Map<*, *> ?: continue
                for (receiver in map.keys) {
                    if (receiver != null && receiver.javaClass.name.contains("ProxyChangeListener")) {
                        val onReceiveMethod = receiver.javaClass.getDeclaredMethod(
                            "onReceive",
                            Context::class.java,
                            Intent::class.java
                        )
                        onReceiveMethod.isAccessible = true
                        onReceiveMethod.invoke(receiver, context, intent)
                    }
                }
            }
        } catch (_: Exception) {
            // Tiho obdelaj, saj je sistemski broadcast že poslan
        }
    }

    // --- 4. Javne metode za upravljanje stanja ---
    @Synchronized
    fun applySettings(context: Context) {
        val proxyMode = PreferencesManager.getSecureProxyMode(context)
        val dohEnabled = PreferencesManager.isDohEnabled(context)
        val dohProviderId = PreferencesManager.getDohProvider(context)

        when (proxyMode) {
            "tor" -> {
                // Orbot privzeti HTTP/SOCKS porti (8118 za HTTP, 9050 za SOCKS)
                stopServer()
                applyProxyToSystemAndChromium(context, "127.0.0.1", 8118)
                Log.i(TAG, "Aktiviran Tor tunel (Orbot) prek 127.0.0.1:8118")
            }
            "custom" -> {
                stopServer()
                val customUrl = PreferencesManager.getSecureProxyUrl(context)
                val host = customUrl.substringBefore(":").replace("http://", "").replace("https://", "").replace("socks5://", "")
                val port = customUrl.substringAfter(":").toIntOrNull() ?: 8080
                if (host.isNotEmpty() && port > 0) {
                    applyProxyToSystemAndChromium(context, host, port)
                } else {
                    clearProxyFromSystemAndChromium(context)
                }
            }
            else -> {
                if (dohEnabled && dohProviderId != "disabled") {
                    val customUrl = PreferencesManager.getCustomDohUrl(context)
                    val effectiveUrl = if (dohProviderId == "custom" && customUrl.isNotBlank()) {
                        customUrl
                    } else {
                        (PROVIDERS[dohProviderId] ?: PROVIDERS["quad9"]!!).url
                    }
                    activeProviderId = dohProviderId

                    val resolver = DoHResolver(effectiveUrl)
                    if (currentServer == null || !currentServer!!.active.get()) {
                        currentServer = LocalDoHServer(resolver)
                        val port = currentServer!!.start()
                        applyProxyToSystemAndChromium(context, "127.0.0.1", port)
                    } else {
                        currentServer?.resolver?.providerUrl = effectiveUrl
                    }
                } else {
                    stopServer()
                    clearProxyFromSystemAndChromium(context)
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
