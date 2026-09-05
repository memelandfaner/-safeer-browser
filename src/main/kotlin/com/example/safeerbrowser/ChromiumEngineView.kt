package com.example.safeerbrowser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.*

@SuppressLint("SetJavaScriptEnabled")
class ChromiumEngineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    companion object {
        const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 15; SM-S931B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
        val PRIVACY_HEADERS = mapOf(
            "Sec-GPC" to "1",
            "DNT" to "1"
        )
    }

    var isDesktopMode: Boolean = false
        set(value) {
            field = value
            settings.userAgentString = if (value) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
            settings.useWideViewPort = value
            settings.loadWithOverviewMode = value
        }

    var isDarkMode: Boolean = true

    var onProgressUpdate: ((Int) -> Unit)? = null
    var onUrlChanged: ((String) -> Unit)? = null
    var onTitleChanged: ((String) -> Unit)? = null
    var onSecurityChanged: ((Boolean) -> Unit)? = null
    var onPageLoaded: ((String, String) -> Unit)? = null
    var onFullscreenToggled: ((View?, WebChromeClient.CustomViewCallback?) -> Unit)? = null

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    init {
        setupSettings()
        setupClients()
    }

    fun applyDarkMode(enable: Boolean) {
        isDarkMode = enable
        UserScriptManager.injectDarkModeToggle(this, enable)
    }

    private fun setupSettings() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        try {
            val isDebug = (0 != (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE))
            WebView.setWebContentsDebuggingEnabled(isDebug)
        } catch (_: Exception) {}

        // 🔊 100% Native Strojni Vklop Zvoka (Unmute STREAM_MUSIC)
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false)
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Blokada sledilnih piškotkov tretjih oseb za maksimalno zasebnost
            cm.setAcceptThirdPartyCookies(this, false)
        }

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = isDesktopMode
            loadWithOverviewMode = isDesktopMode
            
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        }

        addJavascriptInterface(SafeerWebAppInterface(context, this), "SafeerBridge")

        isFocusable = true
        isFocusableInTouchMode = true
    }

    class SafeerWebAppInterface(private val context: Context, private val webView: WebView) {
        @android.webkit.JavascriptInterface
        fun getStats(): String {
            val ads = AdBlockEngine.blockedAdsCount.get()
            val threats = ThreatBlockEngine.totalBlockedThreats.get()
            val dataMb = String.format(java.util.Locale.US, "%.1f", (ads * 140L + threats * 220L) / 1024.0 / 1024.0)
            val timeMin = String.format(java.util.Locale.US, "%.1f", (ads * 1.4 + threats * 2.0) / 60.0)
            return "{\"ads\": $ads, \"threats\": $threats, \"dataMb\": \"$dataMb MB\", \"timeMin\": \"$timeMin min\"}"
        }

        @android.webkit.JavascriptInterface
        fun navigate(url: String) {
            val cur = webView.url ?: ""
            val isLocal = cur.startsWith("file:///android_asset/") || cur.startsWith("safeer://") || cur.isEmpty()
            if (!isLocal) {
                android.util.Log.w("SafeerBridge", "Zavrnjen neavtoriziran klic navigate() iz zunanje strani: $cur")
                return
            }
            (context as? android.app.Activity)?.runOnUiThread {
                (webView as? ChromiumEngineView)?.navigateDocument(url) ?: webView.loadUrl(url)
            }
        }
    }

    override fun loadUrl(url: String) {
        val sanitized = UrlSanitizer.sanitize(url)
        if (sanitized.startsWith("http://", ignoreCase = true) || sanitized.startsWith("https://", ignoreCase = true)) {
            super.loadUrl(sanitized, PRIVACY_HEADERS)
        } else {
            super.loadUrl(sanitized)
        }
    }

    override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
        val sanitized = UrlSanitizer.sanitize(url)
        val combined = additionalHttpHeaders.toMutableMap()
        if (!combined.containsKey("Sec-GPC")) combined["Sec-GPC"] = "1"
        if (!combined.containsKey("DNT")) combined["DNT"] = "1"
        super.loadUrl(sanitized, combined)
    }

    /**
     * Odpri URL kot nov dokument. Na YouTube nikoli ne uporabi location.replace —
     * SPA sicer obdrži stari predvajalnik in predvaja napačen video.
     */
    fun navigateDocument(url: String) {
        val sanitized = UrlSanitizer.sanitize(url)
        val target = normalizeExternalUrl(sanitized)
        stopLoading()
        evaluateJavascript(
            """
            (function(){
                try { window._safeer_yt_agent_installed = false; } catch (e1) {}
                try {
                    var media = document.querySelectorAll('video,audio');
                    for (var i = 0; i < media.length; i++) {
                        try { media[i].pause(); media[i].removeAttribute('src'); media[i].src = ''; media[i].load(); } catch (e2) {}
                    }
                } catch (e3) {}
            })();
            """.trimIndent(),
            null
        )
        post {
            val current = this.url ?: ""
            val currentId = youtubeVideoId(current)
            val targetId = youtubeVideoId(target)
            val sameWatch = !currentId.isNullOrEmpty() && currentId == targetId &&
                youtubeListId(current) == youtubeListId(target) &&
                current.contains("youtube", ignoreCase = true)
            if (sameWatch) {
                reload()
            } else {
                loadUrl(target)
            }
        }
    }

    private fun normalizeExternalUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return url
            val isYt = host.contains("youtube.com") || host.contains("youtu.be")
            if (!isYt) return url
            val path = uri.path ?: ""
            val shortsMatch = Regex("/shorts/([A-Za-z0-9_-]{6,})").find(path)
            val videoId = when {
                host.contains("youtu.be") -> path.trim('/').substringBefore('/')
                !uri.getQueryParameter("v").isNullOrEmpty() -> uri.getQueryParameter("v")
                shortsMatch != null -> shortsMatch.groupValues[1]
                else -> null
            }
            if (videoId.isNullOrEmpty()) return url
            val base = if (isDesktopMode) "https://www.youtube.com" else "https://m.youtube.com"
            if (path.contains("/shorts/") && shortsMatch != null) {
                return "$base/shorts/$videoId"
            }
            val b = Uri.parse("$base/watch?v=$videoId").buildUpon()
            for (key in listOf("list", "start_radio", "index", "t", "time_continue", "radio", "pp", "playnext")) {
                val value = uri.getQueryParameter(key)
                if (!value.isNullOrEmpty()) b.appendQueryParameter(key, value)
            }
            b.build().toString()
        } catch (_: Exception) {
            url
        }
    }

    private fun youtubeListId(url: String): String? {
        return try {
            Uri.parse(url).getQueryParameter("list")
        } catch (_: Exception) {
            null
        }
    }

    private fun youtubeVideoId(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return null
            if (host.contains("youtu.be")) {
                uri.path?.trim('/')?.substringBefore('/')?.takeIf { it.length >= 6 }
            } else {
                uri.getQueryParameter("v")
                    ?: Regex("/shorts/([A-Za-z0-9_-]{6,})").find(uri.path ?: "")?.groupValues?.get(1)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun setupClients() {
        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                // 🛑 Popolna zaščita pred pojavnimi okni in ugrabitvijo oken
                return false
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressUpdate?.invoke(newProgress)
                if (newProgress in 20..60) {
                    view?.let { UserScriptManager.injectEarlyScript(it, isDesktopMode) }
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrEmpty()) {
                    onTitleChanged?.invoke(title)
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
                onFullscreenToggled?.invoke(view, callback)
            }

            override fun onHideCustomView() {
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                onFullscreenToggled?.invoke(null, null)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (callback == null) return
                val targetOrigin = origin ?: ""
                if (targetOrigin.isEmpty()) {
                    callback.invoke(origin, false, false)
                    return
                }

                val act = context as? android.app.Activity
                if (act == null || act.isFinishing || act.isDestroyed) {
                    callback.invoke(origin, false, false)
                    return
                }

                act.runOnUiThread {
                    try {
                        android.app.AlertDialog.Builder(context)
                            .setTitle("Zahteva za lokacijo")
                            .setMessage("Spletno mesto '$targetOrigin' želi dostop do vaše trenutne geografske lokacije.\n\nAli dovolite dostop?")
                            .setPositiveButton("Dovoli") { _, _ ->
                                callback.invoke(origin, true, false)
                            }
                            .setNegativeButton("Zavrni") { _, _ ->
                                callback.invoke(origin, false, false)
                            }
                            .setOnCancelListener {
                                callback.invoke(origin, false, false)
                            }
                            .show()
                    } catch (_: Exception) {
                        callback.invoke(origin, false, false)
                    }
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val act = context as? android.app.Activity
                if (act == null || act.isFinishing || act.isDestroyed) {
                    request.deny()
                    return
                }

                val resources = request.resources ?: emptyArray()
                val host = request.origin?.host ?: request.origin?.toString() ?: "Spletna stran"

                // Prijazna imena zahtevanih dovoljenj
                val labels = resources.map { res ->
                    when (res) {
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "🎤 Mikrofon (zvok)"
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "📷 Kamera (video)"
                        PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> "🔑 Zaščitena medijska vsebina (DRM)"
                        else -> res.substringAfterLast(".")
                    }
                }.joinToString("\n• ", prefix = "• ")

                act.runOnUiThread {
                    try {
                        android.app.AlertDialog.Builder(context)
                            .setTitle("Zahteva za dovoljenje")
                            .setMessage("Spletno mesto '$host' želi dostop do naslednjih virov naprave:\n\n$labels\n\nAli dovolite dostop?")
                            .setPositiveButton("Dovoli") { _, _ ->
                                request.grant(resources)
                            }
                            .setNegativeButton("Zavrni") { _, _ ->
                                request.deny()
                            }
                            .setOnCancelListener {
                                request.deny()
                            }
                            .show()
                    } catch (_: Exception) {
                        request.deny()
                    }
                }
            }

            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                val msg = consoleMessage?.message() ?: ""
                val line = consoleMessage?.lineNumber() ?: 0
                val src = consoleMessage?.sourceId() ?: ""
                android.util.Log.d("SafeerConsole", "[$src:$line] $msg")
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val urlStr = uri.toString()
                val isMainFrame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.isForMainFrame
                } else {
                    true
                }

                // 1. Odklep nevarne domene na lastno odgovornost z enokratnim varnostnim žetonom
                if (urlStr.startsWith("safeer://bypass-threat", ignoreCase = true)) {
                    val token = uri.getQueryParameter("token") ?: ""
                    val bypass = ThreatBlockEngine.consumeBypassToken(token)
                    if (bypass != null) {
                        ThreatBlockEngine.allowForSession(bypass.domain)
                        view?.loadUrl(bypass.targetUrl)
                    } else {
                        android.util.Log.w("SafeerSecurity", "Zavrnjen neveljaven ali potekel bypass token.")
                    }
                    return true
                }

                // 2. Blokiraj le resnične botnet/malware grožnje in znane oglasne domene
                val host = uri.host?.lowercase()?.trim() ?: ""
                if (ThreatBlockEngine.isThreat(urlStr)) {
                    view?.let { wv ->
                        val match = ThreatBlockEngine.checkThreat(urlStr)
                        if (match != null) {
                            val html = ThreatBlockEngine.createSecurityInterstitialHtml(urlStr, match)
                            wv.loadDataWithBaseURL("https://$host", html, "text/html", "UTF-8", null)
                        }
                    }
                    return true
                }

                // Blokiraj klik na znana oglasna omrežja (popunderji)
                if (AdBlockEngine.shouldBlockUrl(urlStr)) {
                    return true
                }

                // 3. Odpri posebne sheme v ustreznih aplikacijah
                val scheme = uri.scheme?.lowercase() ?: ""
                if (scheme != "http" && scheme != "https" && scheme != "file" && scheme != "about") {
                    try {
                        val intent = if (urlStr.startsWith("intent:", ignoreCase = true)) {
                            Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME)
                        } else {
                            Intent(Intent.ACTION_VIEW, uri)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            if (urlStr.startsWith("intent:", ignoreCase = true)) {
                                val parsed = Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME)
                                val fallbackUrl = parsed.getStringExtra("browser_fallback_url")
                                if (!fallbackUrl.isNullOrEmpty()) {
                                    view?.loadUrl(fallbackUrl)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    return true
                }

                // 4. Kirurško čiščenje sledilnih parametrov (Query Tracker Stripping) ob kliku na povezavo
                val method = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.method ?: "GET"
                } else {
                    "GET"
                }
                if (isMainFrame && !method.equals("POST", ignoreCase = true) && (scheme == "http" || scheme == "https")) {
                    val sanitized = UrlSanitizer.sanitize(urlStr)
                    if (sanitized != urlStr) {
                        view?.loadUrl(sanitized)
                        return true
                    }
                }

                // Za vsa legitimna spletna mesta dovoli normalno odpiranje
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                val isMainFrame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.isForMainFrame
                } else {
                    false
                }

                // 🛑 1. Brezkompromisni Threat Shield (Botnet C2, Malware, Phishing, IOC)
                val threatResponse = ThreatBlockEngine.handleThreatIntercept(url, isMainFrame)
                if (threatResponse != null) {
                    return threatResponse
                }

                // ⚡ 2. Napredni AdBlock & Sledilci (Suffix Trie, Streaming Guard & Path Rules)
                return AdBlockEngine.handleIntercept(url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let {
                    android.util.Log.d("SafeerNav", "start $it")
                    onUrlChanged?.invoke(it)
                    onSecurityChanged?.invoke(it.startsWith("https://", ignoreCase = true))
                    view?.let { wv ->
                        UserScriptManager.injectEarlyScript(wv, isDesktopMode)
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    android.util.Log.d("SafeerNav", "finish $it")
                    onUrlChanged?.invoke(it)
                    onSecurityChanged?.invoke(it.startsWith("https://", ignoreCase = true))
                    val pageTitle = title ?: ""
                    onPageLoaded?.invoke(it, pageTitle)
                    view?.let { wv ->
                        UserScriptManager.injectOnPageFinished(wv, isDarkMode, isDesktopMode)
                    }
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                url?.let {
                    android.util.Log.d("SafeerNav", "hist $it")
                    onUrlChanged?.invoke(it)
                    onSecurityChanged?.invoke(it.startsWith("https://", ignoreCase = true))
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                onSecurityChanged?.invoke(false)
                handler?.cancel()

                val act = context as? android.app.Activity
                act?.runOnUiThread {
                    try {
                        val host = error?.url?.let {
                            try { java.net.URI(it).host } catch (_: Exception) { null }
                        } ?: "To spletno mesto"

                        android.app.AlertDialog.Builder(context)
                            .setTitle("Varnostno opozorilo (SSL)")
                            .setMessage("Varna povezava z '$host' ni mogoča, ker je varnostni certifikat neveljaven ali potekel.\n\nDostop je bil zaradi zaščite vaših podatkov prekinjen.")
                            .setPositiveButton("V redu", null)
                            .show()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun isFullscreenVideoActive(): Boolean = customView != null

    fun exitFullscreenVideo() {
        webChromeClient?.onHideCustomView()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(View.VISIBLE)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, View.VISIBLE)
    }

    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        super.dispatchWindowVisibilityChanged(View.VISIBLE)
    }
}
