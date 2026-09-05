package com.safeer.mobile.browser

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
        ".adsbygoogle", "ins.adsbygoogle", "[id^='google_ads']", "[id^='div-gpt-ad']", "[class*='google-ad']",
        "[data-ad-client]", "[data-ad-slot]", "[data-ad-format]",
        "iframe[id*='google_ads_iframe']", "iframe[src*='doubleclick']", "iframe[src*='googlesyndication']",
        "iframe[src*='adservice']", "iframe[src*='adnxs']", "iframe[src*='monetag']",
        "iframe[src*='propellerads']", "iframe[src*='exoclick']", "iframe[src*='trafficjunky']",
        ".a4bIc-ad", ".commercial-unit", ".ad-slot",
        ".ad-container:not(#player):not(#player-container):not(#player-container-id):not(.html5-video-player)",
        ".ad-wrapper:not(#player):not(#player-container):not(.html5-video-player)",
        "[class*='ad_container']:not(#player):not(#player-container):not(#player-container-id):not(.html5-video-player)",
        
        // Sponzorirane vsebine & Native Ads (Taboola, Outbrain)
        ".taboola", ".outbrain", ".trc_rbox_container",
        "[data-ad]:not(#movie_player):not(.html5-video-player):not(#player-container)",
        "[data-ad-unit]",
        ".sponsored-post", ".sponsored-content", ".promoted-tweet", ".promoted-post",
        "[class*='sponsored-wrapper']", "[class*='sponsored_post']", "[class*='native-ad']",
        
        // Popunder, Banner, In-Page Push & Floating Ad elementi
        ".pop-under", ".ad-banner", ".banner-ad", ".sticky-ad", ".floating-banner",
        ".monetag-banner", ".richpush-banner", ".interstitial-ad", ".ad-modal",
        "[class*='reward-zone']", "[id*='reward-zone']", "[class*='floating-ad']",
        "[class*='gamify-ad']", "[class*='coin-chest']", "[class*='ad-floating']",
        "[class*='ad-leaderboard']", "[class*='ad-rectangle']", "[class*='ad-sidebar']",
        
        // Lažni gumbi za prenos (Fake Download Ads)
        "[class*='fake-download']", "[id*='fake-download']", ".download-button-ad",
        "div[class*='download-arrow']",
        
        // YouTube elementi & samodejni vklop zvoka (skrit mute gumb & popolnoma odstranjen Odpri aplikacijo gumb)
        ".ytp-ad-overlay-container", ".ytp-ad-message-container", ".ytp-ad-text",
        ".ytp-ad-player-overlay", ".ytp-ad-preview-container", ".ytp-ad-image-overlay",
        /* skip buttons intentionally excluded — kept clickable via buildCosmeticCss override */
        "ytm-ad-slot-renderer", "ytm-promoted-video-renderer", "ytm-companion-ad-renderer",
        ".ytd-ad-slot-renderer", "ytd-in-feed-ad-layout-renderer", "ytd-banner-promo-renderer",
        "ytd-player-legacy-desktop-watch-ads-renderer", ".ytd-display-ad-renderer",
        ".ytp-unmute", ".ytp-unmute-inner", ".ytp-unmute-animated", ".ytp-unmute-box", "button[aria-label*='Vklopite zvok']", "button[aria-label*='Unmute']",
        "ytm-open-app-button", "ytm-app-promo-renderer", "ytm-mealbar-promo-renderer", "ytm-upsell-dialog-renderer",
        "button[aria-label*='Odpri aplikacijo']", "button[aria-label*='Odpri v aplikaciji']",
        "button[aria-label*='Open app']", "button[aria-label*='Open in app']",
        ".topbar-action-buttons ytm-open-app-button",
        "ytm-mobile-topbar-renderer ytm-open-app-button",
        "a[href*='app_redirect']", "a[href*='open_in_app']",
        ".contribYtLightShapeStaticWashLight", ".cinematic-renderer", "#cinematic-container", "[class*='WashLight']",

        // YouTube: samo oglasi in pause overlay. Mixa/seznama ne skrivaj — sicer klik javi napako.
        ".ytp-pause-overlay", ".ytp-pause-overlay-container",
        "ytm-companion-ad-renderer",
        "ytm-promoted-sparkles-web-renderer", "ytm-paid-content-overlay-renderer",

        // 🎬 Oglasni bannerji in zunanji oglasni elementi (brez vpliva na sam video predvajalnik)
        ".removeAds", "a[href*='casino']", ".topAd", ".bottomAd", ".wideBanner", ".underPlayerAd",
        ".commercial-unit", ".ad-zone", "[class*='ad-banner']", ".ad-banner-overlay",
        ".jw-ad-container", ".plyr__ad", ".vjs-ad", ".video-ad-overlay",
        
        // AdBlock opozorila in prekrivna okna
        ".fc-ab-root", ".adblock-overlay", "#adblock-modal", ".ad-block-warning",
        ".adblock-notice", ".adblock-banner", "[id*='adblock-banner']"
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
            video, audio, #playerContainer, .mgp_container, .mgp_player, .video-stream {
                display: block !important;
                visibility: visible !important;
                opacity: 1 !important;
            }
            .ytp-skip-ad-button, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-ad-skip-button-slot,
            .ytp-skip-ad-button__text, button.ytp-ad-skip-button-text, .ytp-ad-skip-button-container,
            button[aria-label*="Preskoči"], button[aria-label*="Skip ad"], button[aria-label*="Skip ads"] {
                display: flex !important;
                visibility: visible !important;
                opacity: 1 !important;
                pointer-events: auto !important;
                position: relative !important;
                left: auto !important;
                height: auto !important;
                min-height: 0 !important;
                max-height: none !important;
            }
            ytm-playlist-panel-renderer,
            ytm-engagement-panel-section-list-renderer[target-id='engagement-panel-playlist-panel'] {
                max-height: 42vh !important;
                overflow-y: auto !important;
                position: relative !important;
                top: auto !important;
                bottom: auto !important;
            }
            ytm-miniplayer, ytm-miniplayer-bar-renderer, ytm-miniplayer-controls-renderer {
                display: none !important;
                pointer-events: none !important;
            }
            .mgp_container.mgp_playingState .mgp_loadingSpinner,
            .mgp_container.mgp_playingState .mgp_bufferingState,
            .mgp_container.mgp_playingState::after {
                display: none !important;
                opacity: 0 !important;
                visibility: hidden !important;
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
