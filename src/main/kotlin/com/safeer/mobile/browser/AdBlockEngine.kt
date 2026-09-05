package com.safeer.mobile.browser

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicLong

/**
 * ⚡ AdBlockEngine
 * Visoko-zmogljivo jedro za blokiranje oglasov s pomočjo Domain Suffix Trie podatkovne strukture,
 * preverjanja poti (Path Rules) in preprečevanja popunder / in-page push oglasnih omrežij brez motenja normalne navigacije.
 */
object AdBlockEngine {

    var isEnabled: Boolean = true
    val blockedAdsCount = AtomicLong(0)

    var onAdBlocked: (() -> Unit)? = null

    // Suffix Trie za blokirane oglasne in stavniške domene (O(k) iskanje)
    private val blockedTrie = DomainSuffixTrie()

    // Suffix Trie za strogo preverjene varne domene (Bela lista)
    private val whitelistTrie = DomainSuffixTrie()

    // 1x1 prozorni GIF za nevtralizacijo oglasnih slik brez zlomljenih okvirjev
    private val TRANSPARENT_1X1_GIF = byteArrayOf(
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
        0x80.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x21, 0xf9.toByte(), 0x04, 0x01, 0x00, 0x00, 0x00, 0x00,
        0x2c, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,
        0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3b
    )

    // Vzorci oglasnih, sledilnih in analitičnih poti (Path Rules)
    private val BLOCKED_PATH_PATTERNS = listOf(
        "/pagead/", "/pagead2/", "/api/stats/ads", "/ptracking", "/get_midroll_info",
        "/ad_status", "/ads/v1/", "/ads/v2/", "/ad_break", "/adserver/", "/adsystem/",
        "/adservices/", "/ads.js", "/ad.js", "/adservice.", "/metrika",
        "/monetag/", "/popunder", "/pop-under",
        "/_xa/ads", "/_xa/", "justservingfiles.net", "etahub.com",
        "delivery.trafficjunky", "trafficjunky", "tsyndicate",
        "disable-devtool", "devtools-detector",
        "google-analytics.com/g/collect", "google-analytics.com/analytics.js",
        "googletagmanager.com/gtm.js", "googletagmanager.com/gtag/js"
    )

    init {
        initializeWhitelist()
        initializeBlockedDomains()
    }

    private fun initializeWhitelist() {
        val trusted = listOf(
            "google.com", "google.si", "gstatic.com", "googleapis.com", "googleusercontent.com",
            "duckduckgo.com", "bing.com", "yahoo.com", "wikipedia.org", "wikimedia.org",
            "youtube.com", "m.youtube.com", "music.youtube.com", "googlevideo.com", "ytimg.com",
            "accounts.youtube.com", "accounts.google.com", "myaccount.google.com",
            "nlb.si", "nkbm.si", "skb.si", "dh.si", "intesa.si", "intesasanpaolobank.si",
            "sparkasse.si", "revolut.com", "n26.com", "delavska-hranilnica.si",
            "bks-bank.si", "unicreditbank.si", "lon.si", "gorenjska-banka.si",
            "rtvslo.si", "24ur.com", "siol.net", "github.com",
            "gov.si", "e-uprava.gov.si", "posta.si", "si-pass.si", "rekono.si",
            "cloudflare.com", "apple.com", "microsoft.com", "xploretv.si"
        )
        for (d in trusted) whitelistTrie.insert(d)
    }

    private fun initializeBlockedDomains() {
        val adsAndTrackers = listOf(
            // Popunderji, In-Page Push & Agresivna oglasna omrežja
            "popads.net", "popcash.net", "monetag.com", "adcash.com", "propellerads.com",
            "exoclick.com", "trafficjunky.com", "trafficjunky.net", "ads.trafficjunky.net", "delivery.trafficjunky.net",
            "tsyndicate.com", "clickadu.com", "adsterra.com", "adxad.com",
            "hilltopads.com", "hilltopads.net", "richpush.co", "pushground.com", "admaven.com", "rollerads.com",
            "juicyads.com", "trafficfactory.biz", "realsrv.com", "onclickalgo.com", "onclickperformance.com",
            "onclickmega.com", "onclickgate.com", "syndication.exoclick.com", "syndication.realsrv.com",
            "doublepimp.com", "deloplen.com", "highperformancegate.com", "effectivegate.com", "pussing.com",
            "propu.sh", "creativecdn.com", "whosamung.us", "traffichaus.com", "bngpt.com", "adnxs.com", "adnxs-simple.com",
            "adtrue.com", "ad-score.com", "runative-syndicate.com", "plugrush.com", "bullionpromotions.com",
            "vlitag.com", "vidcrunch.com", "aniview.com", "primis.tech", "springserve.com", "smartclip.net",
            "adhigh.net", "adkernel.com", "adsupply.com", "adtarget.me", "adup-tech.com", "adxprts.com", "airpush.com",

            // Oglasni strežniki, borze in sledilci
            "doubleclick.net", "googleads.g.doubleclick.net", "static.doubleclick.net",
            "googlesyndication.com", "pagead2.googlesyndication.com", "googleadservices.com",
            "adservice.google.com", "adservice.google.si", "amazon-adsystem.com",
            "taboola.com", "outbrain.com", "criteo.com", "criteo.net", "rubiconproject.com",
            "pubmatic.com", "openx.net", "smartadserver.com", "bidswitch.net", "casalemedia.com",
            "indexexchange.com", "yieldmo.com", "triplelift.com", "gumgum.com", "seedtag.com",
            "adpushup.com", "ezoic.com", "adthrive.com", "mediavine.com", "media.net", "snigel.com",
            "flashtalking.com", "moatads.com", "adroll.com", "teads.tv", "spotxchange.com", "spotx.tv",
            "connatix.com", "kixer.com", "revcontent.com", "mgid.com", "content.ad", "adblade.com",
            "sharethrough.com", "sovrn.com", "lijit.com", "exponential.com", "tribalfusion.com", "zedo.com",
            "admob.com", "applovin.com", "unityads.unity3d.com", "ironsrc.com", "inmobi.com",
            "vungle.com", "liftoff.io", "mintegral.com", "chartboost.com", "fyber.com",

            // Vedenjsko sledenje, telemetrija in profiliranje
            "scorecardresearch.com", "quantserve.com", "hotjar.com", "clarity.ms",
            "fullstory.com", "logrocket.com", "mouseflow.com", "luckyorange.com", "crazyegg.com",
            "segment.io", "segment.com", "mixpanel.com", "amplitude.com", "branch.io",
            "appsflyer.com", "adjust.com", "kochava.com", "singular.net",
            "connect.facebook.net", "pixel.facebook.com", "analytics.tiktok.com",
            "mc.yandex.ru", "metrika.yandex.ru", "an.yandex.ru",

            // Potisna vsiljiva omrežja (Push scams)
            "pushwelcome.com", "news-feed2.com", "notifpush.com", "pushassist.com", "truepush.com"
        )
        for (d in adsAndTrackers) blockedTrie.insert(d)
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

        // 1. Razčleni gostitelja
        val uri = try { Uri.parse(lower) } catch (_: Exception) { null }
        val host = uri?.host?.lowercase()?.trim() ?: ""

        // 2. Preveri belo listo (z izjemo oglasnih googlevideo tokov)
        if (host.isNotEmpty() && whitelistTrie.matches(host)) {
            // Če gre za googlevideo, preveri, ali vsebuje parametre oglasa
            if (host.contains("googlevideo.com")) {
                if (lower.contains("&ctier=") || lower.contains("?ctier=") ||
                    lower.contains("&oad=") || lower.contains("?oad=") ||
                    lower.contains("&adformat=") || lower.contains("?adformat=") ||
                    lower.contains("&ad_type=") || lower.contains("?ad_type=")) {
                    return true // Blokiraj oglasni video tok!
                }
            }
            return false // Legitimna vsebina na beli listi je dovoljena
        }

        // 3. Devtools zaščita (disable-devtool.js vedno blokiraj)
        if (lower.contains("disable-devtool") || lower.contains("devtools-detector")) {
            return true
        }

        // 4. Domensko preverjanje v Trie (O(k))
        if (host.isNotEmpty() && blockedTrie.matches(host)) {
            return true
        }

        // 5. Preverjanje poti (Path Rules) za oglasne skripte
        for (pattern in BLOCKED_PATH_PATTERNS) {
            if (lower.contains(pattern)) {
                return true
            }
        }

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

            val isXml = (lower.endsWith(".xml") || lower.contains("xml") ||
                        lower.contains("vast") || lower.contains("vmap") ||
                        lower.contains("/delivery/") || lower.contains("/adtag")) &&
                        !lower.contains("_xa") && !lower.contains("json")

            val isJson = !isXml && (lower.endsWith(".json") || lower.contains("json") ||
                         lower.contains("/pagead/") || lower.contains("/api/stats/ads") ||
                         lower.contains("get_midroll_info") || lower.contains("/_xa/") ||
                         lower.contains("ads_batch") || lower.contains("/ads?") ||
                         lower.contains("trafficjunky"))

            val isImage = lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                          lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg") ||
                          lower.contains("/pixel") || lower.contains("/banner") || lower.contains("/ad-image")

            val mime = when {
                isXml -> "application/xml"
                isJson -> "application/json"
                isImage -> "image/gif"
                lower.endsWith(".js") || lower.contains(".js?") -> "application/javascript"
                lower.endsWith(".css") || lower.contains(".css?") -> "text/css"
                lower.endsWith(".html") -> "text/html"
                else -> "text/plain"
            }

            val contentBytes = when {
                isXml -> "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<VAST version=\"3.0\"/>".toByteArray(Charsets.UTF_8)
                isJson -> "{\"adPlacements\":[],\"ads\":[],\"adsBatch\":{},\"status\":\"ok\",\"success\":true}".toByteArray(Charsets.UTF_8)
                isImage -> TRANSPARENT_1X1_GIF
                lower.endsWith(".js") || lower.contains(".js?") -> "// Safeer AdBlock Neutralized\n".toByteArray(Charsets.UTF_8)
                else -> ByteArray(0)
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
