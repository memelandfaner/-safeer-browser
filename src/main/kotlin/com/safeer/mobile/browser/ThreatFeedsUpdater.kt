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
            id = "easylist", name = "EasyList (pravila za oglase)", url = "https://easylist.to/easylist/easylist.txt",
            category = "Oglasi (EasyList)", marker = "easylist", minEntries = 1000, raw = true,
        ),
        PlainListSource(
            id = "si-cert", name = "SI-CERT phishing domene (Slovenija)",
            url = "https://www.cert.si/misp/rpz/last.txt",
            category = "Spletno ribarjenje (Phishing) – potrdil SI-CERT", marker = "", headerless = true,
        ),
    )

    @Volatile
    private var agent: ThreatListAgent? = null

    @Volatile
    var ruleCount: Int = 0
        private set

    /** Število prevedenih pravil EasyList v uporabi. */
    @Volatile
    var filterRuleCount: Int = 0
        private set

    /** Zažene agenta (enkrat na proces). Vrne takoj; vse delo poteka v ozadju. */
    @Synchronized
    fun start(context: Context) {
        if (agent != null) return
        val listAgent = ThreatListAgent(File(context.applicationContext.filesDir, "threat-lists"), SOURCES) { lists ->
            ruleCount = ThreatBlockEngine.rebuildFromLists(lists.filter { !it.source.raw })
            filterRuleCount = AdBlockEngine.installFilterLists(lists.filter { it.source.raw })
            android.util.Log.i("SafeerSecurity", "Seznami v uporabi: ${lists.joinToString { "${it.source.name} (${it.entries.size})" }}; pravila EasyList: $filterRuleCount; pravila za skrivanje: ${AdBlockEngine.cosmeticRuleCount}")
        }
        agent = listAgent
        listAgent.start()
    }

    data class UpdateResult(val totalRules: Int, val failedSources: Int, val error: String) {
        val successful: Boolean get() = failedSources == 0 && error.isEmpty()
    }

    /** Reports partial failure while keeping every previously valid list in use. */
    fun updateFeedsAsync(context: Context, onComplete: ((UpdateResult) -> Unit)? = null) {
        start(context)
        val current = agent
        val requested = current?.requestUpdate {
            val failed = current.statuses.count { it.error.isNotEmpty() || it.lastSuccessEpochSeconds == 0L }
            onComplete?.invoke(UpdateResult(ruleCount, failed, current.lastError))
        } ?: false
        if (!requested) onComplete?.invoke(UpdateResult(ruleCount, SOURCES.size, "Preverjanja ni mogoče zagnati"))
    }

    fun statusLine(): String {
        val lists = agent?.lists.orEmpty()
        val formatter = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
        val now = System.currentTimeMillis() / 1000
        val statuses = agent?.statuses.orEmpty().associateBy { it.sourceId }
        val details = SOURCES.joinToString("\n\n") { source ->
            val status = statuses[source.id]
            val success = status?.lastSuccessEpochSeconds ?: 0
            val last = if (success > 0) formatter.format(java.util.Date(success * 1000)) else "še nikoli"
            val state = when {
                !status?.error.isNullOrEmpty() -> "Zadnji poskus ni uspel: ${status?.error}"
                success == 0L -> "Čaka na prvi uspešen prenos"
                now - success > 24 * 3600 -> "Seznam ni bil uspešno preverjen več kot 24 ur"
                else -> "Preverjeno"
            }
            val count = lists.find { it.source.id == source.id }?.entries?.size ?: 0
            "${source.name}: $count pravil\nZadnje uspešno preverjanje: $last\n$state"
        }
        val running = if (agent?.isRefreshing == true) "\nPreverjanje poteka …" else ""
        return "Seznami: ${lists.size}/${SOURCES.size} virov; EasyList: $filterRuleCount pravil, ${AdBlockEngine.cosmeticRuleCount} za skrivanje$running\n\n$details"
    }
}
