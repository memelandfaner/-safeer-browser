package com.example.safeerbrowser

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 🔄 ThreatFeedsUpdater
 * Avtomatiziran prenos in posodabljanje varnostnih seznamov s preverjanjem SHA-256 integritete.
 */
object ThreatFeedsUpdater {

    data class ThreatFeed(
        val name: String,
        val url: String,
        val category: String,
        val expectedSha256: String? = null // Če je naveden, preveri točen hash
    )

    private val FEEDS = listOf(
        ThreatFeed(
            name = "abuse.ch Feodo Tracker",
            url = "https://feodotracker.abuse.ch/downloads/ipblocklist.txt",
            category = "Botnet C2 Server"
        ),
        ThreatFeed(
            name = "abuse.ch URLhaus",
            url = "https://urlhaus.abuse.ch/downloads/hostfile/",
            category = "Zlonamerna koda (Malware)"
        ),
        ThreatFeed(
            name = "Phishing Army Extended",
            url = "https://phishing.army/download/phishing_army_blocklist_extended.txt",
            category = "Spletno ribarjenje (Phishing)"
        )
    )

    /**
     * Izračuna SHA-256 kontrolno vsoto vsebine.
     */
    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Prenese in posodobi varnostne sezname v ozadju.
     */
    fun updateFeedsAsync(context: Context, onComplete: ((totalAdded: Int) -> Unit)? = null) {
        Thread {
            var totalAdded = 0
            for (feed in FEEDS) {
                try {
                    val conn = (URL(feed.url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 12000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "SafeerBrowser-SecurityShield/1.0")
                    }

                    if (conn.responseCode == 200) {
                        val bytes = conn.inputStream.readBytes()
                        
                        // Preveri SHA-256, če je zahtevan
                        if (feed.expectedSha256 != null) {
                            val computed = computeSha256(bytes)
                            if (!computed.equals(feed.expectedSha256, ignoreCase = true)) {
                                continue // Zavrni poškodovan ali spremenjen seznam
                            }
                        }

                        val reader = BufferedReader(InputStreamReader(bytes.inputStream(), Charsets.UTF_8))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val l = line?.trim() ?: continue
                            if (l.isEmpty() || l.startsWith("#") || l.startsWith(";")) continue

                            // Razčleni gostitelja (npr. "127.0.0.1 badhost.com" ali "badhost.com")
                            val parts = l.split("\\s+".toRegex())
                            val domain = if (parts.size >= 2 && (parts[0] == "127.0.0.1" || parts[0] == "0.0.0.0")) {
                                parts[1]
                            } else {
                                parts[0]
                            }

                            if (domain.contains(".") && !domain.startsWith("localhost") && !domain.startsWith("127.0.0.1")) {
                                ThreatBlockEngine.addThreat(domain, feed.category, feed.name)
                                totalAdded++
                            }
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {}
            }
            onComplete?.invoke(totalAdded)
        }.start()
    }
}
