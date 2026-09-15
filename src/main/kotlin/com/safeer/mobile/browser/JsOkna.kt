package com.safeer.mobile.browser

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Okna JavaScripta: alert(), confirm(), prompt() in vprasanje pred zapustitvijo strani.
 *
 * Privzeti WebChromeClient teh oken ne narise, ampak jih tiho preklice. Stran, ki na
 * odgovor caka, se zato ustavi brez pojasnila -- prijave, potrditve in vnosna okna
 * enostavno ne delujejo. Brskalnik jih mora narisati sam.
 *
 * Zascite:
 *  - naslov okna vedno pove izvor strani, da se stran ne more delati za brskalnik ali sistem,
 *  - besedilo strani skrajsamo, da ne zapolni celega zaslona,
 *  - po nekaj oknih zapored ponudimo "Ne prikazuj vec", da stran ne more zakleniti brskalnika,
 *  - okna iz zavihka, ki ni viden, zavrnemo,
 *  - rezultat je vedno razresen natanko enkrat, sicer bi se stran zaklenila.
 */
object JsOkna {

    private const val NAJVEC_ZNAKOV = 2000
    private const val PRAG_ZA_UTISANJE = 3

    private var kljucStrani: String? = null
    private var stevec = 0
    private var utisano = false

    /** Ponastavi stetje (ob novi navigaciji). */
    fun ponastavi() {
        kljucStrani = null
        stevec = 0
        utisano = false
    }

    /** Besedilo iz virov; ce konteksta ni, ostane zasilni angleski zapis. */
    private fun niz(view: View?, id: Int, zasilno: String): String {
        val c = view?.context ?: return zasilno
        return try { c.getString(id) } catch (e: Throwable) { zasilno }
    }

    private fun izvor(c: Context?, url: String?): String {
        val u = try { Uri.parse(url ?: "") } catch (e: Throwable) { null }
        val gostitelj = u?.host ?: return (try { c?.getString(R.string.dialog_this_page) } catch (e: Throwable) { null }) ?: "This page"
        val shema = u.scheme ?: ""
        return if (shema == "https") gostitelj else "$shema://$gostitelj"
    }

    private fun kljuc(url: String?): String {
        val u = try { Uri.parse(url ?: "") } catch (e: Throwable) { null }
        if (u == null) return url ?: ""
        return (u.scheme ?: "") + "://" + (u.host ?: "") + (u.path ?: "")
    }

    private fun aktivnost(context: Context?): Activity? {
        var c = context
        var varovalo = 0
        while (c is ContextWrapper && varovalo < 20) {
            if (c is Activity) return c
            c = c.baseContext
            varovalo += 1
        }
        return null
    }

    /** Vrne true, kadar okno smemo pokazati; hkrati posodobi stetje. */
    private fun smemoPokazati(url: String?): Boolean {
        val k = kljuc(url)
        if (k != kljucStrani) {
            kljucStrani = k
            stevec = 0
            utisano = false
        }
        if (utisano) return false
        stevec += 1
        return true
    }

    private fun pokazi(
        view: View?,
        url: String?,
        sporocilo: String?,
        privzetoBesedilo: String?,
        jePrompt: Boolean,
        potrdiNapis: String,
        preklicNapis: String?,
        koncano: (Boolean, String?) -> Unit
    ): Boolean {
        val dejavnost = aktivnost(view?.context)
        if (view == null || dejavnost == null || dejavnost.isFinishing || !view.isShown) {
            koncano(false, null)
            return true
        }
        if (!smemoPokazati(url)) {
            koncano(false, null)
            return true
        }

        val gostota = dejavnost.resources.displayMetrics.density
        fun dp(v: Int): Int = (v * gostota).toInt()

        val vsebina = LinearLayout(dejavnost)
        vsebina.orientation = LinearLayout.VERTICAL
        vsebina.setPadding(dp(22), dp(12), dp(22), dp(4))

        val besedilo = TextView(dejavnost)
        besedilo.text = (sporocilo ?: "").take(NAJVEC_ZNAKOV)
        besedilo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        vsebina.addView(besedilo)

        var polje: EditText? = null
        if (jePrompt) {
            val e = EditText(dejavnost)
            e.setText(privzetoBesedilo ?: "")
            e.inputType = InputType.TYPE_CLASS_TEXT
            e.setSingleLine(true)
            e.setSelection(e.text?.length ?: 0)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            vsebina.addView(e, lp)
            polje = e
        }

        var utisaj: CheckBox? = null
        if (stevec >= PRAG_ZA_UTISANJE) {
            val c = CheckBox(dejavnost)
            c.text = niz(view, R.string.dialog_dont_show, "Don’t show more dialogs from this page")
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            vsebina.addView(c, lp)
            utisaj = c
        }

        var odgovorjeno = false
        fun odgovori(potrjeno: Boolean) {
            if (odgovorjeno) return
            odgovorjeno = true
            if (utisaj != null && utisaj.isChecked) utisano = true
            koncano(potrjeno, polje?.text?.toString())
        }

        try {
            val graditelj = AlertDialog.Builder(dejavnost)
                .setTitle(izvor(dejavnost, url))
                .setView(vsebina)
                .setCancelable(true)
                .setPositiveButton(potrdiNapis) { _, _ -> odgovori(true) }
                .setOnCancelListener { odgovori(false) }
                .setOnDismissListener { odgovori(false) }
            if (preklicNapis != null) {
                graditelj.setNegativeButton(preklicNapis) { _, _ -> odgovori(false) }
            }
            val okno = graditelj.create()
            okno.show()
            polje?.requestFocus()
        } catch (e: Throwable) {
            odgovori(false)
        }
        return true
    }

    fun alert(view: WebView?, url: String?, sporocilo: String?, rezultat: JsResult?): Boolean {
        if (rezultat == null) return false
        return pokazi(view, url, sporocilo, null, false, niz(view, R.string.dialog_ok, "OK"), null) { _, _ ->
            rezultat.confirm()
        }
    }

    fun confirm(view: WebView?, url: String?, sporocilo: String?, rezultat: JsResult?): Boolean {
        if (rezultat == null) return false
        return pokazi(view, url, sporocilo, null, false, niz(view, R.string.dialog_ok, "OK"), niz(view, R.string.dialog_cancel, "Cancel")) { potrjeno, _ ->
            if (potrjeno) rezultat.confirm() else rezultat.cancel()
        }
    }

    fun prompt(
        view: WebView?,
        url: String?,
        sporocilo: String?,
        privzeto: String?,
        rezultat: JsPromptResult?
    ): Boolean {
        if (rezultat == null) return false
        return pokazi(view, url, sporocilo, privzeto, true, niz(view, R.string.dialog_ok, "OK"), niz(view, R.string.dialog_cancel, "Cancel")) { potrjeno, besedilo ->
            if (potrjeno) rezultat.confirm(besedilo ?: "") else rezultat.cancel()
        }
    }

    fun predZapustitvijo(
        view: WebView?,
        url: String?,
        sporocilo: String?,
        rezultat: JsResult?
    ): Boolean {
        if (rezultat == null) return false
        val besedilo = if (sporocilo.isNullOrBlank()) {
            niz(view, R.string.dialog_leave_msg, "The page asks whether you really want to leave it. Unsaved entries will be lost.")
        } else {
            sporocilo
        }
        return pokazi(view, url, besedilo, null, false, niz(view, R.string.dialog_leave_page, "Leave page"), niz(view, R.string.dialog_stay, "Stay")) { potrjeno, _ ->
            if (potrjeno) rezultat.confirm() else rezultat.cancel()
        }
    }
}
