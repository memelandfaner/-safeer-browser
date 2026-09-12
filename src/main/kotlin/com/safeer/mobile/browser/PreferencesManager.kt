package com.safeer.mobile.browser

import android.content.Context
import android.content.SharedPreferences
import java.net.URLEncoder

object PreferencesManager {

    private const val PREFS_NAME = "safeer_browser_preferences"

    const val SEARCH_GOOGLE = "google"
    const val SEARCH_DUCKDUCKGO = "duckduckgo"
    const val SEARCH_BRAVE = "brave"

    private const val KEY_LANGUAGE = "pref_language"
    private const val KEY_SEARCH_ENGINE = "pref_search_engine"
    private const val KEY_ADBLOCK_ENABLED = "pref_adblock_enabled"
    private const val KEY_DARK_MODE_ENABLED = "pref_dark_mode_enabled"
    private const val KEY_DESKTOP_DEFAULT = "pref_desktop_default"
    private const val KEY_THIRD_PARTY_COOKIES = "pref_third_party_cookies"
    private const val KEY_JAVASCRIPT_ENABLED = "pref_javascript_enabled"
    private const val KEY_TOTAL_ADS_BLOCKED = "pref_total_ads_blocked"
    private const val KEY_TOTAL_THREATS_BLOCKED = "pref_total_threats_blocked"
    private const val KEY_DOH_ENABLED = "pref_doh_enabled"
    private const val KEY_DOH_PROVIDER = "pref_doh_provider"
    private const val KEY_CUSTOM_DOH_URL = "pref_custom_doh_url"
    private const val KEY_SECURE_PROXY_MODE = "pref_secure_proxy_mode"
    private const val KEY_SECURE_PROXY_URL = "pref_secure_proxy_url"
    private const val KEY_ADGUARD_PROTECTION_ENABLED = "pref_adguard_protection_enabled"
    private const val KEY_SPONSORBLOCK_ENABLED = "pref_sponsorblock_enabled"
    private const val KEY_TEXT_ZOOM = "pref_text_zoom"
    private const val KEY_THEME = "pref_theme"
    private const val KEY_FONT_FAMILY = "pref_font_family"
    private const val KEY_BRAVE_MODE_ENABLED = "pref_brave_mode_enabled"

    const val THEME_DARK_SLATE = "dark_slate"
    const val THEME_AMOLED = "amoled"
    const val THEME_MIDNIGHT = "midnight"
    const val THEME_EMERALD = "emerald"

    const val FONT_SYSTEM = "system"
    const val FONT_SANS = "sans"
    const val FONT_SERIF = "serif"
    const val FONT_MONOSPACE = "monospace"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- 0. Jezik vmesnika ---
    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "auto") ?: "auto"
    }

    fun setLanguage(context: Context, lang: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, lang).apply()
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

    // --- 3.1. Povečava besedila strani ---
    fun getTextZoom(context: Context): Int {
        return getPrefs(context).getInt(KEY_TEXT_ZOOM, 100)
    }

    fun setTextZoom(context: Context, zoom: Int) {
        getPrefs(context).edit().putInt(KEY_TEXT_ZOOM, zoom).apply()
    }

    // --- 3.2. Izbira teme brskalnika ---
    fun getTheme(context: Context): String {
        return getPrefs(context).getString(KEY_THEME, THEME_DARK_SLATE) ?: THEME_DARK_SLATE
    }

    fun setTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString(KEY_THEME, theme).apply()
    }

    // --- 3.3. Izbor pisave (Font Family) ---
    fun getFontFamily(context: Context): String {
        return getPrefs(context).getString(KEY_FONT_FAMILY, FONT_SYSTEM) ?: FONT_SYSTEM
    }

    fun setFontFamily(context: Context, font: String) {
        getPrefs(context).edit().putString(KEY_FONT_FAMILY, font).apply()
    }

    // --- 3.4. Brave način (Brave Shield stil začetne strani) ---
    fun isBraveModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BRAVE_MODE_ENABLED, true)
    }

    fun setBraveModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BRAVE_MODE_ENABLED, enabled).apply()
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
        return getPrefs(context).getBoolean(KEY_THIRD_PARTY_COOKIES, true)
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

    // --- 8. Šifriran DNS (DoH) & Šifriran tunel (Tor / Proxy) ---
    fun isDohEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DOH_ENABLED, false)
    }

    fun setDohEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DOH_ENABLED, enabled).apply()
    }

    fun getDohProvider(context: Context): String {
        return getPrefs(context).getString(KEY_DOH_PROVIDER, "cloudflare") ?: "cloudflare"
    }

    fun setDohProvider(context: Context, provider: String) {
        getPrefs(context).edit().putString(KEY_DOH_PROVIDER, provider).apply()
    }

    fun getCustomDohUrl(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_DOH_URL, "https://1.1.1.1/dns-query") ?: "https://1.1.1.1/dns-query"
    }

    fun setCustomDohUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_DOH_URL, url).apply()
    }

    fun getSecureProxyMode(context: Context): String {
        return getPrefs(context).getString(KEY_SECURE_PROXY_MODE, "disabled") ?: "disabled"
    }

    fun setSecureProxyMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_SECURE_PROXY_MODE, mode).apply()
    }

    fun getSecureProxyUrl(context: Context): String {
        return getPrefs(context).getString(KEY_SECURE_PROXY_URL, "http://127.0.0.1:8080") ?: "http://127.0.0.1:8080"
    }

    fun setSecureProxyUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_SECURE_PROXY_URL, url).apply()
    }

    // --- 9. Vgrajena AdGuard Zaščita ---
    fun isAdguardProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ADGUARD_PROTECTION_ENABLED, true)
    }

    fun setAdguardProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ADGUARD_PROTECTION_ENABLED, enabled).apply()
    }

    // --- 10. SponsorBlock (preskakovanje sponzorskih odsekov na YouTubu) ---
    fun isSponsorBlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SPONSORBLOCK_ENABLED, true)
    }

    fun setSponsorBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SPONSORBLOCK_ENABLED, enabled).apply()
    }
}
