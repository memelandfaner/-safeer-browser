package com.example.safeerbrowser

import android.webkit.WebView

object UserScriptManager {

    private const val DARK_MODE_AMOLED_CSS = """
        /* Samsung Galaxy AMOLED True Black Engine */
        html, body {
            background-color: #000000 !important;
            color: #f1f5f9 !important;
        }
    """

    private const val ANTI_POPUNDER_SHIELD_JS = """
        /* 🛡️ Safeer Anti-Popunder, Anti-Clickjacking & Streaming Shield Engine */
        (function() {
            if (window._safeer_popunder_shield_active) return;
            window._safeer_popunder_shield_active = true;

            // 🚫 1. Popolna nevtralizacija window.open popunderjev
            try {
                window.open = function(url, target, features) {
                    console.log('[Safeer AdBlock] Preprečen window.open:', url);
                    return null;
                };
            } catch(e) {}

            // 🚫 2. Zaščita pred ugrabitvijo top.location iz vdelanih okvirjev (iframes)
            try {
                if (window.top !== window.self) {
                    Object.defineProperty(window, 'top', {
                        get: function() { return window.self; },
                        set: function() {},
                        configurable: true
                    });
                    Object.defineProperty(window, 'parent', {
                        get: function() { return window.self; },
                        set: function() {},
                        configurable: true
                    });
                }
            } catch(e) {}

            // 🚫 3. Samodejno odstranjevanje lažnih opozoril, vsiljenih modalov, dating oglasov in lažnih gumbov
            function cleanAllAdOverlays() {
                try {
                    // Preišči vse elemente na strani
                    var all = document.querySelectorAll('div, section, dialog, [role="dialog"], [role="alertdialog"], span, a, button, p');
                    for (var i = 0; i < all.length; i++) {
                        var el = all[i];
                        var tag = el.tagName.toLowerCase();
                        if (tag === 'html' || tag === 'body' || tag === 'head' || tag === 'script' || tag === 'style') continue;

                        var txt = (el.innerText || el.textContent || '').trim().toLowerCase();
                        var cls = (el.className || '').toString().toLowerCase();
                        var id = (el.id || '').toLowerCase();

                        // 1. Zaznaj nezaželena sporočila (Whatsapp za seks, Lara, Attention, Fake confirm, Reward Zone)
                        var isUnwanted = (
                            txt.includes('whatsapp za seks') ||
                            txt.includes('lara (2 km') ||
                            txt.includes('jebi me zastonj') ||
                            txt.includes('whatsapp za') ||
                            (txt.includes('whatsapp') && (txt.includes('seks') || txt.includes('sex') || txt.includes('zastonj') || txt.includes('lara'))) ||
                            txt.includes('jebi me') ||
                            txt.includes('kurbe') ||
                            txt.includes('reward zone') ||
                            cls.includes('reward-zone') ||
                            id.includes('reward-zone') ||
                            (txt.includes('attention') && txt.includes('confirm to continue')) ||
                            (txt.includes('please confirm') && txt.includes('continue')) ||
                            (txt.includes('click allow') || txt.includes('press allow')) ||
                            (txt.includes('disable your ad blocker') || txt.includes('turn off adblock')) ||
                            (txt.includes('vpn recommended') || txt.includes('battery damaged') || txt.includes('virus detected'))
                        );

                        if (isUnwanted) {
                            // Poišči krovni plavajoči dialog
                            var topDialog = el;
                            while (topDialog.parentElement && topDialog.parentElement !== document.body && topDialog.parentElement !== document.documentElement) {
                                var pStyle = window.getComputedStyle(topDialog.parentElement);
                                if (pStyle.position === 'fixed' || pStyle.position === 'absolute' || parseInt(pStyle.zIndex, 10) > 10) {
                                    topDialog = topDialog.parentElement;
                                } else {
                                    break;
                                }
                            }
                            topDialog.remove();
                            continue;
                        }

                        // 2. Odstrani lažne lebdeče gumbe za prenos
                        if ((txt === 'download' || txt === 'prenesi') && (el.querySelectorAll('video, iframe, form').length === 0)) {
                            var s = window.getComputedStyle(el);
                            if (s.position === 'fixed' || parseInt(s.zIndex, 10) > 50) {
                                el.remove();
                            }
                        }
                    }

                    // 3. Odstrani oglasne iframe okvirje (srcdoc ali lebdeče overlay iframe-e)
                    var iframes = document.querySelectorAll('iframe');
                    for (var k = 0; k < iframes.length; k++) {
                        var ifr = iframes[k];
                        var src = (ifr.getAttribute('src') || ifr.src || '').toLowerCase();
                        var srcdoc = ifr.getAttribute('srcdoc');
                        var isFixed = false;
                        try {
                            var ifStyle = window.getComputedStyle(ifr);
                            if (ifStyle.position === 'fixed' || (ifStyle.position === 'absolute' && parseInt(ifStyle.zIndex, 10) > 20)) {
                                isFixed = true;
                            }
                        } catch(e) {}

                        if (srcdoc != null || src.includes('srcdoc') || (isFixed && !src.includes('embed') && !src.includes('player') && !src.includes('streamex') && !src.includes('vidgod'))) {
                            ifr.remove();
                        }
                    }

                    // 4. Odstrani nevidne prekrivne plasti (Invisible Click-Jacking Overlays)
                    var allFixed = document.querySelectorAll('div, a, span, button');
                    var winW = window.innerWidth || 1000;
                    var winH = window.innerHeight || 800;
                    for (var j = 0; j < allFixed.length; j++) {
                        var fx = allFixed[j];
                        try {
                            var style = window.getComputedStyle(fx);
                            if (style.position === 'fixed' || style.position === 'absolute') {
                                var z = parseInt(style.zIndex, 10) || 0;
                                var op = parseFloat(style.opacity);
                                var rect = fx.getBoundingClientRect();
                                if (z >= 99 && (op === 0 || style.visibility === 'hidden') && rect.width >= winW * 0.5 && rect.height >= winH * 0.5) {
                                    if (fx.querySelectorAll('video, iframe, form').length === 0) {
                                        fx.remove();
                                    }
                                }
                            }
                        } catch(e) {}
                    }
                } catch(e) {}
            }

            // 🚫 5. Blokada klikov na zunanje oglasne povezave
            document.addEventListener('click', function(e) {
                var target = e.target;
                var a = target.closest ? target.closest('a') : null;
                if (a && a.href) {
                    var h = a.href.toLowerCase();
                    if (h.includes('doubleclick') || h.includes('googleads') || h.includes('monetag') ||
                        h.includes('onclick') || h.includes('adsterra') || h.includes('popads') ||
                        h.includes('popcash') || h.includes('hilltop') || h.includes('propu.sh') ||
                        h.includes('highperformance') || h.includes('deloplen') || h.includes('20bet') ||
                        h.includes('1xbet') || h.includes('casino') || h.includes('pussing') ||
                        h.includes('effectivegate') || h.includes('dating') || h.includes('stripchat')) {
                        e.preventDefault();
                        e.stopImmediatePropagation();
                        a.remove();
                    }
                }
            }, true);

            // Zagon čistilca
            cleanAllAdOverlays();
            setInterval(cleanAllAdOverlays, 200);

            var observer = new MutationObserver(cleanAllAdOverlays);
            if (document.body) {
                observer.observe(document.body, { childList: true, subtree: true });
            } else {
                document.addEventListener('DOMContentLoaded', function() {
                    if (document.body) observer.observe(document.body, { childList: true, subtree: true });
                });
            }
        })();
    """

    private const val YOUTUBE_FREEDOM_MOBILE_JS = """
        (function initYouTubeFreedom() {
            if (window._safeer_yt_initialized) return;
            window._safeer_yt_initialized = true;

            // 🛡️ 1. Globoki strojni klik na gumb za preskok
            function forceClick(el) {
                if (!el) return;
                var target = el.querySelector('button') || el;
                try { target.click(); } catch(e) {}
                try {
                    var evtParams = { bubbles: true, cancelable: true, view: window };
                    if (window.PointerEvent) {
                        target.dispatchEvent(new PointerEvent('pointerdown', evtParams));
                        target.dispatchEvent(new PointerEvent('pointerup', evtParams));
                    }
                    target.dispatchEvent(new MouseEvent('mousedown', evtParams));
                    target.dispatchEvent(new MouseEvent('mouseup', evtParams));
                    target.dispatchEvent(new MouseEvent('click', evtParams));
                    if (window.TouchEvent) {
                        try {
                            var touch = new Touch({
                                identifier: Date.now(),
                                target: target,
                                clientX: 0,
                                clientY: 0
                            });
                            target.dispatchEvent(new TouchEvent('touchstart', { bubbles: true, cancelable: true, touches: [touch] }));
                            target.dispatchEvent(new TouchEvent('touchend', { bubbles: true, cancelable: true, touches: [touch] }));
                        } catch(te) {}
                    }
                } catch(e) {}
            }

            // 🚫 2. Odstrani gumb "Odpri aplikacijo" in promocijske pasice
            function removeOpenAppElements() {
                try {
                    var appBtns = document.querySelectorAll(
                        'ytm-open-app-button, ytm-app-promo-renderer, ytm-mealbar-promo-renderer, ' +
                        'button[aria-label*="Odpri aplikacijo"], button[aria-label*="Open app"], ' +
                        'button[aria-label*="aplikacij"], a[aria-label*="Odpri aplikacijo"], ' +
                        'c3-icon-button[aria-label*="Odpri aplikacijo"], c3-icon-button[aria-label*="Open app"], ' +
                        'ytm-mobile-topbar-renderer ytm-open-app-button, ytm-upsell-dialog-renderer'
                    );
                    for (var b = 0; b < appBtns.length; b++) {
                        appBtns[b].remove();
                    }

                    var topActions = document.querySelectorAll('ytm-mobile-topbar-renderer .topbar-actions *');
                    for (var t = 0; t < topActions.length; t++) {
                        var el = topActions[t];
                        var txt = (el.innerText || el.textContent || '').trim().toLowerCase();
                        var aria = (el.getAttribute('aria-label') || '').toLowerCase();
                        if (txt.includes('odpri aplikacijo') || txt.includes('open app') || aria.includes('odpri aplikacijo') || aria.includes('open app') || aria.includes('aplikacij')) {
                            var target = el.closest('c3-icon-button') || el.closest('ytm-button-renderer') || el.closest('yt-button-shape') || el;
                            target.remove();
                        }
                    }
                } catch(e) {}
            }

            // 🛡️ 3. Zanesljiv Ad-Skipper & Auto-FastForward
            function processYouTubeAds() {
                removeOpenAppElements();
                try {
                    var video = document.querySelector('video');
                    var moviePlayer = document.getElementById('movie_player') ||
                                      document.querySelector('.html5-video-player');

                    // Preveri aktivne oznake oglasov
                    var isAd = document.querySelector(
                        '.ad-showing, .ad-interrupting, .ytp-ad-player-overlay, ' +
                        '.ytp-ad-text, .ytp-ad-preview-container, .ytp-ad-simple-ad-badge, ' +
                        '[class*="ad-showing"], [class*="ad-interrupting"], div.video-ads.ytp-ad-module > *, ' +
                        '.ytp-ad-duration-remaining, .ytp-ad-overlay-container, .ytm-ad-slot-renderer, ' +
                        'div[class*="ad-container"], div[class*="ad-div"], .ytp-ad-action-interstitial'
                    );

                    if (!isAd && moviePlayer && moviePlayer.classList) {
                        if (moviePlayer.classList.contains('ad-showing') || moviePlayer.classList.contains('ad-interrupting')) {
                            isAd = moviePlayer;
                        }
                    }

                    if (isAd && video) {
                        // 1. Utišaj zvok oglasa
                        try { video.muted = true; } catch(e) {}
                        // 2. Pospeši oglas na 16x hitrost
                        try { video.playbackRate = 16.0; } catch(e) {}
                        // 3. Varno preskoči na konec trajanja oglasa
                        try {
                            if (isFinite(video.duration) && video.duration > 0) {
                                video.currentTime = video.duration;
                            }
                        } catch(e) {}
                        // 4. Sproži skipAd na predvajalniku
                        if (moviePlayer && typeof moviePlayer.skipAd === 'function') {
                            try { moviePlayer.skipAd(); } catch(e) {}
                        }
                    } else if (!isAd && video) {
                        // Prava avtorska vsebina: povrni normalno hitrost in zvok
                        if (video.playbackRate > 2.0) {
                            video.playbackRate = 1.0;
                            video.muted = false;
                        }
                    }

                    // Samodejni klik na VSE gumbe za preskok
                    var skipButtons = document.querySelectorAll(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, ' +
                        '.ytp-ad-overlay-close-button, button.ytp-ad-skip-button-text, ' +
                        '.ytp-ad-skip-button-slot button, .ytp-ad-preview-container, ' +
                        'button[aria-label*="Preskoči"], button[aria-label*="Skip"], ' +
                        'button[aria-label*="skip"], .ytp-ad-skip-button-container button, ' +
                        '.ytp-ad-skip-button-container, [id^="ad-text"], [id^="skip-button"], ' +
                        'button.ytp-ad-preview-container, .ytp-ad-image-overlay button'
                    );
                    for (var i = 0; i < skipButtons.length; i++) {
                        forceClick(skipButtons[i]);
                    }

                    // Samodejni odklep zvoka
                    var unmuteBtns = document.querySelectorAll(
                        '.ytp-unmute, .ytp-mute-button, button[aria-label*="Vklopite zvok"], ' +
                        'button[aria-label*="zvok"], button[aria-label*="Unmute"], .ytp-volume-panel'
                    );
                    for (var u = 0; u < unmuteBtns.length; u++) {
                        forceClick(unmuteBtns[u]);
                    }

                    // Odstrani spremljajoče oglasne kartice in promocije
                    var badSelectors = [
                        '.badge-style-type-ad',
                        'ytd-ad-slot-renderer',
                        'ytm-ad-slot-renderer',
                        'ytd-in-feed-ad-layout-renderer',
                        'ytm-promoted-sparkles-web-renderer',
                        'ytm-promoted-video-renderer',
                        'ytd-promoted-video-renderer',
                        'ytm-companion-ad-renderer',
                        'ytm-search-pyv-renderer',
                        'ytm-mealbar-promo-renderer',
                        'ytm-app-promo-renderer',
                        'ytm-upsell-dialog-renderer',
                        '#about-this-ad',
                        '#clarify-box',
                        '#about-this-result'
                    ];
                    for (var b = 0; b < badSelectors.length; b++) {
                        var badEls = document.querySelectorAll(badSelectors[b]);
                        for (var j = 0; j < badEls.length; j++) {
                            var item = badEls[j].closest('ytm-rich-item-renderer, ytm-video-with-context-renderer, ytm-item-section-renderer, ytd-rich-item-renderer') || badEls[j];
                            try { item.remove(); } catch(e) {}
                        }
                    }
                } catch(e) {}
            }

            // Visoko-odzivni pregled vsakih 50ms
            setInterval(processYouTubeAds, 50);

            // YouTube SPA dogodki
            window.addEventListener('yt-navigate-finish', processYouTubeAds);
            window.addEventListener('yt-page-data-updated', processYouTubeAds);
            window.addEventListener('popstate', processYouTubeAds);
        })();
    """

    private const val BACKGROUND_PLAYBACK_JS = """
        /* 🎵 Safeer Browser Background Audio & Lock-Screen Playback Engine */
        (function() {
            if (window._safeer_bg_playback_installed) return;
            window._safeer_bg_playback_installed = true;

            try {
                Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                Object.defineProperty(document, 'webkitHidden', { get: function() { return false; }, configurable: true });
                Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; }, configurable: true });
                Object.defineProperty(document, 'hasFocus', { value: function() { return true; }, configurable: true });
            } catch(e) {}

            var stopEvents = ['visibilitychange', 'webkitvisibilitychange', 'pagehide', 'blur', 'focusout'];
            for (var i = 0; i < stopEvents.length; i++) {
                (function(name) {
                    window.addEventListener(name, function(e) {
                        e.stopImmediatePropagation();
                    }, true);
                    document.addEventListener(name, function(e) {
                        e.stopImmediatePropagation();
                    }, true);
                })(stopEvents[i]);
            }

            var origPause = HTMLMediaElement.prototype.pause;
            var origPlay = HTMLMediaElement.prototype.play;

            var lastUserInteractionTime = Date.now();
            var userExplicitlyPaused = false;

            var userActionEvents = ['click', 'touchstart', 'touchend', 'pointerdown', 'pointerup', 'keydown'];
            for (var u = 0; u < userActionEvents.length; u++) {
                window.addEventListener(userActionEvents[u], function() {
                    lastUserInteractionTime = Date.now();
                }, true);
            }

            HTMLMediaElement.prototype.pause = function() {
                var elapsed = Date.now() - lastUserInteractionTime;
                if (elapsed > 600) {
                    return;
                }
                userExplicitlyPaused = true;
                return origPause.apply(this, arguments);
            };

            HTMLMediaElement.prototype.play = function() {
                userExplicitlyPaused = false;
                return origPlay.apply(this, arguments);
            };

            function hookPlayerObject() {
                var player = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                if (player && !player._safeer_bg_hooked) {
                    player._safeer_bg_hooked = true;
                    var origPauseVideo = player.pauseVideo;
                    if (typeof origPauseVideo === 'function') {
                        player.pauseVideo = function() {
                            var elapsed = Date.now() - lastUserInteractionTime;
                            if (elapsed > 600) {
                                return;
                            }
                            userExplicitlyPaused = true;
                            return origPauseVideo.apply(this, arguments);
                        };
                    }
                }
            }

            if ('mediaSession' in navigator) {
                try {
                    navigator.mediaSession.playbackState = 'playing';
                    navigator.mediaSession.setActionHandler('pause', function() {
                        userExplicitlyPaused = true;
                        var v = document.querySelector('video');
                        if (v) origPause.call(v);
                    });
                    navigator.mediaSession.setActionHandler('play', function() {
                        userExplicitlyPaused = false;
                        var v = document.querySelector('video');
                        if (v) origPlay.call(v);
                    });
                } catch(e) {}
            }

            function ensureAudioPlaying() {
                hookPlayerObject();
                var vids = document.querySelectorAll('video');
                for (var v = 0; v < vids.length; v++) {
                    var vid = vids[v];
                    if (!userExplicitlyPaused && vid.paused && !vid.ended && isFinite(vid.duration) && vid.currentTime > 0) {
                        try {
                            origPlay.call(vid).catch(function(){});
                        } catch(e) {}
                    }
                }
            }

            setInterval(ensureAudioPlaying, 300);
        })();
    """

    private const val MOBILE_MEDIA_AUDIO_JS = """
        (function() {
            try {
                var vids = document.querySelectorAll('video, audio');
                for (var i = 0; i < vids.length; i++) {
                    var v = vids[i];
                    v.muted = false;
                    v.defaultMuted = false;
                    v.volume = 1.0;
                    v.setAttribute('playsinline', 'true');
                    v.setAttribute('webkit-playsinline', 'true');
                }
            } catch(e) {}
        })();
    """

    fun injectEarlyScript(webView: WebView) {
        val cosmeticCss = CosmeticFilterEngine.buildCosmeticCss()
        injectCss(webView, cosmeticCss, "safeer-cosmetic-filter")
        webView.evaluateJavascript(ANTI_POPUNDER_SHIELD_JS, null)
        webView.evaluateJavascript(BACKGROUND_PLAYBACK_JS, null)
        webView.evaluateJavascript(YOUTUBE_FREEDOM_MOBILE_JS, null)
    }

    fun injectOnPageFinished(webView: WebView, isDarkMode: Boolean) {
        val cosmeticCss = CosmeticFilterEngine.buildCosmeticCss()
        injectCss(webView, cosmeticCss, "safeer-cosmetic-filter")
        webView.evaluateJavascript(ANTI_POPUNDER_SHIELD_JS, null)
        webView.evaluateJavascript(BACKGROUND_PLAYBACK_JS, null)
        webView.evaluateJavascript(YOUTUBE_FREEDOM_MOBILE_JS, null)

        if (isDarkMode) {
            injectCss(webView, DARK_MODE_AMOLED_CSS, "safeer-dark-mode-style")
        } else {
            removeCss(webView, "safeer-dark-mode-style")
        }

        webView.evaluateJavascript(MOBILE_MEDIA_AUDIO_JS, null)
    }

    fun injectDarkModeToggle(webView: WebView, enable: Boolean) {
        if (enable) {
            injectCss(webView, DARK_MODE_AMOLED_CSS, "safeer-dark-mode-style")
        } else {
            removeCss(webView, "safeer-dark-mode-style")
        }
    }

    private fun injectCss(webView: WebView, css: String, elementId: String? = null) {
        val base64 = android.util.Base64.encodeToString(css.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val idStr = elementId ?: "custom-css"
        val js = """
            (function() {
                try {
                    var parent = document.head || document.documentElement;
                    if (!parent) return;
                    var old = document.getElementById('$idStr');
                    if (old) old.remove();
                    var style = document.createElement('style');
                    style.id = '$idStr';
                    style.type = 'text/css';
                    style.textContent = atob('$base64');
                    parent.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun removeCss(webView: WebView, elementId: String) {
        val js = """
            (function() {
                var el = document.getElementById('$elementId');
                if (el) el.remove();
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
