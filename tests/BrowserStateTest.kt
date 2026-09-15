package com.safeer.mobile.browser

import android.content.Context
import android.os.SystemClock
import android.widget.FrameLayout
import android.widget.Button

fun main() {
    val context = Context()
    val container = FrameLayout(context)
    val manager = TabManager(container) { _, _ -> }
    var configured = 0
    manager.onViewCreated = { check(it.loadedWebView != null); configured++ }
    val first = manager.createTab(context, "https://example.org/one")
    first.title = "First"
    val firstView = first.webView
    val second = manager.createTab(context, "https://example.org/two")
    second.isDesktop = true
    check(firstView.paused && container.children.single() === second.webView)
    first.isPlayingAudio = true
    firstView.isPlayingAudio = true
    SystemClock.time = 11 * 60_000
    manager.suspendInactiveTabs()
    check(first.loadedWebView === firstView) { "music was discarded" }
    first.isPlayingAudio = false
    firstView.isPlayingAudio = false
    firstView.hasEditedForm = true
    manager.suspendInactiveTabs(memoryPressure = true)
    check(first.loadedWebView === firstView) { "edited form was discarded" }
    firstView.hasEditedForm = false
    firstView.deferSuspendCheck = true
    manager.suspendInactiveTabs()
    manager.switchTab(first.id)
    firstView.pendingSuspendCheck!!(true)
    check(!firstView.destroyed) { "late sleep callback discarded the newly active tab" }
    manager.switchTab(second.id)
    firstView.deferSuspendCheck = false
    SystemClock.time += 11 * 60_000
    manager.suspendInactiveTabs()
    check(first.loadedWebView == null && firstView.destroyed)
    manager.switchTab(first.id)
    check(first.webView !== firstView && first.webView.url == first.url)
    manager.switchTab(second.id)
    manager.saveSession()
    manager.dispose()
    val before = ChromiumEngineView.created
    val restoredContainer = FrameLayout(context)
    val restored = TabManager(restoredContainer) { _, _ -> }
    check(restored.restoreSession())
    check(ChromiumEngineView.created == before + 1) { "background tabs loaded on restart" }
    check(restored.getActiveTab()?.id == second.id && restored.getActiveTab()?.webView?.isDesktopMode == true)
    check(restored.getAllTabs()[0].loadedWebView == null && restored.getAllTabs()[0].title == "First")
    val crashing = restored.getActiveTab()!!.webView
    crashing.onRendererGone!!()
    check(crashing.destroyed && restored.getActiveTab()?.loadedWebView == null)
    check(restoredContainer.children.single() is Button) { "missing user-controlled retry" }
    (restoredContainer.children.single() as Button).action!!()
    check(restored.getActiveTab()?.webView !== crashing)
    restored.closeTab(context, first.id)
    restored.saveSession()
    restored.dispose()
    val remaining = TabManager(FrameLayout(context)) { _, _ -> }
    check(remaining.restoreSession() && remaining.count == 1)
    remaining.closeAllTabs(context)
    remaining.dispose()
    val cleared = TabManager(FrameLayout(context)) { _, _ -> }
    check(cleared.restoreSession() && cleared.count == 1 && cleared.getActiveTab()?.url == TabSessionCodec.HOME)
    cleared.dispose()
    check(configured == 3)
    val threads = (1..8).map { Thread { repeat(2000) {
        PreferencesManager.incrementAdsBlocked(context)
        PreferencesManager.incrementThreatsBlocked(context)
    } }.apply { start() } }
    threads.forEach { it.join() }
    check(PreferencesManager.getTotalAdsBlocked(context) == 16000L)
    check(PreferencesManager.getTotalThreatsBlocked(context) == 16000L)
    println("PASS: lazy tabs, audio/form preservation, sleep/wake, crash/retry, close/clear persistence, concurrent counters")
}
