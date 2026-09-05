package com.safeer.mobile.browser

import android.content.Context
import android.content.SharedPreferences
import java.net.URLEncoder

object PreferencesManager {

    private const val PREFS_NAME = "safeer_browser_preferences"

    const val SEARCH_GOOGLE = "google"
    const val SEARCH_DUCKDUCKGO = "duckduckgo"
    const val SEARCH_BRAVE = "brave"

    private const val KEY_SEARCH_ENGINE = "pref_search_engine"
    private const val KEY_ADBLOCK_ENABLED = "pref_adblock_enabled"
    private const val KEY_DARK_MODE_ENABLED = "pref_dark_mode_enabled"
    private const val KEY_DESKTOP_DEFAULT = "pref_desktop_default"
    private const val KEY_THIRD_PARTY_COOKIES = "pref_third_party_cookies"
    private const val KEY_JAVASCRIPT_ENABLED = "pref_javascript_enabled"
    private const val KEY_TOTAL_ADS_BLOCKED = "pref_total_ads_blocked"
    private const val KEY_TOTAL_THREATS_BLOCKED = "pref_total_threats_blocked"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- 1. Privzeti Iskalnik ---
    fun getSearchEngine(context: Context): String {
        return getPrefs(context).getString(KEY_SEARCH_ENGINE, SEARCH_GOOGLE) ?: SEARCH_GOOGLE
    }

    fun setSearchEngine(context: Context, engine: String) {
        getPrefs(context).edit().putString(KEY_SEARCH_ENGINE, engine).apply()
    }

    fun buildSearchUrl(context: Context, query: String): String {
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query
        }
        return when (getSearchEngine(context)) {
            SEARCH_DUCKDUCKGO -> "https://duckduckgo.com/?q=$encoded"
            SEARCH_BRAVE -> "https://search.brave.com/search?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    // --- 2. AdBlock & Varnostni ščit ---
    fun isAdBlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ADBLOCK_ENABLED, true)
    }

    fun setAdBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ADBLOCK_ENABLED, enabled).apply()
    }

    // --- 3. AMOLED Temni način ---
    fun isDarkModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DARK_MODE_ENABLED, true)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply()
    }

    // --- 4. Namizni način ---
    fun isDesktopModeDefault(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DESKTOP_DEFAULT, false)
    }

    fun setDesktopModeDefault(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DESKTOP_DEFAULT, enabled).apply()
    }

    // --- 5. Piškotki tretjih oseb ---
    fun isThirdPartyCookiesEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_THIRD_PARTY_COOKIES, false)
    }

    fun setThirdPartyCookiesEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_THIRD_PARTY_COOKIES, enabled).apply()
    }

    // --- 6. JavaScript izvajanje ---
    fun isJavaScriptEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_JAVASCRIPT_ENABLED, true)
    }

    fun setJavaScriptEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_JAVASCRIPT_ENABLED, enabled).apply()
    }

    // --- 7. Kumulativna statistika blokiranih oglasov in groženj ---
    fun getTotalAdsBlocked(context: Context): Long {
        return getPrefs(context).getLong(KEY_TOTAL_ADS_BLOCKED, 0L)
    }

    fun incrementAdsBlocked(context: Context, count: Long = 1L) {
        val current = getTotalAdsBlocked(context)
        getPrefs(context).edit().putLong(KEY_TOTAL_ADS_BLOCKED, current + count).apply()
    }

    fun getTotalThreatsBlocked(context: Context): Long {
        return getPrefs(context).getLong(KEY_TOTAL_THREATS_BLOCKED, 0L)
    }

    fun incrementThreatsBlocked(context: Context, count: Long = 1L) {
        val current = getTotalThreatsBlocked(context)
        getPrefs(context).edit().putLong(KEY_TOTAL_THREATS_BLOCKED, current + count).apply()
    }
}
