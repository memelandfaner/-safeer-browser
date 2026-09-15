package com.safeer.mobile.browser

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.URI
import java.util.Base64

data class SavedTab(val id: String, val url: String, val title: String, val desktop: Boolean)
data class TabSession(val tabs: List<SavedTab>, val activeId: String?)

/** Only navigation metadata is persisted, never form values, POST bodies or page scripts. */
object TabSessionCodec {
    const val HOME = "file:///android_asset/brave_home.html"
    private const val MAX_TABS = 200
    private const val MAX_ENCODED_SIZE = 8 * 1024 * 1024

    fun safeUrl(url: String): String {
        if (url == HOME || url == "about:blank") return url
        return try {
            val uri = URI(url)
            if (url.length <= 8192 && uri.scheme?.lowercase() in setOf("https", "http") &&
                !uri.host.isNullOrEmpty() && uri.userInfo == null) url else HOME
        } catch (_: Exception) { HOME }
    }

    fun encode(session: TabSession): String {
        // Keep the active tab even when the session exceeds the persistence limit.
        val active = session.tabs.find { it.id == session.activeId }
        val selected = session.tabs.take(MAX_TABS).toMutableList()
        if (active != null && active !in selected) selected[selected.lastIndex] = active
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(1)
            out.writeUTF(session.activeId.orEmpty().take(80))
            out.writeInt(selected.size)
            selected.forEach {
                out.writeUTF(it.id.take(80))
                out.writeUTF(safeUrl(it.url))
                out.writeUTF(it.title.take(512))
                out.writeBoolean(it.desktop)
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    fun decode(value: String?): TabSession {
        val empty = TabSession(emptyList(), null)
        if (value.isNullOrEmpty() || value.length > MAX_ENCODED_SIZE) return empty
        return try {
            DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(value))).use { input ->
                require(input.readInt() == 1)
                val active = input.readUTF()
                val count = input.readInt()
                require(count in 0..MAX_TABS)
                val tabs = (0 until count).map {
                    SavedTab(input.readUTF().take(80), safeUrl(input.readUTF()), input.readUTF().take(512), input.readBoolean())
                }
                require(tabs.all { it.id.isNotEmpty() } && tabs.map { it.id }.distinct().size == tabs.size)
                require(input.available() == 0)
                TabSession(tabs, active.takeIf { id -> tabs.any { it.id == id } } ?: tabs.firstOrNull()?.id)
            }
        } catch (_: Exception) { empty }
    }
}
