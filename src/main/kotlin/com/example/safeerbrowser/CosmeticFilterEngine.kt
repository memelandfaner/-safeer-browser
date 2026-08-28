package com.example.safeerbrowser

/**
 * 🎨 CosmeticFilterEngine
 * Generira in injicira EasyList CSS element hiding pravila v spletne strani za popolno odstranitev praznih oglasnih okvirjev,
 * lažnih gumbov za prenos (Fake Download Buttons), oglasnih srcdoc okvirjev in lebdečih promocijskih pripomočkov (Reward Zone / Floating Popups).
 */
object CosmeticFilterEngine {

    var isEnabled: Boolean = true

    // Splošna EasyList pravila za skrivanje oglasnih elementov
    private val GENERIC_ELEMENT_HIDING_RULES = listOf(
        // Google Ads & DFP
        ".adsbygoogle", "[id^='google_ads']", "[id^='div-gpt-ad']", "[class*='google-ad']",
        ".a4bIc-ad", ".commercial-unit", ".ad-slot", ".ad-container", ".ad-wrapper",
        
        // Sponzorirane vsebine & Native Ads (Taboola, Outbrain)
        ".taboola", ".outbrain", ".trc_rbox_container", "[data-ad]", "[data-ad-unit]",
        ".sponsored-post", ".sponsored-content", ".promoted-tweet", ".promoted-post",
        
        // Popunder, Banner, In-Page Push & Floating Ad elementi
        ".pop-under", ".ad-banner", ".banner-ad", ".sticky-ad", ".floating-banner",
        ".monetag-banner", ".richpush-banner", ".interstitial-ad",
        "[class*='reward-zone']", "[id*='reward-zone']", "[class*='floating-ad']",
        "[class*='gamify-ad']", "[class*='coin-chest']", "[class*='ad-floating']",
        
        // Lažni gumbi za prenos (Fake Download Ads)
        "[class*='fake-download']", "[id*='fake-download']", ".download-button-ad",
        "div[class*='download-arrow']",
        
        // YouTube elementi
        ".ytp-ad-overlay-container", ".ytp-ad-message-container", ".ytp-ad-text",
        ".ytd-ad-slot-renderer", "ytd-in-feed-ad-layout-renderer", "ytd-banner-promo-renderer",
        "ytd-player-legacy-desktop-watch-ads-renderer", ".ytd-display-ad-renderer",
        
        // AdBlock opozorila in prekrivna okna
        ".fc-ab-root", ".adblock-overlay", "#adblock-modal", ".ad-block-warning"
    )

    /**
     * Zgradi strnjen CSS niz za injiciranje v spletno stran.
     */
    fun buildCosmeticCss(): String {
        if (!isEnabled) return ""
        val selectors = GENERIC_ELEMENT_HIDING_RULES.joinToString(", ")
        return """
            $selectors {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                min-height: 0 !important;
                max-height: 0 !important;
                opacity: 0 !important;
                pointer-events: none !important;
                position: absolute !important;
                left: -9999px !important;
            }
        """.trimIndent()
    }

    /**
     * Zgradi JavaScript ukaz za takojšnjo uveljavitev CSS pravil.
     */
    fun buildInjectionScript(): String {
        val css = buildCosmeticCss().replace("\n", " ").replace("\"", "\\\"")
        return """
            (function() {
                try {
                    var styleId = 'safeer-cosmetic-filter';
                    var existing = document.getElementById(styleId);
                    if (!existing) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.type = 'text/css';
                        style.innerHTML = "$css";
                        (document.head || document.documentElement).appendChild(style);
                    }
                } catch(_) {}
            })();
        """.trimIndent()
    }
}
