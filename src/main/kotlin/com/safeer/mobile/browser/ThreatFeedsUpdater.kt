package com.safeer.mobile.browser

import android.content.Context
import com.safeer.threatfeed.PlainListSource
import com.safeer.threatfeed.ThreatListAgent
import java.io.File

/**
 * 🔄 ThreatFeedsUpdater – agent za sezname nevarnih strani (ThreatFox, URLhaus, Phishing Army).
 *
 * Ob vsakem zagonu brskalnika:
 *  1. takoj v ozadju (nizka prioriteta) naloži sezname, shranjene ob prejšnjem zagonu (preverjeni s SHA-256),
 *     zato je zaščita popolna že nekaj trenutkov po zagonu,
 *  2. približno 12 sekund po zagonu, ko se prva stran že nalaga, preveri, ali so na voljo novi seznami
 *     (pogojni prenos: nespremenjen seznam je ena majhna zahteva), in jih zamenja brez prekinitve,
 *  3. med delovanjem preverja vsakih 6 ur.
 * Zagon in nalaganje strani s tem nista upočasnjena. Ob napaki ostanejo v uporabi obstoječi seznami.
 */
object ThreatFeedsUpdater {

    private val SOURCES = listOf(
        PlainListSource(
            id = "threatfox", name = "abuse.ch ThreatFox IOC", url = "https://threatfox.abuse.ch/downloads/hostfile/",
            category = "Botnet C2 & Malware IOC", marker = "threatfox",
        ),
        PlainListSource(
            id = "urlhaus", name = "abuse.ch URLhaus", url = "https://urlhaus.abuse.ch/downloads/hostfile/",
            category = "Zlonamerna koda (Malware)", marker = "urlhaus",
        ),
        PlainListSource(
            id = "phishing-army", name = "Phishing Army Extended",
            url = "https://phishing.army/download/phishing_army_blocklist_extended.txt",
            category = "Spletno ribarjenje (Phishing)", marker = "phishing",
        ),
        PlainListSource(
            id = "hagezi-tif", name = "HaGeZi Threat Intelligence Feeds (mini)",
            url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/tif.mini-onlydomains.txt",
            category = "Nevarne strani (grožnje, ribarjenje, prevare)", marker = "hagezi",
        ),
        PlainListSource(
            id = "hagezi-fake", name = "HaGeZi Fake (lažne trgovine in prevare)",
            url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/fake-onlydomains.txt",
            category = "Lažne trgovine in prevare", marker = "hagezi",
        ),
        PlainListSource(
            id = "si-cert", name = "SI-CERT phishing domene (Slovenija)",
            url = "https://www.cert.si/misp/rpz/last.txt",
            category = "Spletno ribarjenje (Phishing) – potrdil SI-CERT", marker = "", csv = true,
        ),
    )

    @Volatile
    private var agent: ThreatListAgent? = null

    @Volatile
    var ruleCount: Int = 0
        private set

    /** Zažene agenta (enkrat na proces). Vrne takoj; vse delo poteka v ozadju. */
    @Synchronized
    fun start(context: Context) {
        if (agent != null) return
        val listAgent = ThreatListAgent(File(context.applicationContext.filesDir, "threat-lists"), SOURCES) { lists ->
            ruleCount = ThreatBlockEngine.rebuildFromLists(lists)
            android.util.Log.i("SafeerSecurity", "Seznami groženj v uporabi: ${lists.joinToString { "${it.source.name} (${it.entries.size})" }}")
        }
        agent = listAgent
        listAgent.start()
    }

    /** Takojšnje preverjanje (gumb "Posodobi sezname"); [onComplete] dobi število pravil v uporabi. */
    fun updateFeedsAsync(context: Context, force: Boolean = true, onComplete: ((totalRules: Int) -> Unit)? = null) {
        start(context)
        val requested = agent?.requestUpdate { onComplete?.invoke(ruleCount) } ?: false
        if (!requested) onComplete?.invoke(ruleCount)
    }

    fun statusLine(): String {
        val lists = agent?.lists.orEmpty()
        if (lists.isEmpty()) return "Seznami groženj: prvi prenos poteka v ozadju"
        return "Seznami groženj: ${lists.sumOf { it.entries.size }} pravil (${lists.size}/${SOURCES.size} virov)"
    }
}
