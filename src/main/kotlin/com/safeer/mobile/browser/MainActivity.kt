package com.safeer.mobile.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.widget.*
import java.net.URLEncoder

class MainActivity : android.app.Activity() {

    companion object {
        private const val REQ_CODE_PERMISSIONS = 1001
        private const val REQ_CODE_GEO_PERMISSIONS = 1002
        private const val REQ_CODE_NOTIFICATION = 1003
        private const val REQ_CODE_DEFAULT_BROWSER = 1004
    }

    private var keyboardWasVisible = false

    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    private lateinit var mainRoot: RelativeLayout
    private lateinit var mobileTopBar: LinearLayout
    private lateinit var btnHome: Button
    private lateinit var omniboxContainer: LinearLayout
    private lateinit var tvSecurityLock: TextView
    private lateinit var editUrl: EditText
    private lateinit var btnClearUrl: TextView
    private lateinit var btnSearchTrigger: TextView
    private lateinit var btnReload: Button
    private lateinit var btnStar: Button
    private lateinit var btnAddTab: Button
    private lateinit var btnTabCount: Button
    private lateinit var btnMenu: Button
    private lateinit var pageProgressBar: ProgressBar
    private lateinit var webViewContainer: FrameLayout

    // Overlays & Secondary Views
    private lateinit var tabSwitcherOverlay: RelativeLayout
    private lateinit var tabsGridView: GridView
    private lateinit var btnNewTabInSwitcher: Button
    private lateinit var btnCloseTabsSwitcher: Button
    private lateinit var btnCloseAllTabs: TextView

    private lateinit var findInPageBar: LinearLayout
    private lateinit var editFindText: EditText
    private lateinit var tvFindMatches: TextView
    private lateinit var btnFindPrev: Button
    private lateinit var btnFindNext: Button
    private lateinit var btnFindClose: Button

    // Managers & Repositories
    private lateinit var tabManager: TabManager
    private lateinit var repository: BrowserRepository
    private lateinit var downloadHandler: DownloadHandler

    private var customVideoView: View? = null
    private var customVideoCallback: WebChromeClient.CustomViewCallback? = null
    private var isDarkModeActive: Boolean = true

    override fun attachBaseContext(newBase: Context) {
        val lang = PreferencesManager.getLanguage(newBase)
        if (lang != "auto") {
            val locale = java.util.Locale(lang)
            java.util.Locale.setDefault(locale)
            val config = newBase.resources.configuration
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SafeerCrashHandler", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        setContentView(R.layout.activity_main)

        window.statusBarColor = Color.parseColor("#06090F")
        window.navigationBarColor = Color.parseColor("#000000")

        repository = BrowserRepository(this)
        downloadHandler = DownloadHandler(this)

        // ⚙️ Naloži shranjene nastavitve iz PreferencesManager
        AdBlockEngine.isEnabled = PreferencesManager.isAdBlockEnabled(this)
        isDarkModeActive = PreferencesManager.isDarkModeEnabled(this)
        AdBlockEngine.onAdBlocked = {
            PreferencesManager.incrementAdsBlocked(this)
        }
        ThreatBlockEngine.onThreatBlocked = { _, _, _, _ ->
            PreferencesManager.incrementThreatsBlocked(this)
        }

        initViews()
        setupWindowInsets()
        setupTabManager()
        setupOmnibox()
        setupTopButtons()
        setupTouchGestures()
        setupFindInPage()

        // Zaženi posodobitev varnostnih seznamov (ThreatFox, URLhaus, Phishing Army) v ozadju
        ThreatFeedsUpdater.updateFeedsAsync(this)

        // 🛡️ Inicializiraj šifriran DNS (DoH) ali šifriran tunel (Tor / Proxy)
        DoHProxyEngine.applySettings(this) { handleIncomingIntent(intent, isInitial = true) }

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val postNotif = "android.permission.POST_NOTIFICATIONS"
            if (checkSelfPermission(postNotif) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(postNotif), REQ_CODE_NOTIFICATION)
            }
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent, isInitial = false)
    }

    private fun handleIncomingIntent(intent: Intent?, isInitial: Boolean) {
        var targetUrl: String? = null

        if (intent != null) {
            when (intent.action) {
                Intent.ACTION_VIEW -> {
                    targetUrl = intent.dataString
                }
                Intent.ACTION_WEB_SEARCH -> {
                    val query = intent.getStringExtra(android.app.SearchManager.QUERY)
                        ?: intent.getStringExtra("query") ?: ""
                    if (query.isNotBlank()) {
                        targetUrl = PreferencesManager.buildSearchUrl(this, query)
                    }
                }
                Intent.ACTION_SEND -> {
                    if (intent.type?.startsWith("text/") == true) {
                        val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: ""
                        if (shared.isNotBlank()) {
                            targetUrl = if (shared.startsWith("http://") || shared.startsWith("https://")) {
                                shared
                            } else {
                                PreferencesManager.buildSearchUrl(this, shared)
                            }
                        }
                    }
                }
            }
        }

        val finalUrl = targetUrl ?: if (isInitial) "file:///android_asset/brave_home.html" else null
        if (finalUrl != null) {
            if (isInitial) {
                tabManager.createTab(this, finalUrl, true)
            } else {
                // Keep the page the user was reading when another app opens a link.
                val existing = tabManager.getAllTabs().find { it.url == finalUrl }
                if (existing != null) tabManager.switchTab(existing.id)
                else tabManager.createTab(this, finalUrl, true)
            }
        }
    }

    private fun isDefaultBrowser(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 29) {
            val roles = getSystemService(android.app.role.RoleManager::class.java)
            roles?.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER) == true &&
                roles.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)
        } else {
            val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org/"))
            packageManager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == packageName
        }
    }

    private fun requestDefaultBrowser() {
        if (isDefaultBrowser()) {
            Toast.makeText(this, R.string.default_browser_active, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val roles = getSystemService(android.app.role.RoleManager::class.java)
                if (roles?.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER) == true) {
                    startActivityForResult(roles.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER), REQ_CODE_DEFAULT_BROWSER)
                    return
                }
            }
            startActivityForResult(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS), REQ_CODE_DEFAULT_BROWSER)
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.default_browser_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_DEFAULT_BROWSER) {
            Toast.makeText(this, if (isDefaultBrowser()) R.string.default_browser_active else R.string.default_browser_unchanged, Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_CODE_PERMISSIONS -> {
                val req = pendingPermissionRequest
                pendingPermissionRequest = null
                if (req != null) {
                    val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                    if (allGranted) {
                        req.grant(req.resources)
                    } else {
                        req.deny()
                    }
                }
            }
            REQ_CODE_GEO_PERMISSIONS -> {
                val origin = pendingGeoOrigin
                val cb = pendingGeoCallback
                pendingGeoOrigin = null
                pendingGeoCallback = null
                if (cb != null && origin != null) {
                    val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    cb.invoke(origin, granted, false)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        tabManager.getActiveTab()?.webView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        tabManager.getActiveTab()?.webView?.onResume()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        DoHProxyEngine.stopServer()
        super.onDestroy()
    }

    private fun openUrlInBrowser(url: String) {
        val sanitized = UrlSanitizer.sanitize(url)
        val activeTab = tabManager.getActiveTab()
        if (activeTab != null) {
            activeTab.webView.navigateDocument(sanitized)
        } else {
            tabManager.createTab(this, sanitized, true)
        }
    }

    private fun initViews() {
        mainRoot = findViewById(R.id.mainRoot)
        mobileTopBar = findViewById(R.id.mobileTopBar)
        btnHome = findViewById(R.id.btnHome)
        omniboxContainer = findViewById(R.id.omniboxContainer)
        tvSecurityLock = findViewById(R.id.tvSecurityLock)
        editUrl = findViewById(R.id.editUrl)
        btnClearUrl = findViewById(R.id.btnClearUrl)
        btnSearchTrigger = findViewById(R.id.btnSearchTrigger)
        btnReload = findViewById(R.id.btnReload)
        btnStar = findViewById(R.id.btnStar)
        btnAddTab = findViewById(R.id.btnAddTab)
        btnTabCount = findViewById(R.id.btnTabCount)
        btnMenu = findViewById(R.id.btnMenu)
        pageProgressBar = findViewById(R.id.pageProgressBar)
        webViewContainer = findViewById(R.id.webViewContainer)

        tabSwitcherOverlay = findViewById(R.id.tabSwitcherOverlay)
        tabsGridView = findViewById(R.id.tabsGridView)
        btnNewTabInSwitcher = findViewById(R.id.btnNewTabInSwitcher)
        btnCloseTabsSwitcher = findViewById(R.id.btnCloseTabsSwitcher)
        btnCloseAllTabs = findViewById(R.id.btnCloseAllTabs)

        findInPageBar = findViewById(R.id.findInPageBar)
        editFindText = findViewById(R.id.editFindText)
        tvFindMatches = findViewById(R.id.tvFindMatches)
        btnFindPrev = findViewById(R.id.btnFindPrev)
        btnFindNext = findViewById(R.id.btnFindNext)
        btnFindClose = findViewById(R.id.btnFindClose)
    }

    private fun getStatusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) {
            val h = resources.getDimensionPixelSize(resId)
            if (h > 0) return h
        }
        return (24 * resources.displayMetrics.density).toInt()
    }

    private fun getNavigationBarHeight(): Int {
        val resId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resId > 0) {
            val h = resources.getDimensionPixelSize(resId)
            if (h > 0) return h
        }
        return (48 * resources.displayMetrics.density).toInt()
    }

    private fun applySystemBarInsets(statusBarHeight: Int, navBarHeight: Int) {
        val baseToolbarHeight = (64 * resources.displayMetrics.density).toInt()
        val totalToolbarHeight = baseToolbarHeight + statusBarHeight
        val lp = mobileTopBar.layoutParams
        if (lp != null && lp.height != totalToolbarHeight) {
            lp.height = totalToolbarHeight
            mobileTopBar.layoutParams = lp
        }
        mobileTopBar.setPadding(
            mobileTopBar.paddingLeft,
            statusBarHeight,
            mobileTopBar.paddingRight,
            mobileTopBar.paddingBottom
        )

        tabSwitcherOverlay.setPadding(0, statusBarHeight, 0, navBarHeight)

        val rootLp = webViewContainer.layoutParams as? RelativeLayout.LayoutParams
        if (rootLp != null && rootLp.bottomMargin != 0) {
            rootLp.bottomMargin = 0
            webViewContainer.layoutParams = rootLp
        }
    }

    private fun setupWindowInsets() {
        val initialStatusBar = getStatusBarHeight()
        val initialNavBar = getNavigationBarHeight()
        applySystemBarInsets(initialStatusBar, initialNavBar)

        mainRoot.setOnApplyWindowInsetsListener { _, insets ->
            val statusBarHeight = if (android.os.Build.VERSION.SDK_INT >= 30) {
                val sb = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
                if (sb > 0) sb else getStatusBarHeight()
            } else {
                @Suppress("DEPRECATION")
                val sb = insets.systemWindowInsetTop
                if (sb > 0) sb else getStatusBarHeight()
            }
            val navBarHeight = if (android.os.Build.VERSION.SDK_INT >= 30) {
                val nb = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                if (nb > 0) nb else getNavigationBarHeight()
            } else {
                @Suppress("DEPRECATION")
                val nb = insets.systemWindowInsetBottom
                if (nb > 0) nb else getNavigationBarHeight()
            }
            applySystemBarInsets(statusBarHeight, navBarHeight)
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val keyboardVisible = insets.isVisible(android.view.WindowInsets.Type.ime())
                if (keyboardWasVisible && !keyboardVisible && editUrl.hasFocus()) {
                    editUrl.clearFocus()
                    mainRoot.requestFocus()
                }
                keyboardWasVisible = keyboardVisible
            }
            insets
        }
        mainRoot.requestApplyInsets()
    }

    private fun setupTabManager() {
        tabManager = TabManager(webViewContainer) { count, activeTab ->
            btnTabCount.text = count.toString()
            if (activeTab != null) {
                activeTab.webView.isDarkMode = isDarkModeActive
                attachTabListeners(activeTab)
                updateOmniboxDisplay(activeTab.url, activeTab.webView.title)
            }
        }
    }

    private fun attachTabListeners(tab: TabModel) {
        val wv = tab.webView

        wv.onProgressUpdate = { progress ->
            if (tabManager.getActiveTab()?.id == tab.id) {
                if (progress < 100) {
                    pageProgressBar.visibility = View.VISIBLE
                    pageProgressBar.progress = progress
                } else {
                    pageProgressBar.visibility = View.GONE
                }
            }
        }

        wv.onUrlChanged = { newUrl ->
            tab.url = newUrl
            if (tabManager.getActiveTab()?.id == tab.id) {
                updateOmniboxDisplay(newUrl, wv.title)
            }
        }

        wv.onPageLoaded = { finalUrl, pageTitle ->
            tab.url = finalUrl
            tab.title = pageTitle
            if (finalUrl.isNotEmpty() && !finalUrl.startsWith("about:", ignoreCase = true)) {
                val cleanTitle = if (pageTitle.isNotEmpty()) pageTitle else finalUrl
                repository.addHistory(cleanTitle, finalUrl)
            }
        }

        wv.onTitleChanged = { title ->
            tab.title = title
            if (tabManager.getActiveTab()?.id == tab.id) {
                updateOmniboxDisplay(tab.url, title)
            }
        }

        wv.onSecurityChanged = { isSecure ->
            if (tabManager.getActiveTab()?.id == tab.id) {
                tvSecurityLock.text = if (isSecure) "🔒" else "⚠️"
                tvSecurityLock.setTextColor(
                    if (isSecure) Color.parseColor("#10B981") else Color.parseColor("#F59E0B")
                )
            }
        }

        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            downloadHandler.startDownload(url, userAgent, contentDisposition, mimeType)
        }

        wv.onFullscreenToggled = { customView, callback ->
            if (customView != null) {
                customVideoView = customView
                customVideoCallback = callback
                mainRoot.setPadding(0, 0, 0, 0)
                mobileTopBar.visibility = View.GONE
                webViewContainer.visibility = View.GONE
                mainRoot.addView(
                    customView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            } else {
                customVideoView?.let { mainRoot.removeView(it) }
                customVideoView = null
                customVideoCallback = null
                mobileTopBar.visibility = View.VISIBLE
                webViewContainer.visibility = View.VISIBLE
                mainRoot.requestApplyInsets()
            }
        }

        wv.onCreateWindowRequested = { _, isUserGesture, resultMsg ->
            val transport = resultMsg.obj as? android.webkit.WebView.WebViewTransport
            if (!isUserGesture || transport == null) false
            else {
                val newTab = tabManager.createTab(this, "about:blank", true)
                transport.webView = newTab.webView
                resultMsg.sendToTarget()
                true
            }
        }

        wv.onCloseWindowRequested = {
            tabManager.closeTab(this, tab.id)
        }

        wv.onPermissionRequested = { request ->
            val resources = request.resources ?: emptyArray()
            val host = request.origin?.host ?: request.origin?.toString() ?: "Spletna stran"

            val labels = resources.map { res ->
                when (res) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "🎤 Mikrofon (zvok)"
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "📷 Kamera (video)"
                    PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> "🔑 Zaščitena medijska vsebina (DRM)"
                    else -> res.substringAfterLast(".")
                }
            }.joinToString("\n• ", prefix = "• ")

            runOnUiThread {
                try {
                    AlertDialog.Builder(this)
                        .setTitle("Zahteva za dovoljenje")
                        .setMessage("Spletno mesto '$host' želi dostop do naslednjih virov naprave:\n\n$labels\n\nAli dovolite dostop?")
                        .setPositiveButton("Dovoli") { _, _ ->
                            val neededSystem = mutableListOf<String>()
                            if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                neededSystem.add(Manifest.permission.RECORD_AUDIO)
                            }
                            if (resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                neededSystem.add(Manifest.permission.CAMERA)
                            }

                            if (neededSystem.isNotEmpty()) {
                                pendingPermissionRequest = request
                                requestPermissions(neededSystem.toTypedArray(), REQ_CODE_PERMISSIONS)
                            } else {
                                request.grant(resources)
                            }
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

        wv.onGeolocationRequested = { origin, callback ->
            runOnUiThread {
                try {
                    AlertDialog.Builder(this)
                        .setTitle("Zahteva za lokacijo")
                        .setMessage("Spletno mesto '$origin' želi dostop do vaše trenutne geografske lokacije.\n\nAli dovolite dostop?")
                        .setPositiveButton("Dovoli") { _, _ ->
                            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                pendingGeoOrigin = origin
                                pendingGeoCallback = callback
                                requestPermissions(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ),
                                    REQ_CODE_GEO_PERMISSIONS
                                )
                            } else {
                                callback.invoke(origin, true, false)
                            }
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
    }

    private fun setupOmnibox() {
        editUrl.setOnFocusChangeListener { _, hasFocus ->
            btnTabCount.visibility = if (hasFocus) View.GONE else View.VISIBLE
            btnMenu.visibility = if (hasFocus) View.GONE else View.VISIBLE
            tvSecurityLock.visibility = if (hasFocus) View.GONE else View.VISIBLE
            btnSearchTrigger.visibility = if (hasFocus) View.VISIBLE else View.GONE
            if (hasFocus) {
                val currentUrl = tabManager.getActiveTab()?.url ?: ""
                val isLocal = currentUrl.isEmpty() || currentUrl.startsWith("file:///android_asset/") || currentUrl == "about:blank"
                if (isLocal) {
                    editUrl.setText("")
                } else {
                    editUrl.setText(currentUrl)
                    editUrl.selectAll()
                }
                btnClearUrl.visibility = if (editUrl.text.isNotEmpty()) View.VISIBLE else View.GONE
            } else {
                btnClearUrl.visibility = View.GONE
                val activeTab = tabManager.getActiveTab()
                updateOmniboxDisplay(activeTab?.url ?: "", activeTab?.webView?.title)
            }
        }

        editUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (editUrl.hasFocus()) {
                    btnClearUrl.visibility = if (!s.isNullOrEmpty()) View.VISIBLE else View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        editUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performNavigation(editUrl.text.toString().trim())
                hideKeyboard()
                editUrl.clearFocus()
                true
            } else {
                false
            }
        }

        btnClearUrl.setOnClickListener {
            editUrl.setText("")
            editUrl.requestFocus()
        }

        btnSearchTrigger.setOnClickListener {
            performNavigation(editUrl.text.toString().trim())
            hideKeyboard()
            editUrl.clearFocus()
        }
    }

    private fun updateStarState(url: String? = null) {
        val targetUrl = url ?: tabManager.getActiveTab()?.url ?: ""
        val isBm = if (targetUrl.isNotEmpty() && !targetUrl.startsWith("file:///android_asset/")) {
            repository.isBookmarked(targetUrl)
        } else false
        btnStar.text = if (isBm) "⭐" else "☆"
    }

    private fun updateOmniboxDisplay(url: String, title: String?) {
        updateStarState(url)
        if (editUrl.hasFocus()) return

        if (url.isEmpty() || url == "about:blank" || url.startsWith("file:///android_asset/brave_home.html")) {
            editUrl.setText("")
            editUrl.hint = getString(R.string.url_hint)
            tvSecurityLock.text = "🦁"
            return
        }

        try {
            val uri = Uri.parse(url)
            val host = uri.host ?: url
            val cleanHost = host.removePrefix("www.")
            val path = uri.path ?: ""
            val display = if (path.length > 1 && path != "/") "$cleanHost$path" else cleanHost
            editUrl.setText(display)
        } catch (_: Exception) {
            editUrl.setText(url)
        }
    }

    private fun performNavigation(input: String) {
        if (input.isEmpty()) return

        val rawUrl = when {
            input.startsWith("http://", ignoreCase = true) -> {
                val withoutScheme = input.substring(7)
                val host = withoutScheme.substringBefore('/').substringBefore(':').lowercase()
                val isLocal = host == "localhost" || host == "127.0.0.1" ||
                              host.startsWith("192.168.") || host.startsWith("10.") ||
                              host.startsWith("172.16.") || host.startsWith("172.17.") ||
                              host.startsWith("172.18.") || host.startsWith("172.19.") ||
                              host.startsWith("172.2") || host.startsWith("172.30.") || host.startsWith("172.31.")
                if (isLocal) input else "https://$withoutScheme"
            }
            input.startsWith("https://", ignoreCase = true) || input.startsWith("file://", ignoreCase = true) -> {
                input
            }
            input.contains(".") && !input.contains(" ") -> {
                "https://$input"
            }
            else -> {
                PreferencesManager.buildSearchUrl(this, input)
            }
        }

        val finalUrl = UrlSanitizer.sanitize(rawUrl)
        openUrlInBrowser(finalUrl)
    }

    private fun setupTopButtons() {
        btnHome.setOnClickListener {
            openUrlInBrowser("file:///android_asset/brave_home.html")
        }

        btnReload.setOnClickListener {
            val activeTab = tabManager.getActiveTab()
            activeTab?.webView?.reload()
        }

        btnStar.setOnClickListener {
            val activeTab = tabManager.getActiveTab()
            val curUrl = activeTab?.url ?: ""
            if (curUrl.isNotEmpty() && !curUrl.startsWith("file:///android_asset/")) {
                val isBm = repository.isBookmarked(curUrl)
                if (isBm) {
                    repository.removeBookmark(curUrl)
                    btnStar.text = "☆"
                    Toast.makeText(this, getString(R.string.toast_bookmark_removed), Toast.LENGTH_SHORT).show()
                } else {
                    repository.addBookmark(activeTab?.webView?.title ?: "Zaznamek", curUrl)
                    btnStar.text = "⭐"
                    Toast.makeText(this, getString(R.string.toast_bookmark_added), Toast.LENGTH_SHORT).show()
                }
            } else {
                showBookmarksDialog()
            }
        }

        btnStar.setOnLongClickListener {
            showBookmarksDialog()
            true
        }

        btnAddTab.setOnClickListener {
            tabManager.createTab(this, "file:///android_asset/brave_home.html", true)
        }

        btnTabCount.setOnClickListener {
            toggleTabSwitcher()
        }
        btnTabCount.setOnLongClickListener {
            tabManager.createTab(this, "file:///android_asset/brave_home.html", true)
            editUrl.requestFocus()
            showKeyboard()
            true
        }

        btnMenu.setOnClickListener {
            showMobileMenu()
        }

        // Tab switcher buttons
        btnNewTabInSwitcher.setOnClickListener {
            tabSwitcherOverlay.visibility = View.GONE
            tabManager.createTab(this, "file:///android_asset/brave_home.html", true)
        }

        btnCloseTabsSwitcher.setOnClickListener {
            tabSwitcherOverlay.visibility = View.GONE
        }

        btnCloseAllTabs.setOnClickListener {
            tabManager.closeAllTabs(this)
            tabSwitcherOverlay.visibility = View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 80
            private val SWIPE_VELOCITY_THRESHOLD = 80

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (Math.abs(diffX) > Math.abs(diffY) &&
                    Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    if (diffX > 0) {
                        tabManager.switchToPrevTab()
                        Toast.makeText(this@MainActivity, "◀ Prejšnji zavihek", Toast.LENGTH_SHORT).show()
                    } else {
                        tabManager.switchToNextTab()
                        Toast.makeText(this@MainActivity, "Naslednji zavihek ▶", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
        })

        omniboxContainer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun toggleTabSwitcher() {
        if (tabSwitcherOverlay.visibility == View.VISIBLE) {
            tabSwitcherOverlay.visibility = View.GONE
        } else {
            renderTabsGrid()
            tabSwitcherOverlay.visibility = View.VISIBLE
        }
    }

    private fun renderTabsGrid() {
        val allTabs = tabManager.getAllTabs()
        val activeId = tabManager.getActiveTab()?.id

        tabsGridView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = allTabs.size
            override fun getItem(position: Int): Any = allTabs[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val tab = allTabs[position]
                val view = convertView ?: LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.item_tab_card, parent, false)

                val tvTitle = view.findViewById<TextView>(R.id.tvTabTitle)
                val tvUrl = view.findViewById<TextView>(R.id.tvTabUrl)
                val tvActiveBadge = view.findViewById<TextView>(R.id.tvActiveBadge)
                val btnClose = view.findViewById<TextView>(R.id.btnTabClose)
                val cardRoot = view.findViewById<RelativeLayout>(R.id.tabCardRoot)

                tvTitle.text = tab.title.ifEmpty { "Zavihek ${position + 1}" }
                tvUrl.text = tab.url
                
                val isActive = (tab.id == activeId)
                cardRoot.setBackgroundResource(if (isActive) R.drawable.bg_tab_card_active else R.drawable.bg_tab_card)
                tvActiveBadge.visibility = if (isActive) View.VISIBLE else View.GONE

                view.setOnClickListener {
                    tabManager.switchTab(tab.id)
                    tabSwitcherOverlay.visibility = View.GONE
                }

                btnClose.setOnClickListener {
                    tabManager.closeTab(this@MainActivity, tab.id)
                    renderTabsGrid()
                }

                return view
            }
        }
    }

    private fun showMobileMenu() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_mobile_menu)
        // Keep the quick actions visible and the complete options list scrollable on small screens.
        dialog.findViewById<View>(R.id.menuOptionsScroll).layoutParams.height =
            minOf((resources.displayMetrics.heightPixels * 0.60f).toInt(), (520 * resources.displayMetrics.density).toInt())
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.BOTTOM)

        val activeTab = tabManager.getActiveTab()
        val wv = activeTab?.webView

        val menuBtnBack = dialog.findViewById<Button>(R.id.menuBtnBack)
        val menuBtnForward = dialog.findViewById<Button>(R.id.menuBtnForward)
        val menuBtnReload = dialog.findViewById<Button>(R.id.menuBtnReload)
        val menuBtnStar = dialog.findViewById<Button>(R.id.menuBtnStar)
        val menuBtnShare = dialog.findViewById<Button>(R.id.menuBtnShare)

        menuBtnBack.setOnClickListener {
            if (wv?.canGoBack() == true) wv.goBack()
            dialog.dismiss()
        }

        menuBtnForward.setOnClickListener {
            if (wv?.canGoForward() == true) wv.goForward()
            dialog.dismiss()
        }

        menuBtnBack.isEnabled = wv?.canGoBack() == true
        menuBtnForward.isEnabled = wv?.canGoForward() == true
        menuBtnReload.text = if (wv != null && wv.progress < 100) "✕" else "↻"
        menuBtnReload.setOnClickListener {
            if (wv != null && wv.progress < 100) wv.stopLoading() else wv?.reload()
            dialog.dismiss()
        }

        val curUrl = activeTab?.url ?: ""
        val isBm = repository.isBookmarked(curUrl)
        menuBtnStar.text = if (isBm) "⭐" else "☆"
        menuBtnStar.setOnClickListener {
            if (isBm) {
                repository.removeBookmark(curUrl)
                Toast.makeText(this, getString(R.string.toast_bookmark_removed), Toast.LENGTH_SHORT).show()
            } else {
                repository.addBookmark(wv?.title ?: "Zaznamek", curUrl)
                Toast.makeText(this, getString(R.string.toast_bookmark_added), Toast.LENGTH_SHORT).show()
            }
            updateStarState(curUrl)
            dialog.dismiss()
        }

        menuBtnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, curUrl)
            }
            startActivity(Intent.createChooser(shareIntent, "Deli stran"))
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayout>(R.id.rowMenuHome).setOnClickListener {
            openUrlInBrowser("file:///android_asset/brave_home.html")
            dialog.dismiss()
        }
        val defaultRow = dialog.findViewById<LinearLayout>(R.id.rowMenuDefaultBrowser)
        if (isDefaultBrowser()) {
            dialog.findViewById<TextView>(R.id.labelMenuDefaultBrowser).setText(R.string.default_browser_active)
            defaultRow.isEnabled = false
        } else defaultRow.setOnClickListener {
            dialog.dismiss()
            requestDefaultBrowser()
        }
        dialog.findViewById<LinearLayout>(R.id.rowMenuNewTab).setOnClickListener {
            tabManager.createTab(this, "file:///android_asset/brave_home.html", true)
            dialog.dismiss()
            editUrl.requestFocus()
            showKeyboard()
        }

        dialog.findViewById<LinearLayout>(R.id.rowMenuBookmarks).setOnClickListener {
            dialog.dismiss()
            showBookmarksDialog()
        }

        dialog.findViewById<LinearLayout>(R.id.rowMenuDownloads).setOnClickListener {
            dialog.dismiss()
            try {
                startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS))
            } catch (_: Exception) {
                Toast.makeText(this, "Mapa prenosov je v mapi Prenosi", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<LinearLayout>(R.id.rowMenuHistory).setOnClickListener {
            dialog.dismiss()
            showHistoryDialog()
        }

        dialog.findViewById<LinearLayout>(R.id.rowMenuFindInPage).setOnClickListener {
            dialog.dismiss()
            showFindInPage()
        }

        val cbDesktop = dialog.findViewById<CheckBox>(R.id.cbDesktopSite)
        cbDesktop.isChecked = activeTab?.isDesktop ?: false
        dialog.findViewById<LinearLayout>(R.id.rowMenuDesktopSite).setOnClickListener {
            val nextState = !cbDesktop.isChecked
            cbDesktop.isChecked = nextState
            activeTab?.isDesktop = nextState
            wv?.isDesktopMode = nextState
            wv?.reload()
            dialog.dismiss()
        }

        // Threat Shield Status Dialog
        dialog.findViewById<LinearLayout>(R.id.rowMenuThreatStats).setOnClickListener {
            dialog.dismiss()
            showThreatStatsDialog()
        }

        val cbAdBlock = dialog.findViewById<CheckBox>(R.id.cbAdBlock)
        cbAdBlock.isChecked = AdBlockEngine.isEnabled
        dialog.findViewById<LinearLayout>(R.id.rowMenuAdBlock).setOnClickListener {
            AdBlockEngine.isEnabled = !AdBlockEngine.isEnabled
            cbAdBlock.isChecked = AdBlockEngine.isEnabled
            PreferencesManager.setAdBlockEnabled(this, AdBlockEngine.isEnabled)
            Toast.makeText(
                this,
                if (AdBlockEngine.isEnabled) I18n.t(this, "toast_adblock_on") else I18n.t(this, "toast_adblock_off"),
                Toast.LENGTH_SHORT
            ).show()
            wv?.reload()
            dialog.dismiss()
        }

        val cbDark = dialog.findViewById<CheckBox>(R.id.cbDarkMode)
        cbDark.isChecked = isDarkModeActive
        dialog.findViewById<LinearLayout>(R.id.rowMenuDarkMode).setOnClickListener {
            isDarkModeActive = !isDarkModeActive
            cbDark.isChecked = isDarkModeActive
            PreferencesManager.setDarkModeEnabled(this, isDarkModeActive)
            tabManager.getAllTabs().forEach { t ->
                t.webView.applyDarkMode(isDarkModeActive)
            }
            Toast.makeText(
                this,
                if (isDarkModeActive) I18n.t(this, "toast_dark_on") else I18n.t(this, "toast_dark_off"),
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayout>(R.id.rowMenuSettings)?.setOnClickListener {
            dialog.dismiss()
            showSettingsDialog()
        }

        dialog.show()
    }

    private fun showSettingsDialog() {
        val engines = arrayOf("Google", "DuckDuckGo", "Brave Search")
        val engineKeys = arrayOf(
            PreferencesManager.SEARCH_GOOGLE,
            PreferencesManager.SEARCH_DUCKDUCKGO,
            PreferencesManager.SEARCH_BRAVE
        )
        val currentEngine = PreferencesManager.getSearchEngine(this)
        val selectedEngineIndex = engineKeys.indexOf(currentEngine).let { if (it >= 0) it else 0 }

        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 24)
        }

        // 0. Jezik vmesnika / Interface Language
        val tvLangTitle = TextView(this).apply {
            text = I18n.t(this@MainActivity, "settings_language")
            textSize = 15f
            setTextColor(Color.parseColor("#00d2ff"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        view.addView(tvLangTitle)

        val langKeys = arrayOf("auto", "sl", "en", "de", "es", "fr", "it")
        val langNames = arrayOf(
            I18n.t(this, "lang_auto"),
            "Slovenščina",
            "English",
            "Deutsch",
            "Español",
            "Français",
            "Italiano"
        )
        val currentLang = PreferencesManager.getLanguage(this)
        val selectedLangIdx = langKeys.indexOf(currentLang).let { if (it >= 0) it else 0 }

        val rgLang = RadioGroup(this)
        langKeys.forEachIndexed { idx, _ ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = langNames[idx]
                isChecked = (idx == selectedLangIdx)
                setTextColor(Color.WHITE)
            }
            rgLang.addView(rb)
        }
        view.addView(rgLang)

        // Ločilna črta
        view.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2).apply {
                setMargins(0, 24, 0, 24)
            }
            setBackgroundColor(Color.parseColor("#334155"))
        })

        // 1. Iskalnik
        val tvSearchTitle = TextView(this).apply {
            text = I18n.t(this@MainActivity, "settings_search_engine")
            textSize = 15f
            setTextColor(Color.parseColor("#00d2ff"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        view.addView(tvSearchTitle)

        val rgEngine = RadioGroup(this)
        engineKeys.forEachIndexed { idx, _ ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = engines[idx]
                isChecked = (idx == selectedEngineIndex)
                setTextColor(Color.WHITE)
            }
            rgEngine.addView(rb)
        }
        view.addView(rgEngine)

        // Ločilna črta
        view.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2).apply {
                setMargins(0, 24, 0, 24)
            }
            setBackgroundColor(Color.parseColor("#334155"))
        })

        // 2. Preklopniki
        val tvShieldTitle = TextView(this).apply {
            text = I18n.t(this@MainActivity, "settings_privacy_security")
            textSize = 15f
            setTextColor(Color.parseColor("#00d2ff"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        view.addView(tvShieldTitle)

        val cbAdBlock = CheckBox(this).apply {
            text = I18n.t(this@MainActivity, "adblock_shield")
            isChecked = PreferencesManager.isAdBlockEnabled(this@MainActivity)
            setTextColor(Color.WHITE)
        }
        view.addView(cbAdBlock)

        val cbDarkMode = CheckBox(this).apply {
            text = I18n.t(this@MainActivity, "dark_mode")
            isChecked = PreferencesManager.isDarkModeEnabled(this@MainActivity)
            setTextColor(Color.WHITE)
        }
        view.addView(cbDarkMode)

        val cbThirdPartyCookies = CheckBox(this).apply {
            text = I18n.t(this@MainActivity, "third_party_cookies")
            isChecked = PreferencesManager.isThirdPartyCookiesEnabled(this@MainActivity)
            setTextColor(Color.WHITE)
        }
        view.addView(cbThirdPartyCookies)
        view.addView(Button(this).apply {
            setText(if (isDefaultBrowser()) R.string.default_browser_active else R.string.menu_default_browser)
            setOnClickListener { requestDefaultBrowser() }
        })

        val cbJs = CheckBox(this).apply {
            text = I18n.t(this@MainActivity, "enable_javascript")
            isChecked = PreferencesManager.isJavaScriptEnabled(this@MainActivity)
            setTextColor(Color.WHITE)
        }
        view.addView(cbJs)

        val cbAdguard = CheckBox(this).apply {
            text = I18n.t(this@MainActivity, "adguard_protection")
            isChecked = PreferencesManager.isAdguardProtectionEnabled(this@MainActivity)
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        view.addView(cbAdguard)

        val tvAdguardDesc = TextView(this).apply {
            text = I18n.t(this@MainActivity, "adguard_desc")
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(64, 0, 0, 8)
        }
        view.addView(tvAdguardDesc)

        // Ločilna črta
        view.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2).apply {
                setMargins(0, 24, 0, 24)
            }
            setBackgroundColor(Color.parseColor("#334155"))
        })

        // 2.1. Šifriran DNS (DoH) za zaščito pred cenzuro
        val tvDohTitle = TextView(this).apply {
            text = I18n.t(this@MainActivity, "doh_title")
            textSize = 15f
            setTextColor(Color.parseColor("#00d2ff"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        view.addView(tvDohTitle)

        val dohNames = arrayOf(
            I18n.t(this, "doh_quad9"),
            I18n.t(this, "doh_adguard"),
            I18n.t(this, "doh_cloudflare"),
            I18n.t(this, "doh_google"),
            I18n.t(this, "doh_custom"),
            I18n.t(this, "doh_disabled")
        )
        val dohKeys = arrayOf("quad9", "adguard", "cloudflare", "google", "custom", "disabled")
        var currentDoh = PreferencesManager.getDohProvider(this)
        if (!PreferencesManager.isDohEnabled(this)) currentDoh = "disabled"
        val selectedDohIdx = dohKeys.indexOf(currentDoh).let { if (it >= 0) it else 0 }

        val editCustomDoh = EditText(this).apply {
            hint = I18n.t(this@MainActivity, "custom_doh_hint")
            setText(PreferencesManager.getCustomDohUrl(this@MainActivity))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            visibility = if (currentDoh == "custom") View.VISIBLE else View.GONE
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#1e293b"))
        }

        val rgDoh = RadioGroup(this)
        dohKeys.forEachIndexed { idx, key ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = dohNames[idx]
                isChecked = (idx == selectedDohIdx)
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked && key == "custom") {
                        editCustomDoh.visibility = View.VISIBLE
                    } else if (isChecked) {
                        editCustomDoh.visibility = View.GONE
                    }
                }
            }
            rgDoh.addView(rb)
        }
        view.addView(rgDoh)
        view.addView(editCustomDoh)

        // Ločilna črta
        view.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2).apply {
                setMargins(0, 24, 0, 24)
            }
            setBackgroundColor(Color.parseColor("#334155"))
        })

        // 2.2. Šifriran tunel / Proxy
        val tvProxyTitle = TextView(this).apply {
            text = I18n.t(this@MainActivity, "proxy_title")
            textSize = 15f
            setTextColor(Color.parseColor("#00d2ff"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        view.addView(tvProxyTitle)

        val proxyNames = arrayOf(
            I18n.t(this, "proxy_disabled"),
            I18n.t(this, "proxy_tor"),
            I18n.t(this, "proxy_custom")
        )
        val proxyKeys = arrayOf("disabled", "tor", "custom")
        val currentProxy = PreferencesManager.getSecureProxyMode(this)
        val selectedProxyIdx = proxyKeys.indexOf(currentProxy).let { if (it >= 0) it else 0 }

        val editCustomProxy = EditText(this).apply {
            hint = I18n.t(this@MainActivity, "custom_proxy_hint")
            setText(PreferencesManager.getSecureProxyUrl(this@MainActivity))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            visibility = if (currentProxy == "custom") View.VISIBLE else View.GONE
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#1e293b"))
        }

        val rgProxy = RadioGroup(this)
        proxyKeys.forEachIndexed { idx, key ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = proxyNames[idx]
                isChecked = (idx == selectedProxyIdx)
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked && key == "custom") {
                        editCustomProxy.visibility = View.VISIBLE
                    } else if (isChecked) {
                        editCustomProxy.visibility = View.GONE
                    }
                }
            }
            rgProxy.addView(rb)
        }
        view.addView(rgProxy)
        view.addView(editCustomProxy)

        // Ločilna črta
        view.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2).apply {
                setMargins(0, 24, 0, 24)
            }
            setBackgroundColor(Color.parseColor("#334155"))
        })

        // 3. Počisti podatke
        val btnClearData = Button(this).apply {
            text = I18n.t(this@MainActivity, "btn_clear_data")
            setBackgroundResource(R.drawable.bg_mobile_icon_button)
            setTextColor(Color.parseColor("#ff5555"))
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(I18n.t(this@MainActivity, "clear_data_dialog_title"))
                    .setMessage(I18n.t(this@MainActivity, "clear_data_dialog_msg"))
                    .setPositiveButton(I18n.t(this@MainActivity, "btn_clear")) { _, _ ->
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                        android.webkit.WebStorage.getInstance().deleteAllData()
                        tabManager.getAllTabs().forEach { it.webView.clearCache(true) }
                        repository.clearHistory()
                        Toast.makeText(this@MainActivity, I18n.t(this@MainActivity, "toast_data_cleared"), Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(I18n.t(this@MainActivity, "btn_cancel"), null)
                    .show()
            }
        }
        view.addView(btnClearData)

        // 4. Info
        val tvInfo = TextView(this).apply {
            text = "\nSafeer Mobile Browser v1.0.6 • Target SDK 36\nSafeer is a security layer, not a guarantee against all online threats."
            textSize = 11f
            setTextColor(Color.parseColor("#64748b"))
            setPadding(0, 16, 0, 0)
        }
        view.addView(tvInfo)

        val scroll = ScrollView(this).apply {
            addView(view)
            setBackgroundColor(Color.parseColor("#0a0f1d"))
        }

        AlertDialog.Builder(this)
            .setTitle(I18n.t(this, "settings_title"))
            .setView(scroll)
            .setPositiveButton(I18n.t(this, "btn_save")) { _, _ ->
                val langCheckedId = rgLang.checkedRadioButtonId
                val langCheckedRb = rgLang.findViewById<RadioButton>(langCheckedId)
                val langIdx = rgLang.indexOfChild(langCheckedRb)
                val selLang = if (langIdx in langKeys.indices) langKeys[langIdx] else "auto"
                val oldLang = PreferencesManager.getLanguage(this)
                PreferencesManager.setLanguage(this, selLang)

                val checkedId = rgEngine.checkedRadioButtonId
                val checkedRb = rgEngine.findViewById<RadioButton>(checkedId)
                val radioIdx = rgEngine.indexOfChild(checkedRb)
                if (radioIdx in engineKeys.indices) {
                    PreferencesManager.setSearchEngine(this, engineKeys[radioIdx])
                }

                val newAdBlock = cbAdBlock.isChecked
                PreferencesManager.setAdBlockEnabled(this, newAdBlock)
                AdBlockEngine.isEnabled = newAdBlock

                val newDark = cbDarkMode.isChecked
                PreferencesManager.setDarkModeEnabled(this, newDark)
                isDarkModeActive = newDark
                tabManager.getAllTabs().forEach { it.webView.applyDarkMode(newDark) }

                val newThirdParty = cbThirdPartyCookies.isChecked
                PreferencesManager.setThirdPartyCookiesEnabled(this, newThirdParty)
                tabManager.getAllTabs().forEach {
                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(it.webView, newThirdParty)
                }

                val newJs = cbJs.isChecked
                PreferencesManager.setJavaScriptEnabled(this, newJs)
                tabManager.getAllTabs().forEach {
                    it.webView.settings.javaScriptEnabled = newJs
                }

                PreferencesManager.setAdguardProtectionEnabled(this, cbAdguard.isChecked)

                // 🛡️ DoH & Proxy posodobitev
                val dohCheckedId = rgDoh.checkedRadioButtonId
                val dohCheckedRb = rgDoh.findViewById<RadioButton>(dohCheckedId)
                val dohIdx = rgDoh.indexOfChild(dohCheckedRb)
                val selDoh = if (dohIdx in dohKeys.indices) dohKeys[dohIdx] else "quad9"
                PreferencesManager.setDohEnabled(this, selDoh != "disabled")
                PreferencesManager.setDohProvider(this, selDoh)
                PreferencesManager.setCustomDohUrl(this, editCustomDoh.text.toString().trim())

                val proxyCheckedId = rgProxy.checkedRadioButtonId
                val proxyCheckedRb = rgProxy.findViewById<RadioButton>(proxyCheckedId)
                val proxyIdx = rgProxy.indexOfChild(proxyCheckedRb)
                val selProxy = if (proxyIdx in proxyKeys.indices) proxyKeys[proxyIdx] else "disabled"
                PreferencesManager.setSecureProxyMode(this, selProxy)
                PreferencesManager.setSecureProxyUrl(this, editCustomProxy.text.toString().trim())

                DoHProxyEngine.applySettings(this)

                Toast.makeText(this, I18n.t(this, "toast_settings_saved"), Toast.LENGTH_SHORT).show()

                if (selLang != oldLang) {
                    recreate()
                }
            }
            .setNegativeButton(I18n.t(this, "btn_cancel"), null)
            .show()
    }

    private fun showThreatStatsDialog() {
        val totalThreats = ThreatBlockEngine.totalBlockedThreats.get()
        val c2 = ThreatBlockEngine.blockedC2Count.get()
        val malware = ThreatBlockEngine.blockedMalwareCount.get()
        val phishing = ThreatBlockEngine.blockedPhishingCount.get()
        val totalAds = AdBlockEngine.blockedAdsCount.get()

        AlertDialog.Builder(this)
            .setTitle("🛑 Safeer Threat Shield & AdBlock")
            .setMessage(
                """
                Varnostni ščit varuje vašo napravo pred nevarnimi C2 strežniki in zlonamerno kodo:
                
                • Blokiranih C2 Botnet strežnikov: $c2
                • Blokiranih Malware prenosov: $malware
                • Blokiranih Phishing strani: $phishing
                • Skupaj preprečenih groženj: $totalThreats
                • Blokiranih oglasov in sledilcev: $totalAds
                
                Viri: abuse.ch ThreatFox IOC, URLhaus, Phishing Army, StevenBlack Hosts.
                """.trimIndent()
            )
            .setPositiveButton("Posodobi sezname") { _, _ ->
                Toast.makeText(this, "🔄 Posodabljam varnostne sezname...", Toast.LENGTH_SHORT).show()
                ThreatFeedsUpdater.updateFeedsAsync(this) { added ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "✅ Dodanih $added novih varnostnih pravil!", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Zapri", null)
            .show()
    }

    private fun showFindInPage() {
        findInPageBar.visibility = View.VISIBLE
        editFindText.requestFocus()
        showKeyboard(editFindText)

        val activeWv = tabManager.getActiveTab()?.webView
        activeWv?.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            tvFindMatches.text = if (numberOfMatches > 0) "${activeMatchOrdinal + 1}/$numberOfMatches" else "0/0"
        }
        val query = editFindText.text.toString()
        if (query.isNotEmpty()) {
            activeWv?.findAllAsync(query)
        } else {
            tvFindMatches.text = "0/0"
        }
    }

    private fun showBookmarksDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_bookmarks)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val listView = dialog.findViewById<ListView>(R.id.bookmarksListView)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseBookmarks)
        val bookmarks = repository.getBookmarks()

        listView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = bookmarks.size
            override fun getItem(position: Int): Any = bookmarks[position]
            override fun getItemId(position: Int): Long = bookmarks[position].id

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val bm = bookmarks[position]
                val view = convertView ?: LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.item_bookmark, parent, false)

                view.findViewById<TextView>(R.id.tvBmIcon).text = bm.icon
                view.findViewById<TextView>(R.id.tvBmTitle).text = bm.title
                view.findViewById<TextView>(R.id.tvBmUrl).text = bm.url

                view.setOnClickListener {
                    tabManager.getActiveTab()?.webView?.navigateDocument(bm.url)
                    dialog.dismiss()
                }

                view.findViewById<Button>(R.id.btnDeleteBm).setOnClickListener {
                    repository.removeBookmark(bm.url)
                    dialog.dismiss()
                    showBookmarksDialog()
                }

                return view
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showHistoryDialog() {
        val history = repository.getHistory(50)
        val items = history.map { "${it.title}\n${it.url}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("🕒 Zgodovina brskanja")
            .setItems(items) { _, which ->
                val selected = history[which]
                tabManager.getActiveTab()?.webView?.navigateDocument(selected.url)
            }
            .setPositiveButton("Počisti zgodovino") { _, _ ->
                repository.clearHistory()
                Toast.makeText(this, "Zgodovina počiščena", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Zapri", null)
            .show()
    }

    private fun setupFindInPage() {
        editFindText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val wv = tabManager.getActiveTab()?.webView ?: return
                wv.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                    tvFindMatches.text = if (numberOfMatches > 0) "${activeMatchOrdinal + 1}/$numberOfMatches" else "0/0"
                }
                wv.findAllAsync(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFindPrev.setOnClickListener {
            tabManager.getActiveTab()?.webView?.findNext(false)
        }

        btnFindNext.setOnClickListener {
            tabManager.getActiveTab()?.webView?.findNext(true)
        }

        btnFindClose.setOnClickListener {
            tabManager.getActiveTab()?.webView?.clearMatches()
            findInPageBar.visibility = View.GONE
            hideKeyboard(editFindText)
        }
    }

    private fun showKeyboard(target: View = editUrl) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(target: View = editUrl) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(target.windowToken, 0)
    }

    override fun onBackPressed() {
        if (editUrl.hasFocus()) {
            hideKeyboard()
            editUrl.clearFocus()
            mainRoot.requestFocus()
            return
        }
        if (customVideoView != null) {
            tabManager.getActiveTab()?.webView?.exitFullscreenVideo()
            return
        }

        if (findInPageBar.visibility == View.VISIBLE) {
            tabManager.getActiveTab()?.webView?.clearMatches()
            findInPageBar.visibility = View.GONE
            return
        }

        if (tabSwitcherOverlay.visibility == View.VISIBLE) {
            tabSwitcherOverlay.visibility = View.GONE
            return
        }

        val activeTab = tabManager.getActiveTab()
        val currentUrl = activeTab?.webView?.url ?: ""
        if (isYoutubeMixWatch(currentUrl) && activeTab != null) {
            activeTab.webView.evaluateJavascript(UserScriptManager.YOUTUBE_MIX_BACK_HOME_JS, null)
            return
        }

        if (activeTab != null && activeTab.webView.canGoBack()) {
            activeTab.webView.goBack()
            return
        }

        if (tabManager.count > 1 && activeTab != null) {
            tabManager.closeTab(this, activeTab.id)
            return
        }

        super.onBackPressed()
    }

    private fun isYoutubeMixWatch(url: String): Boolean {
        val u = url.lowercase()
        if (!u.contains("youtube.com") && !u.contains("youtu.be")) return false
        if (!u.contains("/watch")) return false
        return u.contains("list=rd") || u.contains("start_radio")
    }
}
