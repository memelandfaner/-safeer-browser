package com.safeer.mobile.browser

import android.net.Uri
import android.webkit.WebResourceResponse
import com.safeer.threatfeed.BankGuard
import com.safeer.threatfeed.BankVerdict
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 🛡️ ThreatBlockEngine
 * Namensko jedro za brezkompromisno blokado Botnet C2 strežnikov, zlonamerne programske opreme (Malware),
 * ribarjenja (Phishing) in indikatorjev napadov (IOC).
 * 
 * 🔒 PRAVILO O NEDOTAKLJIVOSTI: Za te grožnje NE obstaja noben video ali embed bypass!
 */
object ThreatBlockEngine {

    var isEnabled: Boolean = true

    // Statistika blokiranih groženj
    val blockedC2Count = AtomicLong(0)
    val blockedMalwareCount = AtomicLong(0)
    val blockedPhishingCount = AtomicLong(0)
    val blockedIocCount = AtomicLong(0)
    val totalBlockedThreats = AtomicLong(0)

    // Hitri Domain Suffix Trie za grožnje (podpora za atomsko zamenjavo ob posodobitvi feedov)
    @Volatile
    private var threatTrie = DomainSuffixTrie()

    /**
     * Dodatni preverjeni vir groženj: Safeer Threat Intelligence (Ed25519 podpisan seznam, glej
     * SignedThreatIntel.kt). Dopolnjuje vgrajeni seznam, nikoli ga ne nadomešča; napaka vira nikoli
     * ne prekine navigacije.
     */
    @Volatile
    var signedFeedMatcher: ((url: String, host: String) -> DomainSuffixTrie.MatchResult?)? = null

    private fun signedMatch(url: String, host: String): DomainSuffixTrie.MatchResult? {
        val matcher = signedFeedMatcher ?: return null
        return try {
            matcher(url, host)
        } catch (e: Exception) {
            null
        }
    }

    /** Vgrajeni seznam in podpisani vir; kritična kategorija (C2/malware) ima vedno prednost. */
    private fun findThreat(url: String, host: String): DomainSuffixTrie.MatchResult? {
        val local = threatTrie.findMatch(host)
        if (local != null && isCriticalThreat(local.category)) return local
        val signed = signedMatch(url, host) ?: return local
        return if (local == null || isCriticalThreat(signed.category)) signed else local
    }

    // Začasno odobrena spletna mesta (uporabnik je izrecno kliknil 'Nadaljuj na lastno odgovornost' za to sejo)
    private val sessionBypassedDomains = ConcurrentHashMap.newKeySet<String>()

    // Enokratni kriptografski žetoni za varno potrditev obvoza varnostnega opozorila
    data class PendingBypass(val domain: String, val targetUrl: String, val expiryMs: Long)
    private val pendingBypasses = ConcurrentHashMap<String, PendingBypass>()

    // Dogodek ob blokadi
    var onThreatBlocked: ((domain: String, category: String, source: String, isMainFrame: Boolean) -> Unit)? = null

    // 🛡️ Stroga minimalna bela lista domen, ki jih Threat Shield NIKOLI ne sme blokirati
    // Odstranjen je githubusercontent.com in preširoki wildcardi za preprečevanje zlorab (Malware Staging)
    private val NEVER_BLOCK_EXACT = hashSetOf(
        "github.com", "api.github.com", "microsoft.com", "apple.com",
        "fonts.googleapis.com", "fonts.gstatic.com", "ajax.googleapis.com",
        "apis.google.com", "play.google.com"
    )

    private val NEVER_BLOCK_ROOT_DOMAINS = hashSetOf(
        "google.com", "youtube.com", "googlevideo.com", "ytimg.com",
        "duckduckgo.com", "wikipedia.org", "wikimedia.org", "mozilla.org", "android.com",
        // AdGuard – legitimna varnostna programska oprema
        "adguard.com", "adguard.net", "adguard-vpn.com",
        // Hitrostni testi (speedtest.net, fast.com, nperf.com)
        "speedtest.net", "ooklaserver.net", "fast.com", "nperf.com", "nperf.net",
        // Splošne vestičke, iskalniki, novice
        "reddit.com", "redd.it", "bbc.com", "bbc.co.uk", "cnn.com", "reuters.com",
        "finance.si", "bolha.com", "ceneje.si", "mimovrste.com", "enaa.com",
        "24ur.com", "siol.net", "zurnal24.si", "delo.si", "rtvslo.si",
        "weather.com", "accuweather.com", "wetteronline.de",
        // Socialna omrežja in sporočanje
        "facebook.com", "fb.com", "instagram.com", "twitter.com", "x.com",
        "linkedin.com", "tiktok.com", "telegram.org", "whatsapp.com",
        // E-trgovina
        "amazon.com", "amazon.de", "ebay.com", "aliexpress.com", "etsy.com",
        // CDN in infrastruktura (nalaganje pisav, slik, JS knjižnic)
        "cloudflare.com", "cloudflareinsights.com", "fastly.net", "akamaized.net",
        "akamai.net", "jsdelivr.net", "unpkg.com", "cdnjs.cloudflare.com",
        "fonts.googleapis.com", "fonts.gstatic.com", "ajax.googleapis.com",
        // Slovenske bančne, javne in novičarske storitve
        "gov.si", "nlb.si", "nkbm.si", "skb.si", "intesasanpaolobank.si", "dh.si",
        "delavska-hranilnica.si", "sparkasse.si", "bks-bank.si", "unicreditbank.si",
        "posta.si", "zvezapotrosnikov.si",
        // Gostitelji varnostnih feedov
        "abuse.ch", "phishing.army", "cert.si"
    )


    /** Kategorija opozorila BankGuard: ni kritična, uporabnik lahko po opozorilu nadaljuje. */
    const val FAKE_BANK_CATEGORY = "Lažna banka (Phishing)"

    // Gostitelji, ki gostijo vsebino uporabnikov: zanje velja preverjanje vsebine strani kljub izjemam.
    private val USER_CONTENT_HOSTS = setOf("sites.google.com", "docs.google.com", "forms.office.com")

    private val bankHostCache = object : LinkedHashMap<String, BankVerdict?>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BankVerdict?>?) = size > 512
    }

    /** BankGuard: gostitelj, ki se s svojim imenom izdaja za banko (rezultat se hrani, preverjanje je lokalno). */
    fun fakeBankHost(host: String): BankVerdict? {
        synchronized(bankHostCache) {
            if (bankHostCache.containsKey(host)) return bankHostCache[host]
        }
        val verdict = try { BankGuard.hostVerdict(host) } catch (e: Exception) { null }
        synchronized(bankHostCache) { bankHostCache[host] = verdict }
        return verdict
    }

    /** Uporabnik je po opozorilu izbral nadaljevanje za ta gostitelj (ta seja). */
    fun isAllowedForSession(host: String): Boolean = sessionBypassedDomains.contains(host.lowercase().trim().trimEnd('.'))

    /** Naslov zgodovine za opozorilo po naložitvi strani (prepozna vrnitev z "Nazaj"). */
    const val FAKE_BANK_HISTORY_URL = "safeer://security-interstitial/fake-bank"
    /** Sheme lokalno odprtih strani (priponke HTML iz pošte), ki jih BankGuard preveri po vsebini. */
    val LOCAL_PAGE_SCHEMES = setOf("file", "content")

    /** Prave banke (uradne domene, domene bančnih skupin, plačilna in identitetna infrastruktura). */
    fun isRealBankHost(host: String): Boolean = try { BankGuard.isTrusted(host) } catch (e: Exception) { false }

    // Gostitelj lažne banke -> uradna domena prave banke (za gumb "Odpri pravo stran" na opozorilu)
    private val fakeBankOfficialDomains = ConcurrentHashMap<String, String>()

    /** Ključ za lokalno odprto datoteko (priponka iz pošte), ki nima gostitelja. */
    const val LOCAL_PAGE_KEY = "lokalna-datoteka"

    private fun fakeBankMatch(host: String, verdict: BankVerdict, fromPage: Boolean): DomainSuffixTrie.MatchResult {
        if (fakeBankOfficialDomains.size > 256) fakeBankOfficialDomains.clear()
        if (verdict.officialDomain.isNotEmpty()) fakeBankOfficialDomains[host] = verdict.officialDomain
        val explanation = when (verdict.reason) {
            "lure" -> "Stran zahteva podatke plačilne kartice pod pretvezo »${verdict.detail}«; policija, FURS in dostavne službe kazni in poštnine ne pobirajo prek takih strani"
            "local" -> "Datoteka, odprta iz priponke ali prenosa, se predstavlja kot ${verdict.bankName}; prava stran je ${verdict.officialDomain}"
            else -> "${if (fromPage) "Stran se predstavlja kot" else "Naslov posnema"} ${verdict.bankName}; prava stran je ${verdict.officialDomain}"
        }
        return DomainSuffixTrie.MatchResult(
            isMatched = true,
            matchedDomain = host,
            category = FAKE_BANK_CATEGORY,
            sourceFeed = "Safeer Threat Shield · $explanation",
        )
    }

    /**
     * BankGuard za naloženo stran: [signalsJson] je rezultat BankGuard.PAGE_SCRIPT v glavnem oknu.
     * Opozori le, če stran na tuji domeni prikazuje polje za geslo, kodo ali kartico in se predstavlja kot banka.
     */
    fun checkFakeBankPage(pageUrl: String, signalsJson: String?): DomainSuffixTrie.MatchResult? {
        if (!isEnabled) return null
        return try {
            val signals = BankGuard.signalsFromJson(signalsJson) ?: return null
            val pageScheme = Uri.parse(pageUrl).scheme?.lowercase() ?: return null
            if (pageScheme in LOCAL_PAGE_SCHEMES) {
                // Priponka HTML (file:, content:) nima gostitelja; zadostuje, da se strinjata shemi.
                if (signals.scheme != pageScheme || sessionBypassedDomains.contains(LOCAL_PAGE_KEY)) return null
                val verdict = BankGuard.pageVerdict(signals) ?: return null
                return fakeBankMatch(LOCAL_PAGE_KEY, verdict, fromPage = true)
            }
            val pageHost = Uri.parse(pageUrl).host?.lowercase()?.trim()?.trimEnd('.') ?: return null
            val host = signals.host.lowercase().trim().trimEnd('.')
            if (host.isEmpty() || host != pageHost) return null // odgovor stare strani po navigaciji
            if (isNeverBlockDomain(host) && host !in USER_CONTENT_HOSTS) return null
            if (sessionBypassedDomains.contains(host)) return null
            val verdict = BankGuard.pageVerdict(signals) ?: return null
            fakeBankMatch(host, verdict, fromPage = true)
        } catch (e: Exception) {
            null
        }
    }

    fun isNeverBlockDomain(host: String): Boolean {
        val h = host.lowercase().trim()
        if (NEVER_BLOCK_EXACT.contains(h)) return true
        if (NEVER_BLOCK_ROOT_DOMAINS.contains(h)) return true
        for (w in NEVER_BLOCK_ROOT_DOMAINS) {
            if (h.endsWith(".$w")) return true
        }
        return false
    }

    init {
        loadSeedThreatDatabase(threatTrie)
    }

    /**
     * Zamenja celotno drevo groženj z novim atomskim triejem brez prekinitev.
     */
    fun swapThreatTrie(newTrie: DomainSuffixTrie) {
        threatTrie = newTrie
    }

    /**
     * Ustvari enokratni žeton za varen obvoz varnostnega zaslona.
     */
    fun createBypassToken(domain: String, targetUrl: String): String {
        val now = System.currentTimeMillis()
        pendingBypasses.entries.removeIf { it.value.expiryMs < now }
        val token = java.util.UUID.randomUUID().toString().replace("-", "")
        pendingBypasses[token] = PendingBypass(domain, targetUrl, now + 300_000L) // 5 minut veljavnosti
        return token
    }

    /**
     * Porabi enokratni žeton in vrne podatke o obvozu, če je veljaven.
     */
    fun consumeBypassToken(token: String): PendingBypass? {
        val entry = pendingBypasses.remove(token) ?: return null
        if (System.currentTimeMillis() > entry.expiryMs) return null
        return entry
    }

    /**
     * Vnaprej naložena semenska baza znanih nevarnih C2, malware in phishing domen.
     */
    fun loadSeedThreatDatabase(trie: DomainSuffixTrie = threatTrie) {
        // 1. abuse.ch Feodo Tracker (Botnet C2 strežniki - Dridex, Emotet, QakBot, TrickBot)
        val feodoC2 = listOf(
            "c2-tracker.net", "botnet-master.org", "dridex-panel.cc",
            "emotet-feed.com", "qakbot-gate.biz", "trickbot-c2.top", "icedid-network.cc",
            "bazarloader-c2.net", "cobaltstrike-beacon.info", "lokibot-panel.ru", "redline-stealer.cc",
            "vidar-c2.top", "raccoon-gate.com", "asyncrat-host.duckdns.org", "njrat-beacon.biz",
            "remcos-c2.org", "agenttesla-gate.net", "formbook-panel.cc", "xworm-controller.top"
        )
        for (d in feodoC2) trie.insert(d, category = "Botnet C2 Server", sourceFeed = "abuse.ch Feodo Tracker")

        // 2. abuse.ch URLhaus & ThreatFox (Zlonamerna koda / Malware distribution & IOC)
        val urlhausMalware = listOf(
            "malware-drop.com", "payload-delivery.cc",
            "evil-apk-download.net", "stealer-gate.org", "cryptominer-pool.top", "ransomware-host.xyz",
            "dropper-server.ru", "trojan-source.cc", "apk-injector.top", "malicious-script.biz",
            "23vlcfp.cfd", "2lizguk.buzz", "x91kza.monster", "dl-android-update.top",
            "system-patch-android.click", "security-alert-center.top", "device-scan-security.cc"
        )
        for (d in urlhausMalware) trie.insert(d, category = "Zlonamerna koda (Malware)", sourceFeed = "abuse.ch URLhaus / ThreatFox")

        // 3. Phishing Army & Lažno predstavljanje (Kraja gesel in bančnih podatkov)
        val phishingDomains = listOf(
            "login-bank-verification.com", "secure-account-update.net",
            "verify-paypal-center.com", "apple-id-suspended.info", "google-account-recovery.top",
            "microsoft-auth-verify.cc", "nlb-klik-prijava.com", "nkbm-varnostni-pregled.net",
            "posta-slovenije-paket.top", "dhl-slovenia-slednje.cc", "si-pass-prijava.info"
        )
        for (d in phishingDomains) trie.insert(d, category = "Spletno ribarjenje (Phishing)", sourceFeed = "Phishing Army")

        // 4. StevenBlack Malware & Agresivna stavniška omrežja z nevarno kodo
        val stevenBlackMalware = listOf(
            "20bet.top", "20bet-aff.com", "1xbet.mobi", "1xbet-partner.com", "vulkanvegas-play.top",
            "parimatch-aff.com", "monetag-loader.com", "richpush-ads.co", "onclickalgo.com",
            "syndication.exoclick.com"
        )
        for (d in stevenBlackMalware) trie.insert(d, category = "Nevarno oglasno/stavno omrežje", sourceFeed = "StevenBlack Unified")
    }

    /**
     * Zgradi novo drevo iz vgrajenega seznama in seznamov agenta ter ga atomsko zamenja.
     * Domene pravih bank se iz zunanjih seznamov prevzamejo samo kot potrjen C2/malware, nikoli kot ribarjenje.
     */
    fun rebuildFromLists(lists: List<com.safeer.threatfeed.PlainList>): Int {
        val newTrie = DomainSuffixTrie()
        loadSeedThreatDatabase(newTrie)
        var added = 0
        for (list in lists) {
            val critical = isCriticalThreat(list.source.category)
            for (domain in list.entries) {
                if (!critical && isRealBankHost(domain)) continue
                if (!critical && isNeverBlockDomain(domain)) continue
                newTrie.insert(domain, list.source.category, list.source.name)
                added++
            }
        }
        swapThreatTrie(newTrie)
        return added
    }

    /**
     * Vstavi novo zaznano grožnjo v bazo.
     */
    fun addThreat(domain: String, category: String, sourceFeed: String) {
        threatTrie.insert(domain, category, sourceFeed)
    }

    /**
     * Preveri, ali kategorija grožnje spada med kritične C2/Malware strežnike (pravilo Zero-Bypass).
     */
    fun isCriticalThreat(category: String?): Boolean {
        if (category == null) return false
        val c = category.lowercase()
        return c.contains("c2") || c.contains("botnet") || c.contains("malware") || c.contains("zlonamerna")
    }

    /**
     * Preveri, ali URL ali gostitelj predstavlja varnostno grožnjo.
     * Vrne podrobnosti o grožnji ali null, če je domena varna.
     */
    fun checkThreat(url: String): DomainSuffixTrie.MatchResult? {
        if (!isEnabled || url.isEmpty()) return null

        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase()?.trim() ?: return null
            if (host.isEmpty()) return null

            val match = findThreat(url, host)
            if (match == null) {
                // 🏦 BankGuard: naslov, ki posnema banko (npr. nlb-klik-prijava.com, otpbamka.si)
                if (isNeverBlockDomain(host) || sessionBypassedDomains.contains(host)) return null
                val verdict = fakeBankHost(host) ?: return null
                return fakeBankMatch(host, verdict, fromPage = false)
            }

            // 🔒 ZERO-BYPASS PRAVILO: Kritične C2 in Malware grožnje NIKOLI nimajo izjeme!
            if (isCriticalThreat(match.category)) {
                return match
            }

            // Compatibility exceptions never override a confirmed malware/C2 match.
            // Prave banke nikoli ne dobijo opozorila o ribarjenju ali prevari (seznami se lahko zmotijo).
            if (isNeverBlockDomain(host) || isRealBankHost(host)) return null

            // Manj nevarne kategorije (phishing/ad opozorila) lahko imajo sejne izjeme
            if (sessionBypassedDomains.contains(host) || sessionBypassedDomains.contains(match.matchedDomain.lowercase())) {
                return null
            }

            return match
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Pomožna funkcija za hitro preverjanje, ali je URL grožnja.
     */
    fun isThreat(url: String): Boolean = checkThreat(url) != null

    /**
     * Odobri domeno za to sejo (uporabnik je izbral 'Nadaljuj na lastno odgovornost').
     * Kritične C2/Malware domene se brezpogojno zavrnejo.
     */
    fun allowForSession(domain: String) {
        val clean = domain.trim().lowercase()
        if (clean.isEmpty()) return
        val isUrl = clean.contains("://")
        val host = if (isUrl) {
            (try { Uri.parse(clean).host } catch (e: Exception) { null })?.lowercase() ?: return
        } else {
            clean
        }
        val local = threatTrie.findMatch(host)
        val signed = signedMatch(if (isUrl) clean else "https://$clean/", host)
        val critical = listOfNotNull(local, signed).firstOrNull { isCriticalThreat(it.category) }
        if (critical != null) {
            android.util.Log.w("SafeerSecurity", "🔒 Zero-Bypass: zavrnjen poskus obvoza za kritično grožnjo '$clean' (${critical.category})")
            return
        }
        sessionBypassedDomains.add(clean)
    }

    /**
     * Zabeleži statistiko blokade.
     */
    fun recordBlock(result: DomainSuffixTrie.MatchResult) {
        totalBlockedThreats.incrementAndGet()
        val cat = result.category?.lowercase() ?: ""
        when {
            cat.contains("c2") || cat.contains("botnet") -> blockedC2Count.incrementAndGet()
            cat.contains("malware") || cat.contains("zlonamerna") -> blockedMalwareCount.incrementAndGet()
            cat.contains("phishing") || cat.contains("ribarjenje") -> blockedPhishingCount.incrementAndGet()
            else -> blockedIocCount.incrementAndGet()
        }
    }

    private fun htmlEscape(s: String): String {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
    }

    /**
     * Ustvari privlačen AMOLED Red varnostni opozorilni zaslon (Security Interstitial Page) za glavno okno.
     */
    fun createSecurityInterstitialHtml(blockedUrl: String, match: DomainSuffixTrie.MatchResult, afterPageLoad: Boolean = false): String {
        // Opozorilo po naložitvi strani stoji za lažno stranjo v zgodovini: "Nazaj" preskoči obe.
        val backSteps = if (afterPageLoad) 2 else 1
        val domain = htmlEscape(match.matchedDomain)
        val category = htmlEscape(match.category ?: "Varnostna grožnja")
        val source = htmlEscape(match.sourceFeed ?: "Varnostni ščit Safeer Browser")
        val isCritical = isCriticalThreat(match.category)
        val isFakeBank = match.category == FAKE_BANK_CATEGORY
        val officialDomain = if (isFakeBank) fakeBankOfficialDomains[match.matchedDomain.lowercase()] else null
        val heading = if (isFakeBank) "Lažna spletna banka" else "Varnostna grožnja blokirana"
        val description = if (isFakeBank) {
            "Ta stran ni prava spletna banka. Na njej ne vpisujte uporabniškega imena, gesla, kode SMS, davčne številke, PIN-a ali podatkov kartice. " +
                "Do banke vedno dostopajte z vpisom uradnega naslova ali prek uradne aplikacije. " +
                "Banka vas nikoli ne pokliče, da bi zahtevala kodo ali PIN, in nikoli ne zahteva namestitve programov za oddaljeni dostop (AnyDesk, TeamViewer)."
        } else {
            "Safeer Browser je preprečil povezavo z nevarnim spletnim mestom, ki lahko ogrozi varnost vaše naprave ali poskuša ukrasti osebne podatke."
        }
        val officialButtonHtml = if (officialDomain != null) {
            "<a class=\"btn btn-primary\" style=\"background:#16a34a\" href=\"https://${htmlEscape(officialDomain)}/\">Odpri pravo stran: ${htmlEscape(officialDomain)}</a>"
        } else ""

        val bypassActionHtml = if (isCritical) {
            """
            <div style="margin-top: 14px; padding: 12px; background: rgba(255, 68, 68, 0.12); border: 1px solid rgba(255, 68, 68, 0.35); border-radius: 12px; font-size: 13px; color: #ff8888; text-align: center; line-height: 1.4;">
                🔒 <strong>Pravilo ničelnega obvoza (Zero-Bypass):</strong><br>Ta domena je identificirana kot kritični Botnet C2 ali Malware strežnik. Zaradi zaščite naprave obvoz ni dovoljen.
            </div>
            """.trimIndent()
        } else {
            val bypassToken = createBypassToken(match.matchedDomain, blockedUrl)
            """
            <a class="btn btn-danger-outline" href="safeer://bypass-threat?token=$bypassToken">
                Nadaljuj na lastno odgovornost (Odkleni za to sejo)
            </a>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="sl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>⚠️ Varnostno opozorilo - Safeer Browser</title>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    background-color: #050508;
                    color: #e5e5e5;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                    padding: 24px;
                    text-align: center;
                }
                .card {
                    background: rgba(22, 10, 14, 0.85);
                    border: 1px solid rgba(255, 68, 68, 0.35);
                    border-radius: 20px;
                    padding: 32px 24px;
                    max-width: 480px;
                    width: 100%;
                    box-shadow: 0 10px 40px rgba(255, 0, 0, 0.25);
                    backdrop-filter: blur(12px);
                }
                .icon {
                    width: 72px;
                    height: 72px;
                    margin: 0 auto 20px;
                    background: rgba(255, 68, 68, 0.15);
                    border: 2px solid #ff4444;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 36px;
                    animation: pulse 2s infinite;
                }
                @keyframes pulse {
                    0% { box-shadow: 0 0 0 0 rgba(255, 68, 68, 0.5); }
                    70% { box-shadow: 0 0 0 16px rgba(255, 68, 68, 0); }
                    100% { box-shadow: 0 0 0 0 rgba(255, 68, 68, 0); }
                }
                h1 {
                    font-size: 22px;
                    font-weight: 700;
                    color: #ff5555;
                    margin-bottom: 12px;
                }
                p.desc {
                    font-size: 15px;
                    color: #a0a0b0;
                    line-height: 1.5;
                    margin-bottom: 24px;
                }
                .badge-box {
                    background: rgba(0, 0, 0, 0.5);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    border-radius: 12px;
                    padding: 14px;
                    margin-bottom: 24px;
                    text-align: left;
                }
                .badge-row {
                    display: flex;
                    justify-content: space-between;
                    font-size: 13px;
                    margin-bottom: 6px;
                }
                .badge-row:last-child { margin-bottom: 0; }
                .badge-label { color: #888; }
                .badge-val { color: #fff; font-weight: 600; word-break: break-all; }
                .badge-danger { color: #ff5555; font-weight: 700; }
                
                .btn {
                    display: block;
                    width: 100%;
                    padding: 14px;
                    border-radius: 12px;
                    font-size: 15px;
                    font-weight: 600;
                    text-decoration: none;
                    cursor: pointer;
                    margin-bottom: 12px;
                    border: none;
                    transition: all 0.2s ease;
                }
                .btn-primary {
                    background: #2563eb;
                    color: #fff;
                    box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4);
                }
                .btn-primary:active { background: #1d4ed8; transform: scale(0.98); }
                .btn-danger-outline {
                    background: transparent;
                    color: #888;
                    border: 1px solid rgba(255, 255, 255, 0.15);
                    font-size: 13px;
                    padding: 10px;
                }
                .btn-danger-outline:active { color: #ff5555; border-color: #ff5555; }
                .footer-text {
                    font-size: 12px;
                    color: #555;
                    margin-top: 16px;
                }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="icon">🛑</div>
                <h1>$heading</h1>
                <p class="desc">$description</p>
                
                <div class="badge-box">
                    <div class="badge-row">
                        <span class="badge-label">Domena:</span>
                        <span class="badge-val">$domain</span>
                    </div>
                    <div class="badge-row">
                        <span class="badge-label">Vrsta grožnje:</span>
                        <span class="badge-danger">$category</span>
                    </div>
                    <div class="badge-row">
                        <span class="badge-label">Varnostni vir:</span>
                        <span class="badge-val">$source</span>
                    </div>
                </div>

                <button class="btn btn-primary" onclick="if (history.length > $backSteps) { history.go(-$backSteps); } else { location.href = 'about:blank'; }">
                    ⬅ Nazaj na varno (Priporočeno)
                </button>

                $officialButtonHtml
                
                $bypassActionHtml

                <div class="footer-text">
                    Zaščita Safeer Threat Shield • abuse.ch Feodo / URLhaus / ThreatFox
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Vrne WebResourceResponse z varnostnim opozorilom za glavno okno ali prazen odgovor za podvire.
     */
    fun handleThreatIntercept(url: String, isMainFrame: Boolean): WebResourceResponse? {
        val match = checkThreat(url) ?: return null
        recordBlock(match)
        onThreatBlocked?.invoke(match.matchedDomain, match.category ?: "Grožnja", match.sourceFeed ?: "Safeer Shield", isMainFrame)

        return if (isMainFrame) {
            val html = createSecurityInterstitialHtml(url, match)
            WebResourceResponse(
                "text/html",
                "UTF-8",
                200,
                "OK",
                mapOf("Access-Control-Allow-Origin" to "*", "Cache-Control" to "no-store"),
                ByteArrayInputStream(html.toByteArray(Charsets.UTF_8))
            )
        } else {
            // Podviri (skripte, slike, C2 beaconi) se tiho prekinejo z varnim praznim odgovorom
            WebResourceResponse(
                "text/plain",
                "UTF-8",
                200,
                "OK",
                mapOf("Access-Control-Allow-Origin" to "*"),
                ByteArrayInputStream(ByteArray(0))
            )
        }
    }
}
