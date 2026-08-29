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

            // 🚫 3. Samodejno odstranjevanje lažnih opozoril, vsiljenih modalov in lažnih gumbov
            function cleanAllAdOverlays() {
                try {
                    var adSelectors = [
                        '.reward-zone', '#reward-zone', '.fc-ab-root', '.adblock-overlay', '#adblock-modal',
                        '[class*="dating-popup"]', '[id*="dating-popup"]', '[class*="fake-download"]',
                        '.download-button-ad', 'div[class*="download-arrow"]'
                    ].join(', ');
                    
                    var adElements = document.querySelectorAll(adSelectors);
                    adElements.forEach(function(el) {
                        try { el.remove(); } catch(e) {}
                    });

                    // Odstrani lažna sistemska opozorila (baterija poškodovana, virus zaznan)
                    var dialogs = document.querySelectorAll('[role="dialog"], [role="alertdialog"], .modal, .popup');
                    for (var i = 0; i < dialogs.length; i++) {
                        var d = dialogs[i];
                        var txt = (d.innerText || d.textContent || '').trim().toLowerCase();
                        if (txt.includes('battery damaged') || txt.includes('virus detected') || 
                            txt.includes('vpn recommended') || txt.includes('whatsapp za seks') ||
                            (txt.includes('disable your ad blocker') && txt.includes('disable'))) {
                            try { d.remove(); } catch(e) {}
                        }
                    }
                } catch(e) {}
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

            // 🎯 1. Odstrani oglasne bloke iz predvajalnih podatkov (Network & JSON Level Ad Stripping)
            function cleanYtData(data) {
                if (!data || typeof data !== 'object') return data;
                try {
                    if (data.adPlacements) delete data.adPlacements;
                    if (data.adSlots) delete data.adSlots;
                    if (data.playerAds) delete data.playerAds;
                    if (data.adBreakHeartbeatParams) delete data.adBreakHeartbeatParams;
                    if (data.playbackTracking) {
                        delete data.playbackTracking.videostatsPlaybackUrl;
                        delete data.playbackTracking.videostatsDelayplayUrl;
                        delete data.playbackTracking.videostatsWatchtimeUrl;
                        delete data.playbackTracking.ptrackingUrl;
                        delete data.playbackTracking.qoeUrl;
                        delete data.playbackTracking.atrUrl;
                    }
                } catch(e) {}
                return data;
            }

            // Hook window.ytInitialPlayerResponse
            try {
                var _realPlayerResp = window.ytInitialPlayerResponse;
                Object.defineProperty(window, 'ytInitialPlayerResponse', {
                    get: function() { return _realPlayerResp; },
                    set: function(val) { _realPlayerResp = cleanYtData(val); },
                    configurable: true
                });
                if (window.ytInitialPlayerResponse) {
                    window.ytInitialPlayerResponse = cleanYtData(window.ytInitialPlayerResponse);
                }
            } catch(e) {}

            // Hook window.fetch za /youtubei/v1/player in /next
            try {
                var origFetch = window.fetch;
                window.fetch = function(input, init) {
                    var url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                    if (url && (url.indexOf('/youtubei/v1/player') !== -1 || url.indexOf('/youtubei/v1/next') !== -1 || url.indexOf('/youtubei/v1/browse') !== -1)) {
                        return origFetch.apply(this, arguments).then(function(res) {
                            var cloned = res.clone();
                            return cloned.json().then(function(json) {
                                var cleaned = cleanYtData(json);
                                return new Response(JSON.stringify(cleaned), {
                                    status: res.status,
                                    statusText: res.statusText,
                                    headers: res.headers
                                });
                            }).catch(function() {
                                return res;
                            });
                        });
                    }
                    return origFetch.apply(this, arguments);
                };
            } catch(e) {}

            // Hook XMLHttpRequest
            try {
                var origOpen = XMLHttpRequest.prototype.open;
                var origSend = XMLHttpRequest.prototype.send;
                XMLHttpRequest.prototype.open = function(method, url) {
                    this._safeer_yt_url = url;
                    return origOpen.apply(this, arguments);
                };
                XMLHttpRequest.prototype.send = function() {
                    var xhr = this;
                    if (xhr._safeer_yt_url && (xhr._safeer_yt_url.indexOf('/youtubei/v1/player') !== -1 || xhr._safeer_yt_url.indexOf('/youtubei/v1/next') !== -1)) {
                        var origStateChange = xhr.onreadystatechange;
                        xhr.onreadystatechange = function() {
                            if (xhr.readyState === 4 && xhr.status === 200) {
                                try {
                                    var data = JSON.parse(xhr.responseText);
                                    var cleaned = cleanYtData(data);
                                    Object.defineProperty(xhr, 'responseText', { value: JSON.stringify(cleaned), configurable: true });
                                    Object.defineProperty(xhr, 'response', { value: JSON.stringify(cleaned), configurable: true });
                                } catch(e) {}
                            }
                            if (origStateChange) origStateChange.apply(this, arguments);
                        };
                    }
                    return origSend.apply(this, arguments);
                };
            } catch(e) {}

            // Hook JSON.parse
            try {
                var origParse = JSON.parse;
                JSON.parse = function(text, reviver) {
                    var res = origParse.apply(this, arguments);
                    if (res && typeof res === 'object') {
                        if (res.adPlacements || res.adSlots || res.playerAds) {
                            cleanYtData(res);
                        }
                    }
                    return res;
                };
            } catch(e) {}

            // 🛡️ 2. Globoki strojni klik na gumb za preskok
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
                } catch(e) {}
            }

            // 🚫 3. Odstrani gumb "Odpri aplikacijo" in promocijske pasice
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
                } catch(e) {}
            }

            // ⚡ 4. 0-sekundni hipni preskok morebitnih oglasnih sekvenc
            function processYouTubeAds() {
                removeOpenAppElements();
                try {
                    var video = document.querySelector('video');
                    var moviePlayer = document.getElementById('movie_player') ||
                                      document.querySelector('.html5-video-player');

                    // Preveri aktivno oglasno stanje predvajalnika
                    var isAd = false;
                    if (moviePlayer && moviePlayer.classList) {
                        if (moviePlayer.classList.contains('ad-showing') || moviePlayer.classList.contains('ad-interrupting')) {
                            isAd = true;
                        }
                    }

                    if (isAd && video) {
                        // Hipni preskok: utišaj, pospeši in takoj sproži skipAd
                        try { video.muted = true; } catch(e) {}
                        try { video.playbackRate = 16.0; } catch(e) {}
                        if (moviePlayer && typeof moviePlayer.skipAd === 'function') {
                            try { moviePlayer.skipAd(); } catch(e) {}
                        }
                    } else if (!isAd && video) {
                        if (video.playbackRate > 2.0) {
                            video.playbackRate = 1.0;
                            video.muted = false;
                        }
                    }

                    // Samodejni klik na gumbe za preskok
                    var skipBtn = document.querySelector(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, ' +
                        '.ytp-ad-overlay-close-button, button.ytp-ad-skip-button-text, ' +
                        '.ytp-ad-skip-button-slot button, [id^="skip-button"], ' +
                        'button[aria-label*="Preskoči"], button[aria-label*="Skip"]'
                    );
                    if (skipBtn) {
                        forceClick(skipBtn);
                    }

                    // 🔊 100% Samodejni vklop zvoka (Unmute) za YouTube
                    if (video && !isAd) {
                        if (video.muted) {
                            video.muted = false;
                        }
                        if (video.volume < 1.0) {
                            video.volume = 1.0;
                        }
                    }

                    var unmuteBtns = document.querySelectorAll(
                        '.ytp-unmute, .ytp-unmute-inner, .ytp-unmute-animated, ' +
                        'button[aria-label*="Vklopite zvok"], button[aria-label*="zvok"], ' +
                        'button[aria-label*="Unmute"], button[aria-label*="unmute"], ' +
                        '.ytp-mute-button[title*="zvok"], .ytp-mute-button[title*="Unmute"]'
                    );
                    for (var u = 0; u < unmuteBtns.length; u++) {
                        forceClick(unmuteBtns[u]);
                    }
                } catch(e) {}
            }

            setInterval(processYouTubeAds, 25);
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
