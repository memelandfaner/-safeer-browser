package com.safeer.mobile.browser

import com.safeer.threatfeed.FeedMatch
import com.safeer.threatfeed.FeedSource
import com.safeer.threatfeed.PlainList
import com.safeer.threatfeed.PlainListSource

fun main() {
    check(!ThreatBlockEngine.isThreat("https://www.bbc.com/news"))
    check(!ThreatBlockEngine.isThreat("https://www.rtvslo.si/"))
    ThreatBlockEngine.addThreat("compromised.fastly.net", "Malware", "test fixture")
    check(ThreatBlockEngine.isThreat("https://compromised.fastly.net/embed/video.m3u8"))
    check(ThreatBlockEngine.isThreat("https://compromised.fastly.net/login?state=fixture"))
    ThreatBlockEngine.allowForSession("compromised.fastly.net")
    check(ThreatBlockEngine.isThreat("https://compromised.fastly.net/file"))
    check(!ThreatBlockEngine.isThreat("https://unrelated.fastly.net/file"))
    check(!ThreatBlockEngine.isThreat("https://compromised.fastly.net.example.org/file"))
    val token = ThreatBlockEngine.createBypassToken("warning.test", "https://warning.test/")
    check(ThreatBlockEngine.consumeBypassToken(token) != null)
    check(ThreatBlockEngine.consumeBypassToken(token) == null)
    println("PASS: ordinary sites, CDN malware, no critical bypass, domain boundary, single-use tokens")

    // Signed Safeer Threat Intelligence layer (matcher installed by SignedThreatIntel on Android)
    ThreatBlockEngine.signedFeedMatcher = { url, host ->
        when {
            host == "c2.signed.test" -> DomainSuffixTrie.MatchResult(true, host, "Botnet C2 strežnik", "Safeer Threat Intelligence")
            host.endsWith("phish.signed.test") -> DomainSuffixTrie.MatchResult(true, "phish.signed.test", "Spletno ribarjenje (Phishing)", "Safeer Threat Intelligence")
            url.startsWith("https://files.signed.test/payload") -> DomainSuffixTrie.MatchResult(true, "https://files.signed.test/payload", "Zlonamerna koda (Malware)", "Safeer Threat Intelligence")
            host == "broken.signed.test" -> throw IllegalStateException("matcher failure")
            else -> null
        }
    }
    check(ThreatBlockEngine.isThreat("https://c2.signed.test/beacon"))
    check(ThreatBlockEngine.isThreat("https://files.signed.test/payload"))
    check(!ThreatBlockEngine.isThreat("https://files.signed.test/readme"))
    check(!ThreatBlockEngine.isThreat("https://broken.signed.test/")) { "matcher errors never block or crash" }
    ThreatBlockEngine.allowForSession("c2.signed.test")
    check(ThreatBlockEngine.isThreat("https://c2.signed.test/beacon")) { "signed C2 has no bypass" }
    ThreatBlockEngine.allowForSession("https://files.signed.test/payload")
    check(ThreatBlockEngine.isThreat("https://files.signed.test/payload")) { "signed malware URL has no bypass" }
    check(ThreatBlockEngine.isThreat("https://login.phish.signed.test/"))
    ThreatBlockEngine.allowForSession("phish.signed.test")
    check(!ThreatBlockEngine.isThreat("https://login.phish.signed.test/")) { "explicit phishing bypass covers the matched domain" }
    ThreatBlockEngine.addThreat("mixed.signed.test", "Spletno ribarjenje (Phishing)", "seed")
    ThreatBlockEngine.signedFeedMatcher = { _, host ->
        if (host == "mixed.signed.test") DomainSuffixTrie.MatchResult(true, host, "Zlonamerna koda (Malware)", "Safeer Threat Intelligence") else null
    }
    check(ThreatBlockEngine.checkThreat("https://mixed.signed.test/")?.category == "Zlonamerna koda (Malware)") { "critical signed match wins" }
    ThreatBlockEngine.signedFeedMatcher = null
    check(ThreatBlockEngine.checkThreat("https://mixed.signed.test/")?.category == "Spletno ribarjenje (Phishing)")
    val source = FeedSource("fixture", "CC0-1.0", "Fixture", "https://safeer.si/")
    fun signed(category: String) = SignedThreatIntel.toMatchResult(FeedMatch("x.test", "hostname", category, source))
    check(ThreatBlockEngine.isCriticalThreat(signed("botnet_c2")?.category) && ThreatBlockEngine.isCriticalThreat(signed("malware")?.category))
    check(!ThreatBlockEngine.isCriticalThreat(signed("phishing")?.category) && !ThreatBlockEngine.isCriticalThreat(signed("scam")?.category))
    check(signed("ads") == null && signed("tracker") == null) { "ads are not security threats" }
    check(signed("malware")?.sourceFeed == "Safeer Threat Intelligence · Fixture")
    check(SignedThreatIntel.isEnabled == SignedThreatIntel.TRUSTED_KEYS.isNotEmpty())
    check(SignedThreatIntel.TRUSTED_KEYS.values.all { java.util.Base64.getDecoder().decode(it).size == 32 }) { "trusted keys are 32-byte Ed25519 keys" }
    println("PASS: signed feed layer, zero-bypass for signed C2/malware, matcher failures ignored")

    // 🏦 BankGuard
    val fake = ThreatBlockEngine.FAKE_BANK_CATEGORY
    check(ThreatBlockEngine.checkThreat("https://nlb-klik-varnost.net/prijava")?.category == fake) { "bank lookalike host" }
    check(ThreatBlockEngine.checkThreat("https://otpbamka.si/")?.category == fake) { "bank typosquat host" }
    check(!ThreatBlockEngine.isCriticalThreat(fake)) { "fake bank warning can be bypassed after the warning" }
    for (real in listOf("https://klik.nlb.si/", "https://bankanet.otpbanka.si/", "https://www.dbs.si/", "https://3ds.bankart.si/acs", "https://www.paypal.com/signin")) {
        check(!ThreatBlockEngine.isThreat(real)) { "real bank blocked: $real" }
    }
    ThreatBlockEngine.addThreat("bankanet.otpbanka.si", "Spletno ribarjenje (Phishing)", "mistaken list")
    check(!ThreatBlockEngine.isThreat("https://bankanet.otpbanka.si/")) { "a mistaken phishing entry never blocks a real bank" }
    val lookalike = ThreatBlockEngine.checkThreat("https://nkbm-prijava.eu/")!!
    val warning = ThreatBlockEngine.createSecurityInterstitialHtml("https://nkbm-prijava.eu/", lookalike)
    check(warning.contains("Lažna spletna banka") && warning.contains("href=\"https://otpbanka.si/\"") && warning.contains("bypass-threat")) { "fake bank warning page" }
    ThreatBlockEngine.allowForSession("nkbm-prijava.eu")
    check(!ThreatBlockEngine.isThreat("https://nkbm-prijava.eu/")) { "session bypass after the warning" }

    val pageJson = """{"host":"secure-login.example","scheme":"https","password":true,"otp":false,"card":false,"title":"NLB Klik - prijava","site":"","headings":"","logos":"","text":""}"""
    val pageMatch = ThreatBlockEngine.checkFakeBankPage("https://secure-login.example/index.html", pageJson)
    check(pageMatch?.category == fake && pageMatch!!.sourceFeed!!.contains("nlb.si")) { "fake bank page" }
    check(ThreatBlockEngine.createSecurityInterstitialHtml("https://secure-login.example/", pageMatch!!, afterPageLoad = true).contains("history.go(-2)"))
    check(ThreatBlockEngine.checkFakeBankPage("https://other.example/", pageJson) == null) { "late answer of a previous page is ignored" }
    check(ThreatBlockEngine.checkFakeBankPage("https://klik.nlb.si/", pageJson.replace("secure-login.example", "klik.nlb.si")) == null) { "real bank page" }
    check(ThreatBlockEngine.checkFakeBankPage("https://www.facebook.com/nlb", pageJson.replace("secure-login.example", "www.facebook.com")) == null)
    check(ThreatBlockEngine.checkFakeBankPage("https://sites.google.com/view/x", pageJson.replace("secure-login.example", "sites.google.com")) != null) { "user content hosts are checked" }
    check(ThreatBlockEngine.checkFakeBankPage("https://secure-login.example/", pageJson.replace("\"password\":true", "\"password\":false")) == null) { "no credential field" }
    check(ThreatBlockEngine.checkFakeBankPage("https://secure-login.example/", "null") == null && ThreatBlockEngine.checkFakeBankPage("https://secure-login.example/", "{bad") == null)

    // Lists from the agent: real banks are never taken over, the built-in list stays
    val urlhaus = PlainListSource("urlhaus", "abuse.ch URLhaus", "https://urlhaus.abuse.ch/downloads/hostfile/", "Zlonamerna koda (Malware)", "urlhaus")
    val phishing = PlainListSource("phishing-army", "Phishing Army", "https://phishing.army/", "Spletno ribarjenje (Phishing)", "phishing")
    val added = ThreatBlockEngine.rebuildFromLists(listOf(
        PlainList(urlhaus, listOf("evil-from-list.example", "compromised.posta.si"), 0),
        PlainList(phishing, listOf("phish-from-list.example", "www.24ur.com", "paypal.com", "www.nlb.si", "online.intesasanpaolobank.si"), 0),
    ))
    check(added == 3) { "added $added" }
    check(ThreatBlockEngine.isThreat("https://evil-from-list.example/") && ThreatBlockEngine.isThreat("https://phish-from-list.example/"))
    check(ThreatBlockEngine.isThreat("https://compromised.posta.si/x.apk")) { "confirmed malware on a catalogue host still blocks" }
    check(!ThreatBlockEngine.isThreat("https://www.nlb.si/") && !ThreatBlockEngine.isThreat("https://www.24ur.com/") && !ThreatBlockEngine.isThreat("https://www.paypal.com/"))
    check(!ThreatBlockEngine.isAllowedForSession("secure-login.example") && ThreatBlockEngine.isAllowedForSession("NKBM-prijava.eu."))
    check(ThreatBlockEngine.isThreat("https://payload-delivery.cc/")) { "built-in list kept" }
    println("PASS: BankGuard hosts and pages, real banks untouched, agent lists")
}
