package com.example.safeerbrowser

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicLong

/**
 * ⚡ AdBlockEngine
 * Napredno jedro za blokiranje oglasov s pomočjo Domain Suffix Trie podatkovne strukture,
 * preverjanja poti (Path Rules) in strogo nadzorovanih izjem za pretočne vsebine.
 */
object AdBlockEngine {

    var isEnabled: Boolean = true
    val blockedAdsCount = AtomicLong(0)

    var onAdBlocked: (() -> Unit)? = null

    // Suffix Trie za blokirane oglasne in stavniške domene (O(k) iskanje)
    private val blockedTrie = DomainSuffixTrie()

    // Suffix Trie za strogo preverjene varne domene (Bela lista)
    private val whitelistTrie = DomainSuffixTrie()

    // Potrjeni pretočni strežniki (kjer je dovoljeno pretakanje HLS tokov)
    private val VERIFIED_STREAMING_HOSTS = hashSetOf(
        "vidlink.pro", "vidsrc.me", "vidsrc.in", "vidsrc.pm", "vidsrc.net", "vidsrc.to", "vidsrc.xyz",
        "autoembed.co", "autoembed.cc", "multiembed.mov", "2embed.cc", "111movies.com", "hydrahd.ws",
        "megacloud.tv", "rabbitstream.net", "dokicloud.one", "vizcloud.online", "filemoon.sx", "streamtape.com"
    )

    // Vzorci oglasnih in sledilnih poti (Top Path Rules)
    private val BLOCKED_PATH_PATTERNS = listOf(
        "/pagead/", "/api/stats/ads", "/ptracking", "/get_midroll_info",
        "/ads.js", "/ad.js", "/adservice.", "/pixel.", "collect?v=",
        "/metrika", "/watch.js", "/tag.js", "/monetag/", "/popunder",
        "disable-devtool", "devtools-detector", "adsystem.com", "/delivery/"
    )

    init {
        initializeWhitelist()
        initializeBlockedDomains()
    }

    private fun initializeWhitelist() {
        // Stroga bela lista - samo specifične varne storitve (brez krovnih CDN-jev kot cloudflare.com/cloudfront.net)
        val trusted = listOf(
            "google.com", "google.si", "gstatic.com", "googleapis.com", "googleusercontent.com",
            "duckduckgo.com", "bing.com", "yahoo.com", "wikipedia.org", "wikimedia.org",
            "youtube.com", "m.youtube.com", "music.youtube.com", "googlevideo.com", "ytimg.com",
            "accounts.youtube.com", "accounts.google.com", "myaccount.google.com",
            "nlb.si", "nkbm.si", "skb.si", "dh.si", "intesa.si", "intesasanpaolobank.si",
            "sparkasse.si", "revolut.com", "n26.com", "delavska-hranilnica.si",
            "bks-bank.si", "unicreditbank.si", "lon.si", "gorenjska-banka.si",
            "rtvslo.si", "24ur.com", "siol.net", "github.com",
            "themoviedb.org", "tmdb.org", "image.tmdb.org", "api.themoviedb.org"
        )
        for (d in trusted) whitelistTrie.insert(d)
        for (d in VERIFIED_STREAMING_HOSTS) whitelistTrie.insert(d)
    }

    private fun initializeBlockedDomains() {
        val adsAndGambling = listOf(
            // Stavniške & Casino platforme
            "20bet.com", "20bet.top", "20bet-aff.com", "1xbet.com", "1xbet.mobi", "1xbet-partner.com",
            "betwinner.com", "melbet.com", "mostbet.com", "vulkanvegas.com", "parimatch.com", "ggbet.com",
            "betsson.com", "unibet.com", "bet365.com", "betway.com", "bwin.com", "campobet.com",
            "rabona.com", "fezbet.com", "librabet.com", "nomini.com", "wazamba.com", "sportaza.com",
            "greatwin.com", "casinia.com", "spinanga.com", "boomerang-casino.com",

            // Popunderji & Agresivna oglasna omrežja
            "popads.net", "popcash.net", "monetag.com", "adcash.com", "propellerads.com",
            "exoclick.com", "trafficjunky.com", "clickadu.com", "adsterra.com", "adxad.com",
            "hilltopads.com", "richpush.co", "pushground.com", "admaven.com", "rollerads.com",
            "juicyads.com", "trafficfactory.biz", "realsrv.com", "onclickalgo.com", "onclickperformance.com",
            "syndication.exoclick.com", "syndication.realsrv.com", "delivery.trafficjunky.net",
            "engine.phn.doublepimp.com",

            // Oglasni strežniki in sledilci
            "doubleclick.net", "googleads.g.doubleclick.net", "static.doubleclick.net",
            "googlesyndication.com", "pagead2.googlesyndication.com", "googleadservices.com",
            "adservice.google.com", "adservice.google.si", "amazon-adsystem.com",
            "taboola.com", "outbrain.com", "criteo.com", "adnxs.com", "rubiconproject.com",
            "pubmatic.com", "openx.net", "smartadserver.com", "bidswitch.net", "casalemedia.com",
            "scorecardresearch.com", "quantserve.com", "hotjar.com", "clarity.ms",
            "mc.yandex.ru", "metrika.yandex.ru", "an.yandex.ru"
        )
        for (d in adsAndGambling) blockedTrie.insert(d)
    }

    /**
     * Preveri, ali je domena na strogi beli listi.
     */
    fun isWhitelisted(host: String): Boolean {
        return whitelistTrie.matches(host)
    }

    /**
     * Preveri, ali URL ustreza oglasu, sledilcu ali blokirani domeni.
     */
    fun shouldBlockUrl(url: String): Boolean {
        if (!isEnabled || url.isEmpty()) return false
        val lower = url.lowercase()

        // 1. Devtools zaščita (disable-devtool.js vedno blokiraj)
        if (lower.contains("disable-devtool") || lower.contains("devtools-detector")) {
            return true
        }

        // 2. YouTube & Google Video legitimni viri
        if (lower.contains("googlevideo.com") ||
            lower.contains("youtube.com/s/player") ||
            lower.contains("youtube.com/youtubei")) {
            return false
        }

        // 3. Preverjanje poti (Path Rules)
        for (pattern in BLOCKED_PATH_PATTERNS) {
            if (lower.contains(pattern)) {
                return true
            }
        }

        // 4. Strogo preverjanje potrjenih pretočnih strežnikov (brez splošnih lukenj za poljubne domene)
        try {
            val uri = Uri.parse(lower)
            val host = uri.host?.lowercase()?.trim() ?: ""

            if (host.isNotEmpty()) {
                // Če je na beli listi in ni oglasne poti, dovoli
                if (whitelistTrie.matches(host)) {
                    return false
                }

                // Preveri blokirane domene v Trie (O(k))
                if (blockedTrie.matches(host)) {
                    return true
                }
            }
        } catch (_: Exception) {}

        return false
    }

    /**
     * Prestrezanje oglasnih zahtevkov in vračanje veljavnih praznih odgovorov.
     */
    fun handleIntercept(url: String): WebResourceResponse? {
        if (!isEnabled) return null
        val lower = url.lowercase()

        if (shouldBlockUrl(url)) {
            blockedAdsCount.incrementAndGet()
            onAdBlocked?.invoke()

            val isJson = lower.endsWith(".json") || lower.contains("json") ||
                         lower.contains("/pagead/") || lower.contains("/api/stats/ads") ||
                         lower.contains("get_midroll_info")

            val mime = when {
                isJson -> "application/json"
                lower.endsWith(".js") -> "application/javascript"
                lower.endsWith(".css") -> "text/css"
                lower.endsWith(".png") -> "image/png"
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
                lower.endsWith(".gif") -> "image/gif"
                lower.endsWith(".svg") -> "image/svg+xml"
                lower.endsWith(".html") -> "text/html"
                else -> "text/plain"
            }

            val contentBytes = if (isJson) {
                "{\"adPlacements\":[],\"status\":\"ok\"}".toByteArray(Charsets.UTF_8)
            } else {
                ByteArray(0)
            }

            return WebResourceResponse(
                mime,
                "UTF-8",
                200,
                "OK",
                mapOf("Access-Control-Allow-Origin" to "*", "Cache-Control" to "no-store"),
                ByteArrayInputStream(contentBytes)
            )
        }

        return null
    }
}
