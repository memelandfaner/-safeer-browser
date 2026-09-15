package com.safeer.mobile.browser.link

import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.safeer.mobile.browser.cast.CastSenderClient
import com.safeer.mobile.browser.cast.HubDiscovery
import com.safeer.mobile.browser.cast.HubPairing
import org.json.JSONArray
import org.json.JSONObject

/**
 * Most med stranjo Safeer Linka in aplikacijo.
 *
 * Stran Safeer Linka je del aplikacije (assets/link/), ne prihaja z omrezja. Most je
 * pripet SAMO na njen pogled -- nikoli na zavihek, v katerem se odpirajo spletne strani --
 * zato do teh metod nobena spletna stran ne more.
 *
 * Zeton naprave ostane v zasebnih nastavitvah aplikacije: stran ga nikoli ne vidi in ga
 * ne more prebrati. Vsak klic proti Hubu opravi most, stran pove samo, kaj zeli.
 *
 * Odgovori se vracajo z `window.safeerLinkOdziv(vrsta, podatki)`, ker klici mostu pridejo
 * z druge niti in ne smejo cakati na omrezje.
 */
class LinkMost(
    private val dejavnost: Activity,
    private val pogled: WebView,
    private val trenutnaStran: () -> Pair<String, String?>,
    private val zapriZaslon: () -> Unit,
    private val odpriVBrskalniku: (String) -> Unit
) {

    companion object {
        private const val TAG = "SafeerLink"
        const val PREFS = "safeer_cast_prefs"
    }

    private var odjemalec: CastSenderClient? = null
    private var zadnjeNaprave: JSONArray = JSONArray()

    // ------------------------------------------------------------------
    // Pomozno
    // ------------------------------------------------------------------

    private fun nastavitve() = dejavnost.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun hubUrl(): String = nastavitve().getString("hub_url", "") ?: ""

    private fun zeton(): String? = nastavitve().getString("control_token", null)

    private fun potVstopnice(): String =
        nastavitve().getString("hub_ticket_path", "/cast/ticket") ?: "/cast/ticket"

    /** Odgovor strani. Vedno na glavni niti in vedno kot JSON, da stran ne sestavlja nizov. */
    private fun odziv(vrsta: String, podatki: Any) {
        val telo = when (podatki) {
            is JSONObject, is JSONArray -> podatki.toString()
            is Boolean -> podatki.toString()
            else -> JSONObject.quote(podatki.toString())
        }
        pogled.post {
            try {
                pogled.evaluateJavascript(
                    "window.safeerLinkOdziv && window.safeerLinkOdziv(${JSONObject.quote(vrsta)}, $telo)",
                    null
                )
            } catch (e: Throwable) {
                android.util.Log.w(TAG, "Odziva ni bilo mogoce dostaviti: ${e.message}")
            }
        }
    }

    /**
     * Napaka za stran. [koda] je stabilna oznaka, ki jo stran prevede v jezik naprave;
     * [sporocilo] ostane zraven kot rezerva in za diagnostiko, ce kode ne pozna.
     */
    private fun napaka(koda: String, sporocilo: String) = odziv(
        "napaka",
        JSONObject().put("koda", koda).put("sporocilo", sporocilo)
    )

    // ------------------------------------------------------------------
    // Stanje
    // ------------------------------------------------------------------

    /**
     * Stanje brez omrezja: ali Hub poznamo, ali nas ze pozna in kako se ta naprava imenuje.
     * Stran tako nariše pravi zaslon takoj, brez cakanja.
     */

    /**
     * Jezik, ki ga ima uporabnik na napravi -- stran govori v njem.
     * Vrnemo samo dvocrkovno oznako; stran zna slovensko in anglesko.
     */
    @JavascriptInterface
    fun jezik(): String = try {
        val jeziki = dejavnost.resources.configuration.locales
        val prvi = if (jeziki.size() > 0) jeziki.get(0) else java.util.Locale.getDefault()
        (prvi.language ?: "").lowercase().take(2)
    } catch (e: Throwable) {
        ""
    }

    /**
     * Odklopi TO napravo od Safeer Linka: pozabi zeton in naslov.
     *
     * Namenoma ne posegamo v druge naprave -- to je odlocitev za napravo, ki jo ima
     * uporabnik v roki. Ostale se odstrani v Safeer Controlu.
     */
    @JavascriptInterface
    fun pozabiNapravo() {
        try {
            odjemalec?.disconnect()
        } catch (e: Throwable) {
            android.util.Log.w(TAG, "Zapiranja povezave ni bilo mogoce dokoncati: ${e.message}")
        }
        odjemalec = null
        try {
            nastavitve().edit()
                .remove("control_token")
                .remove("hub_url")
                .remove("hub_ticket_path")
                .remove("hub_last_seen")
                .apply()
        } catch (e: Throwable) {
            android.util.Log.w(TAG, "Nastavitev ni bilo mogoce pocistiti: ${e.message}")
        }
        odziv("pozabljeno", true)
    }

    @JavascriptInterface
    fun stanje(): String {
        return try {
            JSONObject().apply {
                put("hub", hubUrl())
                put("znan", hubUrl().isNotBlank())
                put("seznanjen", zeton() != null)
                put("naprava", "Safeer (" + android.os.Build.MODEL + ")")
                put("id", ime())
                put("videnZadnjic", nastavitve().getLong("hub_last_seen", 0L))
            }.toString()
        } catch (e: Throwable) {
            "{\"znan\":false,\"seznanjen\":false}"
        }
    }

    private fun ime(): String =
        "phone-" + android.os.Build.MODEL.replace(Regex("\\s+"), "-").lowercase()

    // ------------------------------------------------------------------
    // Hub: iskanje in seznanitev
    // ------------------------------------------------------------------

    /** Poisce Hub v krajevnem omrezju. Ce ga ni, stran to pove mirno -- brskalnik dela naprej. */
    @JavascriptInterface
    fun poisciHub() {
        try {
            HubDiscovery.discover(dejavnost) { naslov ->
                odziv("hub", JSONObject().apply {
                    put("najden", naslov != null)
                    put("naslov", naslov ?: "")
                })
            }
        } catch (e: Throwable) {
            napaka("iskanje_ni_steklo", "Iskanja ni bilo mogoce zagnati: ${e.message}")
        }
    }

    /** Zacne seznanitev: koda se pokaze v strani, potrdi pa se v Safeer Controlu. */
    @JavascriptInterface
    fun seznani() {
        val naslov = hubUrl()
        if (naslov.isBlank()) {
            napaka("hub_ni_znan", "Hub ni znan. Najprej ga poisci.")
            return
        }
        try {
            HubPairing.pair(
                dejavnost, naslov, ime(), "Safeer (" + android.os.Build.MODEL + ")",
                { koda -> odziv("koda", koda) },
                { uspelo -> odziv("seznanitev", uspelo) }
            )
        } catch (e: Throwable) {
            napaka("seznanitev_ni_stekla", "Seznanitve ni bilo mogoce zaceti: ${e.message}")
        }
    }

    // ------------------------------------------------------------------
    // Naprave in Cast
    // ------------------------------------------------------------------

    private fun odjemalec(): CastSenderClient? {
        val naslov = hubUrl()
        if (naslov.isBlank()) return null
        odjemalec?.let { return it }
        val nov = CastSenderClient(
            naslov, zeton(), potVstopnice(),
            sinhronizira = ZaznamkiSync.jeVklopljena(dejavnost)
        )
        nov.onSyncData = { kategorija, razlicica, _, vsebina ->
            if (kategorija == ZaznamkiSync.KATEGORIJA && ZaznamkiSync.jeVklopljena(dejavnost)) {
                // Zdruzevanje odpre bazo, zato ne na glavni niti.
                Thread {
                    try {
                        val repo = com.safeer.mobile.browser.BrowserRepository(dejavnost)
                        val dodanih = ZaznamkiSync.zdruzi(repo, vsebina)
                        if (razlicica > ZaznamkiSync.razlicica(dejavnost)) {
                            ZaznamkiSync.shraniRazlicico(dejavnost, razlicica)
                        }
                        odziv("sinhronizacija", JSONObject().apply {
                            put("kategorija", kategorija)
                            put("dodanih", dodanih)
                            put("skupaj", ZaznamkiSync.stevilo(repo))
                            put("vklopljena", true)
                        })
                    } catch (e: Throwable) {
                        napaka("zdruzevanje_ni_koncano", "Zdruzevanja zaznamkov ni bilo mogoce koncati: ${e.message}")
                    }
                }.start()
            }
        }
        nov.onDevicesChanged = { seznam ->
            val polje = JSONArray()
            seznam.forEach { n ->
                polje.put(JSONObject().apply {
                    put("id", n.id)
                    put("ime", n.name)
                    put("vloga", n.role)
                    put("zmoznosti", JSONArray(n.capabilities))
                })
            }
            zadnjeNaprave = polje
            odziv("naprave", polje)
        }
        nov.onPlaybackStatus = { s ->
            odziv("predvajanje", JSONObject().apply {
                put("naprava", s.deviceId)
                put("stanje", s.state)
                put("naslov", s.title ?: "")
                put("url", s.currentUrl ?: "")
                put("polozaj", s.position)
                put("trajanje", s.duration)
            })
        }
        nov.onConnectedStateChanged = { povezan -> odziv("povezava", povezan) }
        odjemalec = nov
        return nov
    }

    /** Povezi se s Hubom in vrni seznam naprav. */
    @JavascriptInterface
    fun poveziSe() {
        val o = odjemalec()
        if (o == null) {
            napaka("hub_ni_znan", "Hub ni znan.")
            return
        }
        try {
            o.connect()
        } catch (e: Throwable) {
            napaka("povezava_ni_uspela", "Povezava ni uspela: ${e.message}")
        }
    }

    /** Zadnji znani seznam naprav brez novega klica (za takojsen izris). */
    @JavascriptInterface
    fun naprave(): String = zadnjeNaprave.toString()

    /** Naslov in ime strani, ki je odprta v brskalniku -- to Link ponudi za posiljanje. */
    @JavascriptInterface
    fun trenutnaStranJson(): String {
        return try {
            val (url, naslov) = trenutnaStran()
            JSONObject().apply {
                put("url", url)
                put("naslov", naslov ?: "")
                put("posljiva", url.isNotBlank() && !url.startsWith("file:///android_asset/"))
            }.toString()
        } catch (e: Throwable) {
            "{\"url\":\"\",\"naslov\":\"\",\"posljiva\":false}"
        }
    }

    /** Poslje trenutno odprto stran na izbrano napravo. */
    @JavascriptInterface
    fun posljiTrenutno(idNaprave: String) {
        val (url, naslov) = trenutnaStran()
        if (url.isBlank() || url.startsWith("file:///android_asset/")) {
            napaka("stran_ni_primerna", "Ta stran ni primerna za posiljanje.")
            return
        }
        poslji(idNaprave, url, naslov ?: "")
    }

    /** Poslje poljuben naslov. Stran ga sme podati, ker jo je napisal uporabnik sam. */
    @JavascriptInterface
    fun poslji(idNaprave: String, url: String, naslov: String) {
        val o = odjemalec()
        if (o == null) {
            napaka("hub_ni_znan", "Hub ni znan.")
            return
        }
        val cist = url.trim()
        if (!cist.startsWith("http://") && !cist.startsWith("https://")) {
            napaka("samo_http", "Poslati je mogoce samo naslove http in https.")
            return
        }
        try {
            o.sendUrl(idNaprave, cist, naslov.ifBlank { null })
            odziv("poslano", JSONObject().apply {
                put("naprava", idNaprave)
                put("url", cist)
            })
        } catch (e: Throwable) {
            napaka("posiljanje_ni_uspelo", "Posiljanje ni uspelo: ${e.message}")
        }
    }

    /** Predvajanje: pause, play, seek, volume, stop -- kar Hub dovoli. */
    @JavascriptInterface
    fun nadzor(idNaprave: String, ukaz: String, vrednost: Double) {
        val o = odjemalec()
        if (o == null) {
            napaka("hub_ni_znan", "Hub ni znan.")
            return
        }
        try {
            when (ukaz) {
                "seek" -> o.sendControl(idNaprave, "seek", position = vrednost)
                "volume" -> o.sendControl(idNaprave, "volume", volume = vrednost)
                else -> o.sendControl(idNaprave, ukaz)
            }
        } catch (e: Throwable) {
            napaka("ukaz_ni_uspel", "Ukaz ni uspel: ${e.message}")
        }
    }

    // ------------------------------------------------------------------
    // Okno
    // ------------------------------------------------------------------

    /** Zapre Safeer Link in prekine povezavo -- brez odprte povezave ni prometa. */
    @JavascriptInterface
    fun zapri() {
        try { odjemalec?.disconnect() } catch (_: Throwable) {}
        odjemalec = null
        dejavnost.runOnUiThread { zapriZaslon() }
    }

    /** Odpre naslov v navadnem zavihku (npr. konzolo Safeer Controla). */
    @JavascriptInterface
    fun odpri(url: String) {
        val cist = url.trim()
        if (!cist.startsWith("http://") && !cist.startsWith("https://")) return
        dejavnost.runOnUiThread {
            zapriZaslon()
            odpriVBrskalniku(cist)
        }
    }

    // ------------------------------------------------------------------
    // Sinhronizacija zaznamkov
    // ------------------------------------------------------------------

    /** Stanje sinhronizacije za izris; brez omrezja. */
    @JavascriptInterface
    fun sinhronizacijaStanje(): String {
        return try {
            val repo = com.safeer.mobile.browser.BrowserRepository(dejavnost)
            JSONObject().apply {
                put("zaznamki", JSONObject().apply {
                    put("vklopljena", ZaznamkiSync.jeVklopljena(dejavnost))
                    put("stevilo", ZaznamkiSync.stevilo(repo))
                })
            }.toString()
        } catch (e: Throwable) {
            "{\"zaznamki\":{\"vklopljena\":false,\"stevilo\":0}}"
        }
    }

    /**
     * Vklopi ali izklopi sinhronizacijo zaznamkov.
     *
     * Ob vklopu naprava najprej vprasa Hub, kaj ze hrani, in sele nato poslje svoje --
     * tako se stanji zdruzita in nihce ne izgubi zaznamka. Ker se zmoznost "sync"
     * prijavi ob povezavi, se moramo povezati znova.
     */
    @JavascriptInterface
    fun nastaviSinhronizacijo(vklopljena: Boolean) {
        try {
            ZaznamkiSync.nastavi(dejavnost, vklopljena)
            try { odjemalec?.disconnect() } catch (_: Throwable) {}
            odjemalec = null
            if (!vklopljena) {
                odziv("sinhronizacija", JSONObject().apply {
                    put("kategorija", ZaznamkiSync.KATEGORIJA)
                    put("vklopljena", false)
                    put("dodanih", 0)
                })
                poveziSe()
                return
            }
            val o = odjemalec() ?: run { napaka("hub_ni_znan", "Hub ni znan."); return }
            o.connect()
            // Povezava potrebuje trenutek; sele nato ima smisel karkoli poslati.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    o.zahtevajSinhronizacijo(ZaznamkiSync.KATEGORIJA)
                    Thread {
                        try {
                            val repo = com.safeer.mobile.browser.BrowserRepository(dejavnost)
                            val vsebina = ZaznamkiSync.izvozi(repo)
                            val nova = ZaznamkiSync.razlicica(dejavnost) + 1
                            ZaznamkiSync.shraniRazlicico(dejavnost, nova)
                            o.posljiSinhronizacijo(ZaznamkiSync.KATEGORIJA, nova, vsebina)
                            odziv("sinhronizacija", JSONObject().apply {
                                put("kategorija", ZaznamkiSync.KATEGORIJA)
                                put("vklopljena", true)
                                put("dodanih", 0)
                                put("skupaj", ZaznamkiSync.stevilo(repo))
                            })
                        } catch (e: Throwable) {
                            napaka("zaznamki_niso_poslani", "Zaznamkov ni bilo mogoce poslati: ${e.message}")
                        }
                    }.start()
                } catch (e: Throwable) {
                    napaka("sync_ni_stekla", "Sinhronizacije ni bilo mogoce zaceti: ${e.message}")
                }
            }, 1500L)
        } catch (e: Throwable) {
            napaka("sync_ni_nastavljena", "Sinhronizacije ni bilo mogoce nastaviti: ${e.message}")
        }
    }

    /** Ali tece na televizorju. Televizor je zaslon in nima komu posiljati. */
    @JavascriptInterface
    fun jeTelevizor(): Boolean = false

    /** Naslov konzole Safeer Controla, izpeljan iz naslova Huba. */

    /** Ob zaprtju zaslona pospravi povezavo. */
    fun pospravi() {
        try { odjemalec?.disconnect() } catch (_: Throwable) {}
        odjemalec = null
    }
}
