package com.safeer.mobile.browser.link

import android.content.Context
import com.safeer.mobile.browser.BrowserRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sinhronizacija zaznamkov med Safeer napravami prek domacega Huba.
 *
 * Pravilo zdruzevanja v tej razlicici je namenoma preprosto in varno: zaznamki se
 * ZDRUZUJEJO po naslovu. Kar ima ena naprava in druga ne, druga dobi. Brisanje se
 * NE prenasa -- naprava, ki zaznamek izbrise, ga bo ob naslednjem zdruzevanju
 * dobila nazaj. To je zavestna izbira: v prvi razlicici je bolje, da se zaznamek
 * pomotoma vrne, kot da ga sinhronizacija tiho pobrise na vseh napravah. Brisanje
 * bo mogoce, ko bomo hranili nagrobnike (zapis, da je bil zaznamek izbrisan in kdaj).
 *
 * Vsebina potuje samo po domacem omrezju in samo kadar uporabnik sinhronizacijo
 * vklopi; Hub jo hrani, da naprava, ki je bila ugasnjena, ujame ostale.
 */
object ZaznamkiSync {

    const val KATEGORIJA = "bookmarks"
    private const val PREFS = "safeer_link_prefs"
    private const val KLJUC_VKLOP = "sync_bookmarks"
    private const val KLJUC_RAZLICICA = "sync_bookmarks_version"

    fun jeVklopljena(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KLJUC_VKLOP, false)

    fun nastavi(context: Context, vklopljena: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KLJUC_VKLOP, vklopljena).apply()
    }

    fun razlicica(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KLJUC_RAZLICICA, 0L)

    fun shraniRazlicico(context: Context, razlicica: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KLJUC_RAZLICICA, razlicica).apply()
    }

    /** Zaznamki te naprave v obliki, ki potuje po omrezju. */
    fun izvozi(repozitorij: BrowserRepository): JSONObject {
        val polje = JSONArray()
        repozitorij.getBookmarks().forEach { z ->
            polje.put(JSONObject().apply {
                put("title", z.title)
                put("url", z.url)
                put("icon", z.icon)
            })
        }
        return JSONObject().put("items", polje)
    }

    /**
     * Zdruzi prejeto vsebino s tem, kar naprava ze ima. Vrne, koliko zaznamkov je bilo
     * dodanih. Obstojecih ne spreminja: naslov je kljuc, ime pa pusti tako, kot ga je
     * uporabnik shranil.
     */
    fun zdruzi(repozitorij: BrowserRepository, vsebina: JSONObject): Int {
        val polje = vsebina.optJSONArray("items") ?: return 0
        val obstojeci = repozitorij.getBookmarks().map { it.url }.toMutableSet()
        var dodanih = 0
        for (i in 0 until polje.length()) {
            val z = polje.optJSONObject(i) ?: continue
            val url = z.optString("url", "").trim()
            if (url.isEmpty()) continue
            if (!url.startsWith("http://") && !url.startsWith("https://")) continue
            if (obstojeci.contains(url)) continue
            val ime = z.optString("title", "").ifBlank { url }
            val ikona = z.optString("icon", "").ifBlank { "⭐" }
            if (repozitorij.addBookmark(ime, url, ikona)) {
                obstojeci.add(url)
                dodanih += 1
            }
        }
        return dodanih
    }

    /** Za izpis v Linku: koliko zaznamkov ima ta naprava. */
    fun stevilo(repozitorij: BrowserRepository): Int = repozitorij.getBookmarks().size

    /** Pomozno za preizkuse in izpis: seznam naslovov. */
    fun naslovi(vsebina: JSONObject): List<String> {
        val polje = vsebina.optJSONArray("items") ?: return emptyList()
        val seznam = mutableListOf<String>()
        for (i in 0 until polje.length()) {
            polje.optJSONObject(i)?.optString("url")?.let { if (it.isNotEmpty()) seznam.add(it) }
        }
        return seznam
    }
}
