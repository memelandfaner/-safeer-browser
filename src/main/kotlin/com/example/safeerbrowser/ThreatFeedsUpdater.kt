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

    private fun isValidDomainName(name: String): Boolean {
        val n = name.trim().lowercase()
        if (!n.contains(".") || n.startsWith(".") || n.endsWith(".")) return false
        if (n.startsWith("localhost") || n.startsWith("127.0.0.1")) return false
        val parts = n.split(".")
        if (parts.size < 2) return false
        // Če so vsi segmenti številčni, gre za IPv4 in ne veljavno domensko ime
        if (parts.all { it.isNotEmpty() && it.all { c -> c.isDigit() } }) return false
        return n.all { it.isLetterOrDigit() || it == '.' || it == '-' }
    }

    /**
     * Prenese in posodobi varnostne sezname v ozadju z atomsko zamenjavo drevesa (Atomic Trie Swap).
     */
    fun updateFeedsAsync(context: Context, onComplete: ((totalAdded: Int) -> Unit)? = null) {
        Thread {
            var totalAdded = 0
            val newTrie = DomainSuffixTrie()
            ThreatBlockEngine.loadSeedThreatDatabase(newTrie)

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

                            if (isValidDomainName(domain)) {
                                newTrie.insert(domain, feed.category, feed.name)
                                totalAdded++
                            }
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {}
            }

            // Če so bili novi viri uspešno preneseni, atomsko zamenjamo celotno drevo groženj
            if (totalAdded > 0) {
                ThreatBlockEngine.swapThreatTrie(newTrie)
            }

            onComplete?.invoke(totalAdded)
        }.start()
    }
}
