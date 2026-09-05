package com.safeer.mobile.browser

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.UUID

data class TabModel(
    val id: String = UUID.randomUUID().toString(),
    val webView: ChromiumEngineView,
    var title: String = "Začetna stran",
    var url: String = "file:///android_asset/brave_home.html",
    var isDesktop: Boolean = false,
    var favicon: String = "🦁",
    var isPlayingAudio: Boolean = false
)

class TabManager(
    private val container: FrameLayout,
    private val onTabsUpdated: (count: Int, activeTab: TabModel?) -> Unit
) {

    private val tabs = mutableListOf<TabModel>()
    private var activeTabId: String? = null

    val count: Int get() = tabs.size

    fun createTab(context: Context, url: String = "file:///android_asset/brave_home.html", makeActive: Boolean = true): TabModel {
        val webView = ChromiumEngineView(context)
        webView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val tab = TabModel(
            webView = webView,
            title = "Nov zavihek",
            url = url
        )
        webView.isDesktopMode = tab.isDesktop
        webView.onAudioStateChanged = { playing ->
            tab.isPlayingAudio = playing
            if (!playing && activeTabId != tab.id) {
                try { tab.webView.onPause() } catch (_: Exception) {}
            }
        }

        tabs.add(tab)

        if (makeActive || tabs.size == 1) {
            switchTab(tab.id)
        }

        if (url.isNotEmpty() && url != "about:blank") {
            webView.loadUrl(url)
        }
        notifyUpdated()
        return tab
    }

    fun switchTab(tabId: String) {
        val target = tabs.find { it.id == tabId } ?: return
        if (activeTabId != null && activeTabId != tabId) {
            val oldTab = tabs.find { it.id == activeTabId }
            if (oldTab != null && !oldTab.isPlayingAudio) {
                try { oldTab.webView.onPause() } catch (_: Exception) {}
            }
        }
        activeTabId = tabId

        container.removeAllViews()
        if (target.webView.parent != null) {
            (target.webView.parent as? ViewGroup)?.removeView(target.webView)
        }
        container.addView(target.webView)
        try { target.webView.onResume() } catch (_: Exception) {}

        notifyUpdated()
    }

    fun closeTab(context: Context, tabId: String) {
        val idx = tabs.indexOfFirst { it.id == tabId }
        if (idx == -1) return

        val tabToClose = tabs[idx]
        tabToClose.webView.destroy()
        tabs.removeAt(idx)

        if (tabs.isEmpty()) {
            createTab(context, "https://www.google.com", true)
        } else if (activeTabId == tabId) {
            val nextIdx = if (idx < tabs.size) idx else tabs.size - 1
            switchTab(tabs[nextIdx].id)
        } else {
            notifyUpdated()
        }
    }

    fun closeAllTabs(context: Context) {
        for (tab in tabs) {
            tab.webView.destroy()
        }
        tabs.clear()
        container.removeAllViews()
        createTab(context, "https://www.google.com", true)
    }

    fun getActiveTab(): TabModel? {
        return tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()
    }

    fun getAllTabs(): List<TabModel> = tabs.toList()

    fun switchToNextTab() {
        if (tabs.size <= 1) return
        val curIdx = tabs.indexOfFirst { it.id == activeTabId }
        if (curIdx != -1) {
            val nextIdx = (curIdx + 1) % tabs.size
            switchTab(tabs[nextIdx].id)
        }
    }

    fun switchToPrevTab() {
        if (tabs.size <= 1) return
        val curIdx = tabs.indexOfFirst { it.id == activeTabId }
        if (curIdx != -1) {
            val prevIdx = (curIdx - 1 + tabs.size) % tabs.size
            switchTab(tabs[prevIdx].id)
        }
    }

    private fun notifyUpdated() {
        onTabsUpdated(tabs.size, getActiveTab())
    }
}
