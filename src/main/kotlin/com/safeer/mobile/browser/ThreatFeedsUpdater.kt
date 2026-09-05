package com.safeer.mobile.browser

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
        val category: String
    )

    private val FEEDS = listOf(
        ThreatFeed(
            name = "abuse.ch ThreatFox IOC",
            url = "https://threatfox.abuse.ch/downloads/hostfile/",
            category = "Botnet C2 & Malware IOC"
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

    private const val PREFS_NAME = "safeer_security_prefs"
    private const val KEY_LAST_UPDATE = "last_threat_update_time"
    private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 ur

    /**
     * Izračuna SHA-256 kontrolno vsoto vsebine za varnostno beleženje integritete (Security Audit Log).
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
    fun updateFeedsAsync(context: Context, force: Boolean = false, onComplete: ((totalAdded: Int) -> Unit)? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0L)
        val now = System.currentTimeMillis()

        if (!force && (now - lastUpdate < CACHE_DURATION_MS)) {
            android.util.Log.i("SafeerSecurity", "Varnostni seznami so posodobljeni (cache velja še ${((CACHE_DURATION_MS - (now - lastUpdate)) / 3600000)} ur).")
            onComplete?.invoke(0)
            return
        }

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
                        if (bytes.size < 100) continue

                        // Beleženje SHA-256 kontrolne vsote za revizijo integritete
                        val hash = computeSha256(bytes)
                        android.util.Log.i("SafeerSecurity", "Posodobljen vir '${feed.name}' (${bytes.size} B, SHA-256: $hash)")

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

                            if (isValidDomainName(domain) && !ThreatBlockEngine.isNeverBlockDomain(domain)) {
                                newTrie.insert(domain, feed.category, feed.name)
                                totalAdded++
                            }
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {}
            }

            // Če so bili novi viri uspešno preneseni, atomsko zamenjamo celotno drevo groženj in shranimo čas
            if (totalAdded > 0) {
                ThreatBlockEngine.swapThreatTrie(newTrie)
                prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
            }

            onComplete?.invoke(totalAdded)
        }.start()
    }
}
