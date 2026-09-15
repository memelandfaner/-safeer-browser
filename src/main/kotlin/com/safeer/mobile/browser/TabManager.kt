package com.safeer.mobile.browser

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import java.util.UUID

class TabModel(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Začetna stran",
    var url: String = TabSessionCodec.HOME,
    var isDesktop: Boolean = false,
    var favicon: String = "🦁",
    var isPlayingAudio: Boolean = false
) {
    var loadedWebView: ChromiumEngineView? = null
        internal set
    internal lateinit var createView: () -> ChromiumEngineView
    val webView: ChromiumEngineView get() = loadedWebView ?: createView()
    internal var savedState: Bundle? = null
    internal var lastUsed = SystemClock.elapsedRealtime()
}

class TabManager(
    private val container: FrameLayout,
    private val onTabsUpdated: (count: Int, activeTab: TabModel?) -> Unit
) {
    private val tabs = mutableListOf<TabModel>()
    private var activeTabId: String? = null
    private val context = container.context
    private val prefs = context.getSharedPreferences("safeer_tab_session", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var restoring = false
    private var disposed = false
    private val saveTask = Runnable { saveSession() }
    private val sleepTask = object : Runnable {
        override fun run() {
            if (disposed) return
            suspendInactiveTabs()
            handler.postDelayed(this, 60_000L)
        }
    }

    /** Configure callbacks before the first navigation, including restored/background tabs. */
    var onViewCreated: ((TabModel) -> Unit)? = null
    var onAudioStateChanged: ((tab: TabModel, playing: Boolean) -> Unit)? = null

    init { handler.postDelayed(sleepTask, 60_000L) }

    fun getPlayingTab(): TabModel? {
        fun playing(tab: TabModel) = tab.isPlayingAudio || tab.loadedWebView?.isPlayingAudio == true
        return getActiveTab()?.takeIf { playing(it) } ?: tabs.firstOrNull { playing(it) }
    }

    val count: Int get() = tabs.size

    private fun addModel(saved: SavedTab): TabModel {
        val tab = TabModel(id = saved.id, url = saved.url, title = saved.title, isDesktop = saved.desktop)
        tab.createView = { ensureView(tab) }
        tabs.add(tab)
        return tab
    }

    fun createTab(context: Context, url: String = TabSessionCodec.HOME, makeActive: Boolean = true): TabModel {
        val tab = addModel(SavedTab(UUID.randomUUID().toString(), url, "Nov zavihek", PreferencesManager.isDesktopModeDefault(context)))
        if (makeActive || tabs.size == 1) switchTab(tab.id) else notifyUpdated()
        return tab
    }

    private fun ensureView(tab: TabModel): ChromiumEngineView {
        check(!disposed && tab in tabs)
        tab.loadedWebView?.let { return it }
        val view = ChromiumEngineView(context)
        tab.loadedWebView = view
        view.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        view.isDesktopMode = tab.isDesktop
        view.onAudioStateChanged = { playing ->
            if (tab.loadedWebView === view) {
                tab.isPlayingAudio = playing
                if (!playing && activeTabId != tab.id) view.onPause()
                onAudioStateChanged?.invoke(tab, playing)
            }
        }
        view.onRendererGone = { rendererGone(tab, view) }
        onViewCreated?.invoke(tab)
        if (activeTabId == tab.id) attach(view)
        val state = tab.savedState
        tab.savedState = null
        val restored = state != null && try { view.restoreState(state) != null } catch (_: Exception) { false }
        if (!restored && tab.url.isNotEmpty() && tab.url != "about:blank") view.loadUrl(tab.url)
        return view
    }

    private fun attach(view: ChromiumEngineView) {
        container.removeAllViews()
        (view.parent as? ViewGroup)?.removeView(view)
        container.addView(view)
    }

    fun switchTab(tabId: String) {
        val target = tabs.find { it.id == tabId } ?: return
        getActiveTab()?.takeIf { it.id != tabId }?.let { old ->
            old.lastUsed = SystemClock.elapsedRealtime()
            if (!old.isPlayingAudio && old.loadedWebView?.isPlayingAudio != true) old.loadedWebView?.onPause()
        }
        activeTabId = tabId
        target.lastUsed = SystemClock.elapsedRealtime()
        val view = ensureView(target)
        attach(view)
        view.onResume()
        notifyUpdated()
    }

    /** Save only URLs/titles; background pages remain unloaded until selected. */
    fun restoreSession(): Boolean {
        if (tabs.isNotEmpty()) return true
        val session = TabSessionCodec.decode(prefs.getString("session", null))
        if (session.tabs.isEmpty()) return false
        restoring = true
        try {
            session.tabs.forEach { addModel(it) }
            switchTab(session.activeId ?: tabs.first().id)
        } finally { restoring = false }
        scheduleSave()
        return true
    }

    fun scheduleSave() {
        if (restoring || disposed) return
        handler.removeCallbacks(saveTask)
        handler.postDelayed(saveTask, 400L)
    }

    fun saveSession() {
        if (restoring || disposed || tabs.isEmpty()) return
        handler.removeCallbacks(saveTask)
        val data = TabSession(tabs.map { SavedTab(it.id, it.url, it.title, it.isDesktop) }, activeTabId)
        prefs.edit().putString("session", TabSessionCodec.encode(data)).apply()
    }

    /** Keep music, fullscreen video and edited forms alive. Back/forward history stays in memory. */
    fun suspendInactiveTabs(memoryPressure: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        tabs.filter { it.id != activeTabId }.forEach { tab ->
            val view = tab.loadedWebView ?: return@forEach
            if (tab.isPlayingAudio || view.isPlayingAudio || view.isFullscreenVideoActive() || view.hasEditedForm) return@forEach
            if (!memoryPressure && now - tab.lastUsed < 10 * 60_000L) return@forEach
            if (view.progress < 100 || AuthenticationPages.isAuthenticationPage(tab.url)) return@forEach
            val candidateUrl = tab.url
            view.canSuspendSafely { safe ->
                // The JS answer arrives later: the user may have selected, navigated or closed this tab.
                if (!safe || disposed || tab !in tabs || tab.id == activeTabId || tab.loadedWebView !== view ||
                    tab.url != candidateUrl || tab.isPlayingAudio || view.isPlayingAudio || view.hasEditedForm ||
                    view.isFullscreenVideoActive() || view.progress < 100) return@canSuspendSafely
                val state = Bundle()
                if (try { view.saveState(state) == null } catch (_: Exception) { true }) return@canSuspendSafely
                tab.savedState = state
                release(tab, view)
            }
        }
        saveSession()
    }

    private fun release(tab: TabModel, view: ChromiumEngineView) {
        tab.loadedWebView = null
        tab.isPlayingAudio = false
        view.onAudioStateChanged = null
        view.onRendererGone = null
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
    }

    private fun rendererGone(tab: TabModel, view: ChromiumEngineView) {
        if (tab.loadedWebView !== view) return
        view.exitFullscreenVideo()
        tab.savedState = null
        release(tab, view)
        onAudioStateChanged?.invoke(tab, false)
        if (tab.id == activeTabId) {
            // No automatic crash/reload loop. The user decides when to retry.
            container.addView(Button(context).apply {
                setText(R.string.tab_reload_after_crash)
                setOnClickListener { switchTab(tab.id) }
            })
        }
        notifyUpdated()
    }

    fun closeTab(context: Context, tabId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return
        val tab = tabs[index]
        tab.loadedWebView?.let { release(tab, it) }
        tabs.removeAt(index)
        if (tabs.isEmpty()) createTab(context)
        else if (activeTabId == tabId) switchTab(tabs[minOf(index, tabs.lastIndex)].id)
        else notifyUpdated()
        onAudioStateChanged?.invoke(tab, false)
    }

    fun closeAllTabs(context: Context) {
        tabs.forEach { tab -> tab.loadedWebView?.let { release(tab, it) } }
        tabs.clear()
        container.removeAllViews()
        activeTabId = null
        createTab(context)
        onAudioStateChanged?.invoke(getActiveTab()!!, false)
        saveSession()
    }

    fun dispose() {
        saveSession()
        disposed = true
        handler.removeCallbacksAndMessages(null)
        tabs.forEach { tab -> tab.loadedWebView?.let { release(tab, it) } }
        tabs.clear()
    }

    fun getActiveTab(): TabModel? = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()
    fun getAllTabs(): List<TabModel> = tabs.toList()
    fun switchToNextTab() = switchRelative(1)
    fun switchToPrevTab() = switchRelative(-1)
    private fun switchRelative(offset: Int) {
        if (tabs.size <= 1) return
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index >= 0) switchTab(tabs[(index + offset + tabs.size) % tabs.size].id)
    }
    private fun notifyUpdated() {
        onTabsUpdated(tabs.size, getActiveTab())
        scheduleSave()
    }
}
