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

            // 🚫 3. Samodejno odstranjevanje lažnih opozoril, vsiljenih modalov in video oglasnih prekrivk
            function cleanAllAdOverlays() {
                try {
                    var adSelectors = [
                        '.reward-zone', '#reward-zone', '.fc-ab-root', '.adblock-overlay', '#adblock-modal',
                        '[class*="dating-popup"]', '[id*="dating-popup"]', '[class*="fake-download"]',
                        '.download-button-ad', 'div[class*="download-arrow"]',
                        '.mgp_adOverlay', '.mgp_adSkip', '.mgp_adMarker', '.mgp_commercial',
                        '.adBlockContainer', 'div[class*="adSkip"]',
                        '.mgp_skipAdButton', 'a[class*="adLink"]', 'div[class*="adInformation"]', '.adInformation',
                        'div[class*="mgp_ad"]', '.removeAds', 'a[href*="casino"]', '.topAd', '.bottomAd',
                        '.wideBanner', '.underPlayerAd', '.commercial-unit', '.ad-zone',
                        '[class*="ad-banner"]', '[class*="player-advertisement"]', '[id*="player-advertisement"]',
                        '.ad-banner-overlay', '.jw-ad-container', '.plyr__ad', '.vjs-ad', '.video-ad-overlay'
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

                    // Odstrani oglasne iframe okvirje (srcdoc ali lebdeče overlay iframe-e)
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

                    // Odstrani nevidne prekrivne plasti (Invisible Click-Jacking Overlays)
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

            // ⚡ 4. Samodejno preskakovanje video oglasov (Instant Video Ad Skipper)
            function autoSkipVideoAds() {
                try {
                    // Klikni gumb za preskok oglasa takoj ko se pojavi
                    var skipButtons = document.querySelectorAll(
                        '.videoAdUiSkipButton, .mgp_skipAdButton, .mgp_adSkip, [class*="skipAd"], ' +
                        '[class*="SkipAd"], [class*="adSkip"], [class*="ad-skip"], .video-ad-skip, ' +
                        'button[class*="skip-ad"], .skip-button, .ad-skip-button'
                    );
                    skipButtons.forEach(function(btn) {
                        if (btn && (btn.offsetWidth > 0 || btn.offsetHeight > 0)) {
                            try { btn.click(); } catch(_) {}
                        }
                    });

                    // Če teče oglasni video posnetek na spletnih straneh (NE na YouTube, kjer deluje namenski YouTube Freedom)
                    if (location.hostname.indexOf('youtube.com') === -1) {
                        var isAdActive = document.querySelector('.mgp_adPlaying, [class*="adPlaying"]');
                        if (isAdActive) {
                            var adVideos = document.querySelectorAll('.mgp_adContainer video, .ad-container video, video.ad-video');
                            adVideos.forEach(function(v) {
                                if (v && !v.paused) {
                                    if (isFinite(v.duration) && v.duration > 0) {
                                        try { v.currentTime = v.duration; } catch(_) {}
                                    }
                                    try { v.playbackRate = 16.0; } catch(_) {}
                                    try { v.muted = true; } catch(_) {}
                                }
                            });
                        }
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

            // Zagon čistilcev in samodejnega preskakovanja
            cleanAllAdOverlays();
            autoSkipVideoAds();
            setInterval(function() {
                cleanAllAdOverlays();
                autoSkipVideoAds();
            }, 200);

            var observer = new MutationObserver(function() {
                cleanAllAdOverlays();
                autoSkipVideoAds();
            });
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
        (function initYouTubeFreedomAgent() {
            if (window._safeer_yt_agent_installed) return;
            window._safeer_yt_agent_installed = true;

            // 🚫 1. Popolna blokada samodejnega predvajanja predogledov na iskalni in domači strani (Stop Feed Autoplay)
            try {
                var origPlay = HTMLMediaElement.prototype.play;
                HTMLMediaElement.prototype.play = function() {
                    var isWatch = location.pathname.indexOf('/watch') !== -1 || location.pathname.indexOf('/shorts') !== -1;
                    if (!isWatch && location.hostname.indexOf('youtube.com') !== -1) {
                        try { this.pause(); } catch(_) {}
                        return Promise.reject(new DOMException('Feed autoplay blocked by Safeer', 'AbortError'));
                    }
                    return origPlay.apply(this, arguments);
                };
            } catch(e) {}

            // 🧠 2. Safeer YouTube Stream Accelerator & Learning Agent
            var ytAgent = {
                learnedProfiles: {},
                lastVideoId: null,
                retryCount: 0,
                
                // Naloži shranjene profile predvajanja za takojšen zagon
                init: function() {
                    try {
                        var saved = localStorage.getItem('_safeer_yt_agent_profile');
                        if (saved) this.learnedProfiles = JSON.parse(saved);
                    } catch(e) {}
                    
                    this.injectPerformanceHints();
                    this.startSupervision();
                },

                // Pospeši povezovanje z Googlovimi video strežniki (Preconnect & DNS-prefetch)
                injectPerformanceHints: function() {
                    try {
                        var preconnects = ['https://googlevideo.com', 'https://i.ytimg.com', 'https://yt3.ggpht.com'];
                        preconnects.forEach(function(url) {
                            var link = document.createElement('link');
                            link.rel = 'preconnect';
                            link.href = url;
                            link.crossOrigin = 'anonymous';
                            document.head.appendChild(link);
                        });
                    } catch(e) {}
                },

                // 🛑 Zaustavi morebitne samodejne predoglede v iskanju in domači strani
                stopFeedAutoplay: function() {
                    try {
                        var isWatch = location.pathname.indexOf('/watch') !== -1 || location.pathname.indexOf('/shorts') !== -1;
                        if (!isWatch) {
                            var videos = document.querySelectorAll('video');
                            for (var i = 0; i < videos.length; i++) {
                                var v = videos[i];
                                if (!v.paused) {
                                    v.pause();
                                }
                            }
                        }
                    } catch(e) {}
                },

                // ⚡ Samodejno pospeši predvajanje in odpravi nepotrebno čakanje na /watch in /shorts
                boostPlayback: function() {
                    try {
                        // 🎯 Zaženi pospeševalnik le na dejanskih straneh z videom (/watch ali /shorts)
                        var isWatchPage = location.pathname.indexOf('/watch') !== -1 || location.pathname.indexOf('/shorts') !== -1;
                        if (!isWatchPage) {
                            this.stopFeedAutoplay();
                            return;
                        }

                        var video = document.querySelector('video');
                        var moviePlayer = document.getElementById('movie_player') ||
                                          document.querySelector('.html5-video-player');

                        // 🚀 1. Hipno sproži gumbe za predvajanje (Instant Play-Trigger)
                        var playTriggers = document.querySelectorAll(
                            '.ytp-large-play-button, .player-control-overlay, .ytp-cued-thumbnail-overlay, ' +
                            'button.ytp-play-button[aria-label*="Predvajaj"], button.ytp-play-button[aria-label*="Play"], ' +
                            '.ytp-cued-thumbnail-overlay-image'
                        );
                        for (var t = 0; t < playTriggers.length; t++) {
                            try { playTriggers[t].click(); } catch(_) {}
                        }

                        if (moviePlayer && typeof moviePlayer.playVideo === 'function') {
                            try { moviePlayer.playVideo(); } catch(_) {}
                        }

                        if (!video) return;

                        video.preload = 'auto';
                        video.setAttribute('playsinline', 'true');
                        video.setAttribute('webkit-playsinline', 'true');

                        // Poslušaj ročne pavze uporabnika, da ne vsiljujemo predvajanja, če si je uporabnik sam ustavil video
                        if (!video._safeer_pause_hooked) {
                            video._safeer_pause_hooked = true;
                            video.addEventListener('pause', function() {
                                if (!video.ended && video.readyState >= 2) {
                                    video._safeer_user_paused = true;
                                }
                            });
                            video.addEventListener('play', function() {
                                video._safeer_user_paused = false;
                            });
                        }

                        var isAd = false;
                        if (moviePlayer && moviePlayer.classList) {
                            isAd = moviePlayer.classList.contains('ad-showing') ||
                                   moviePlayer.classList.contains('ad-interrupting');
                        }

                        // 🚫 Če se pojavi oglas, ga v 0 sekundah preskoči
                        if (isAd) {
                            video.muted = true;
                            if (isFinite(video.duration) && video.duration > 0) {
                                video.currentTime = video.duration;
                            }
                            video.playbackRate = 16.0;
                            if (moviePlayer && typeof moviePlayer.skipAd === 'function') {
                                try { moviePlayer.skipAd(); } catch(_) {}
                            }
                        } else {
                            // ✅ Normalen video: povrni originalno hitrost in vklopi zvok
                            if (video.playbackRate > 2.0) {
                                video.playbackRate = 1.0;
                                video.muted = false;
                            }
                            if (video.muted) {
                                video.muted = false;
                            }
                            if (video.volume < 1.0) {
                                video.volume = 1.0;
                            }

                            // 🚀 Hipen zagon predvajanja brez odvečnega čakanja
                            if (video.paused && !video.ended && !video._safeer_user_paused) {
                                var playPromise = video.play();
                                if (playPromise !== undefined) {
                                    playPromise.catch(function() {});
                                }
                            }
                        }

                        // 🎯 Klik na gumb za preskok oglasa
                        var skipBtn = document.querySelector(
                            '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, ' +
                            '.ytp-ad-overlay-close-button, button.ytp-ad-skip-button-text, ' +
                            '.ytp-ad-skip-button-slot button, [id^="skip-button"], ' +
                            'button[aria-label*="Preskoči"], button[aria-label*="Skip"]'
                        );
                        if (skipBtn) {
                            skipBtn.click();
                        }

                        // 🔊 Odstrani YouTube mute prekrivke
                        var unmuteBtns = document.querySelectorAll(
                            '.ytp-unmute, .ytp-unmute-inner, .ytp-unmute-animated, ' +
                            'button[aria-label*="Vklopite zvok"], button[aria-label*="zvok"], ' +
                            'button[aria-label*="Unmute"], button[aria-label*="unmute"]'
                        );
                        for (var u = 0; u < unmuteBtns.length; u++) {
                            unmuteBtns[u].click();
                        }
                    } catch(e) {}
                },

                // 🛡️ Samodejno zdravljenje napak (Anti-"Prišlo je do težave" Auto-Healer)
                healErrors: function() {
                    try {
                        var isWatchPage = location.pathname.indexOf('/watch') !== -1 || location.pathname.indexOf('/shorts') !== -1;
                        if (!isWatchPage) return;

                        var errorContainer = document.querySelector('.ytp-error, .yt-playability-error-supported-renderers, ytm-player-error-message-renderer');
                        var retryBtn = document.querySelector('button[aria-label*="znova"], button[aria-label*="retry"], .ytp-error-content button, ytm-player-error-message-renderer button');

                        if (errorContainer || retryBtn) {
                            if (retryBtn) {
                                retryBtn.click();
                            } else {
                                var video = document.querySelector('video');
                                if (video && isFinite(video.currentTime) && video.currentTime > 0) {
                                    var savedTime = video.currentTime;
                                    video.load();
                                    video.currentTime = savedTime;
                                    video.play().catch(function() {});
                                }
                            }
                        }

                        // Odstrani gumb "Odpri aplikacijo", promocije aplikacije in modalna okna
                        var appPromos = document.querySelectorAll(
                            'ytm-open-app-button, ytm-app-promo-renderer, ytm-mealbar-promo-renderer, ytm-upsell-dialog-renderer, ' +
                            '.topbar-action-buttons, button[aria-label*="Odpri aplikacijo"], button[aria-label*="Open app"], ' +
                            '[aria-label*="Odpri"], [aria-label*="Open in app"]'
                        );
                        for (var p = 0; p < appPromos.length; p++) {
                            try { appPromos[p].style.display = 'none'; appPromos[p].remove(); } catch(_) {}
                        }
                        var topBtns = document.querySelectorAll('ytm-mobile-topbar-renderer button, ytm-mobile-topbar-renderer a');
                        for (var tb = 0; tb < topBtns.length; tb++) {
                            var tVal = (topBtns[tb].textContent || '').trim().toLowerCase();
                            if (tVal.indexOf('odpri') !== -1 || tVal.indexOf('open') !== -1) {
                                try { topBtns[tb].style.display = 'none'; topBtns[tb].remove(); } catch(_) {}
                            }
                        }
                    } catch(e) {}
                },

                // Stalni nadzorni cikel agenta (bliskovita 25ms zanka)
                startSupervision: function() {
                    var self = this;
                    setInterval(function() {
                        self.boostPlayback();
                        self.healErrors();
                    }, 25);

                    window.addEventListener('yt-navigate-finish', function() { self.boostPlayback(); });
                    window.addEventListener('yt-page-data-updated', function() { self.boostPlayback(); });
                    window.addEventListener('popstate', function() { self.boostPlayback(); });
                    document.addEventListener('DOMContentLoaded', function() { self.boostPlayback(); });
                }
            };

            ytAgent.init();
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
