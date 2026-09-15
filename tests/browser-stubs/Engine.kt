package com.safeer.mobile.browser
import android.content.Context
import android.os.Bundle
import android.view.View
class ChromiumEngineView(context: Context): View(context) {
    companion object { var created = 0 }
    init { created++ }
    var isDesktopMode = false
    var isPlayingAudio = false
    var hasEditedForm = false
    var progress = 100
    var fullScreen = false
    var destroyed = false
    var url = ""
    var paused = false
    var onAudioStateChanged: ((Boolean) -> Unit)? = null
    var onRendererGone: (() -> Unit)? = null
    fun loadUrl(value: String) { check(!destroyed); url = value }
    fun restoreState(state: Bundle): Any? { url = state.url; return this }
    fun saveState(state: Bundle): Any? { check(!destroyed); state.url = url; return this }
    fun onPause() { paused = true }
    fun onResume() { paused = false }
    var deferSuspendCheck = false
    var pendingSuspendCheck: ((Boolean) -> Unit)? = null
    fun canSuspendSafely(callback: (Boolean) -> Unit) {
        if (deferSuspendCheck) pendingSuspendCheck = callback else callback(true)
    }
    fun isFullscreenVideoActive() = fullScreen
    fun exitFullscreenVideo() { fullScreen = false }
    fun destroy() { check(parent == null); check(!destroyed); destroyed = true }
}
object R { object string { const val tab_reload_after_crash = 1 } }
