package com.safeer.mobile.browser

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

/**
 * Vratar pred pojavnimi okni. Pravilo je preprosto: **nic se ne odpre, cesar uporabnik ni
 * zahteval** - ne okno, ne zavihek.
 *
 * Dosedanja zascita je preprecila samo okna brez uporabnikovega dotika (`isUserGesture`).
 * Strani s filmi pa uporabijo prav prvi klik na predvajalnik: isti klik sprozi `window.open`
 * z oglasom, zato je za Android to "uporabnik je kliknil" in okno je slo skozi. Brskalnik je
 * zavihek ustvaril takoj, se preden je vedel, kam vodi.
 *
 * Zdaj: okno dobi zacasen, skrit pogled, ki ni zavihek in ga uporabnik nikoli ne vidi.
 * Takoj ko izvemo naslov, pogled unicimo. Stevec zavihkov se ne premakne, predvajalnik pa
 * klik dobi normalno in film stece.
 *
 *   - oglas, sledilec ali znana grda domena -> tiho, brez sledi;
 *   - vse drugo                             -> spodaj se za nekaj sekund pokaze vrstica
 *                                              "Prepreceno novo okno: <naslov>" z gumbom
 *                                              Odpri, ce uporabnik to okno vseeno hoce.
 *
 * Izjeme iz seznamov (pravila `@@`) tu ne veljajo: namenjene so temu, da se ne pokvarijo
 * trgovine in banke, ne pa temu, da spustijo pojavno okno.
 */
object PopUpVratar {

    private const val TAG = "SafeerPopUp"
    private const val CAKANJE_MS = 6_000L
    private const val VRSTICA_MS = 8_000L

    /**
     * Vrne true, kadar je okno prevzel vratar (stran dobi veljaven odgovor, uporabnik pa ne
     * vidi nicesar). Klicano iz onCreateWindow na glavni niti.
     */
    fun prestrezi(
        dejavnost: Activity,
        koren: RelativeLayout,
        resultMsg: android.os.Message,
        odpriVZavihku: (String) -> Unit
    ): Boolean {
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val glavna = Handler(Looper.getMainLooper())

        val skriti = WebView(dejavnost)
        skriti.settings.javaScriptEnabled = false
        skriti.settings.loadsImagesAutomatically = false
        skriti.settings.blockNetworkLoads = true      // naslov izvemo iz zahteve, nalagati ni treba nicesar

        var odloceno = false

        fun unici() {
            try {
                skriti.stopLoading()
                skriti.webViewClient = WebViewClient()
                skriti.destroy()
            } catch (e: Throwable) {
                Log.w(TAG, "skritega pogleda ni bilo mogoce pospraviti: ${e.message}")
            }
        }

        fun odloci(naslov: String?) {
            if (odloceno) return
            if (naslov.isNullOrBlank() || !naslov.startsWith("http", ignoreCase = true)) return
            odloceno = true
            glavna.post {
                unici()
                if (jeNezazeleno(naslov)) {
                    Log.i(TAG, "Pojavno okno blokirano: $naslov")
                } else {
                    Log.i(TAG, "Pojavno okno zadrzano: $naslov")
                    pokaziVrstico(dejavnost, koren, naslov, odpriVZavihku)
                }
            }
        }

        skriti.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                odloci(request?.url?.toString())
                return true
            }

            @Deprecated("Za starejse sisteme", ReplaceWith(""))
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                odloci(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                odloci(url)
            }
        }

        transport.webView = skriti
        resultMsg.sendToTarget()

        // Ce okno nikamor ne odnavigira (prazna lupina za kasneje), ga po tiho pospravimo.
        glavna.postDelayed({
            if (!odloceno) {
                odloceno = true
                unici()
            }
        }, CAKANJE_MS)
        return true
    }

    private fun jeNezazeleno(naslov: String): Boolean = try {
        AdBlockEngine.shouldBlockUrl(naslov) || ThreatBlockEngine.isThreat(naslov)
    } catch (e: Throwable) {
        Log.w(TAG, "presoja ni uspela: ${e.message}")
        false
    }

    private fun gostitelj(naslov: String): String = try {
        android.net.Uri.parse(naslov).host?.removePrefix("www.") ?: naslov
    } catch (e: Exception) {
        naslov
    }

    /** Tanka vrstica na dnu: ne zatemni zaslona in ne prekine predvajanja. */
    private fun pokaziVrstico(
        dejavnost: Activity,
        koren: RelativeLayout,
        naslov: String,
        odpriVZavihku: (String) -> Unit
    ) {
        val gostota = dejavnost.resources.displayMetrics.density
        fun dp(v: Int) = (v * gostota).toInt()

        val vrstica = LinearLayout(dejavnost).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(8), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#F21B2420"))
                setStroke(dp(1), Color.parseColor("#3A4A42"))
            }
            elevation = dp(8).toFloat()
        }

        val besedilo = TextView(dejavnost).apply {
            text = dejavnost.getString(R.string.popup_vprasanje, gostitelj(naslov))
            setTextColor(Color.parseColor("#E6EFEA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val odstrani = {
            try { koren.removeView(vrstica) } catch (e: Throwable) { Log.w(TAG, "odstranjevanje: ${e.message}") }
        }

        fun gumb(oznaka: String, barva: String, dejanje: () -> Unit) = TextView(dejavnost).apply {
            text = oznaka
            setTextColor(Color.parseColor(barva))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { dejanje() }
        }

        vrstica.addView(besedilo)
        vrstica.addView(gumb(dejavnost.getString(R.string.popup_pokazi), "#5FE3A1") {
            odstrani()
            try { odpriVZavihku(naslov) } catch (e: Throwable) { Log.w(TAG, "odpiranje: ${e.message}") }
        })
        vrstica.addView(gumb(dejavnost.getString(R.string.popup_zapri), "#C9D6CF") { odstrani() })

        val postavitev = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            setMargins(dp(12), dp(12), dp(12), dp(16))
        }

        vrstica.visibility = View.VISIBLE
        koren.addView(vrstica, postavitev)
        Handler(Looper.getMainLooper()).postDelayed({ if (vrstica.parent != null) odstrani() }, VRSTICA_MS)
    }
}
