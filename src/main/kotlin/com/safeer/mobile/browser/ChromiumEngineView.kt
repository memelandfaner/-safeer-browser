package com.safeer.mobile.browser

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
import java.io.ByteArrayInputStream

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
    var onPermissionRequested: ((PermissionRequest) -> Unit)? = null
    var onGeolocationRequested: ((String, GeolocationPermissions.Callback) -> Unit)? = null
    var onCreateWindowRequested: ((isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message) -> Boolean)? = null
    var onCloseWindowRequested: (() -> Unit)? = null

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
            // Omogoči za nemoteno prijavo (OAuth 2.0, Google Sign-In, bančništvo)
            cm.setAcceptThirdPartyCookies(this, true)
        }

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(true)
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
            val totalSavedKb = (ads * 45L) + (threats * 120L)
            val dataMb = if (totalSavedKb >= 1024) {
                String.format(java.util.Locale.US, "%.1f MB", totalSavedKb / 1024.0)
            } else {
                "$totalSavedKb KB"
            }
            val totalSec = (ads * 1.0) + (threats * 1.5)
            val timeMin = if (totalSec >= 60) {
                String.format(java.util.Locale.US, "%.1f min", totalSec / 60.0)
            } else {
                String.format(java.util.Locale.US, "%.0f s", totalSec)
            }
            return "{\"ads\": $ads, \"threats\": $threats, \"dataMb\": \"$dataMb\", \"timeMin\": \"$timeMin\"}"
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
                if (resultMsg == null) return false
                if (!isUserGesture) return false // Popolna zaščita pred samodejnimi popunderji brez uporabniškega klika
                val handler = onCreateWindowRequested
                if (handler != null) {
                    return handler.invoke(isDialog, isUserGesture, resultMsg)
                }
                return false
            }

            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                onCloseWindowRequested?.invoke()
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

                if (onGeolocationRequested != null) {
                    onGeolocationRequested?.invoke(targetOrigin, callback)
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
                if (onPermissionRequested != null) {
                    onPermissionRequested?.invoke(request)
                    return
                }

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

                // 2. Blokiraj le resnične botnet/malware grožnje
                if (ThreatBlockEngine.isThreat(urlStr)) {
                    view?.let { wv ->
                        val match = ThreatBlockEngine.checkThreat(urlStr)
                        if (match != null) {
                            val html = ThreatBlockEngine.createSecurityInterstitialHtml(urlStr, match)
                            wv.loadDataWithBaseURL("safeer://security-interstitial", html, "text/html", "UTF-8", null)
                        }
                    }
                    return true
                }

                // 3. Odpri posebne zunanje sheme v ustreznih aplikacijah z zaščito pred ugrabitvijo Intentov
                val scheme = uri.scheme?.lowercase() ?: ""
                if (scheme != "http" && scheme != "https" && scheme != "file" && scheme != "about" && scheme != "safeer") {
                    try {
                        val intent = if (urlStr.startsWith("intent:", ignoreCase = true)) {
                            val parsed = Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME)
                            parsed.addCategory(Intent.CATEGORY_BROWSABLE)
                            parsed.component = null
                            parsed.selector = null
                            parsed
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
                val adResponse = AdBlockEngine.handleIntercept(url)
                if (adResponse != null) {
                    return adResponse
                }

                // 🎬 3. YouTube Document-Start Injekcija (0 oglasov pred zagonom videa)
                if (isMainFrame && isYouTubeHtmlDocument(url)) {
                    val interceptedYt = interceptAndSanitizeYouTubeWatch(url, settings.userAgentString)
                    if (interceptedYt != null) {
                        return interceptedYt
                    }
                }

                return null
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val isMain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request?.isForMainFrame == true
                } else {
                    true
                }

                if (isMain) {
                    val failingUrl = request?.url?.toString() ?: ""
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        when (error?.errorCode) {
                            ERROR_CONNECT, ERROR_HOST_LOOKUP, ERROR_TIMEOUT, ERROR_UNKNOWN -> {
                                val html = getOfflineErrorHtml(failingUrl)
                                view?.loadDataWithBaseURL("safeer://offline", html, "text/html", "UTF-8", null)
                            }
                        }
                    }
                }
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

    private fun isYouTubeHtmlDocument(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            if (!host.contains("youtube.com") && !host.contains("youtu.be")) return false
            val path = uri.path?.lowercase() ?: "/"
            !path.startsWith("/api/") && !path.startsWith("/youtubei/") && !path.startsWith("/videoplayback") &&
                !path.endsWith(".js") && !path.endsWith(".css") && !path.endsWith(".png") &&
                !path.endsWith(".jpg") && !path.endsWith(".webp") && !path.endsWith(".svg") &&
                !path.endsWith(".ico") && !path.endsWith(".woff2") && !path.endsWith(".woff")
        } catch (_: Exception) {
            false
        }
    }

    private fun interceptAndSanitizeYouTubeWatch(url: String, userAgent: String): WebResourceResponse? {
        return try {
            val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", userAgent)
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrEmpty()) {
                    setRequestProperty("Cookie", cookies)
                }
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9,sl;q=0.8")
                setRequestProperty("Accept-Encoding", "gzip, deflate")
                setRequestProperty("Sec-Fetch-Dest", "document")
                setRequestProperty("Sec-Fetch-Mode", "navigate")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return null
            }

            // Shrani morebitne Set-Cookie glave v CookieManager prek zanesljive iteracije indeksov
            val cm = CookieManager.getInstance()
            var headerIdx = 1
            while (true) {
                val key = conn.getHeaderFieldKey(headerIdx) ?: break
                if (key.equals("Set-Cookie", ignoreCase = true)) {
                    val cookieVal = conn.getHeaderField(headerIdx)
                    if (!cookieVal.isNullOrEmpty()) {
                        cm.setCookie(url, cookieVal)
                    }
                }
                headerIdx++
            }
            cm.flush()

            val encoding = conn.contentEncoding?.lowercase() ?: ""
            val rawInputStream = if (encoding.contains("gzip")) {
                java.util.zip.GZIPInputStream(conn.inputStream)
            } else {
                conn.inputStream
            }

            val rawHtml = rawInputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val bootstrapJs = UserScriptManager.getYoutubeBootstrapScript()
            val scriptTag = "<script type=\"text/javascript\">$bootstrapJs</script>"

            val headRegex = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)
            val headMatch = headRegex.find(rawHtml)

            val modifiedHtml = when {
                headMatch != null -> {
                    val idx = headMatch.range.last + 1
                    rawHtml.substring(0, idx) + scriptTag + rawHtml.substring(idx)
                }
                rawHtml.contains("<html>", ignoreCase = true) -> {
                    val idx = rawHtml.indexOf("<html>", ignoreCase = true) + 6
                    rawHtml.substring(0, idx) + "<head>" + scriptTag + "</head>" + rawHtml.substring(idx)
                }
                else -> scriptTag + rawHtml
            }

            val responseHeaders = mutableMapOf<String, String>(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            )

            WebResourceResponse(
                "text/html",
                "UTF-8",
                code,
                "OK",
                responseHeaders,
                ByteArrayInputStream(modifiedHtml.toByteArray(Charsets.UTF_8))
            )
        } catch (e: Exception) {
            android.util.Log.d("SafeerYT", "Bypass intercept error: ${e.message}")
            null
        }
    }

    private fun getOfflineErrorHtml(failingUrl: String): String {
        val safeUrl = failingUrl.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        return """
        <!DOCTYPE html>
        <html lang="sl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Povezava ni uspela - Safeer Browser</title>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    background-color: #06090f;
                    color: #e2e8f0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    padding: 24px;
                    text-align: center;
                }
                .card {
                    background: #111827;
                    border: 1px solid #1f2937;
                    border-radius: 20px;
                    padding: 32px 24px;
                    max-width: 440px;
                    width: 100%;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                }
                .icon { font-size: 48px; margin-bottom: 16px; }
                h1 { font-size: 20px; font-weight: 700; color: #f8fafc; margin-bottom: 12px; }
                p { font-size: 14px; color: #94a3b8; line-height: 1.5; margin-bottom: 20px; }
                .url-badge { font-size: 12px; color: #64748b; word-break: break-all; margin-bottom: 24px; display: block; }
                .btn {
                    background: #2563eb;
                    color: #fff;
                    border: none;
                    padding: 14px 28px;
                    border-radius: 12px;
                    font-size: 15px;
                    font-weight: 600;
                    cursor: pointer;
                    width: 100%;
                    box-shadow: 0 4px 14px rgba(37,99,235,0.4);
                }
                .btn:active { background: #1d4ed8; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="icon">🌐</div>
                <h1>Spletne strani ni mogoče naložiti</h1>
                <p>Preverite internetno povezavo ali pravilnost spletnega naslova.</p>
                <span class="url-badge">$safeUrl</span>
                <button class="btn" onclick="location.reload()">Poskusi znova</button>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun isFullscreenVideoActive(): Boolean = customView != null

    fun exitFullscreenVideo() {
        webChromeClient?.onHideCustomView()
    }
}
