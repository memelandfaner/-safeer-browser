package com.safeer.mobile.browser

import android.webkit.WebView

object UserScriptManager {

    private const val DARK_MODE_AMOLED_CSS = """
        /* Safeer Native Dark Mode Engine */
        :root {
            color-scheme: dark !important;
        }
    """

    private const val GPC_AND_DNT_JS = """
        /* 🔒 Safeer Global Privacy Control (GPC) & Do Not Track (DNT) W3C Engine */
        (function() {
            if (window._safeer_gpc_active) return;
            window._safeer_gpc_active = true;

            var gpcProp = {
                value: true,
                writable: false,
                configurable: false,
                enumerable: true
            };
            var dntProp = {
                value: '1',
                writable: false,
                configurable: false,
                enumerable: true
            };

            try {
                Object.defineProperty(navigator, 'globalPrivacyControl', gpcProp);
                Object.defineProperty(navigator, 'doNotTrack', dntProp);
                if (window.Navigator && window.Navigator.prototype) {
                    Object.defineProperty(window.Navigator.prototype, 'globalPrivacyControl', gpcProp);
                    Object.defineProperty(window.Navigator.prototype, 'doNotTrack', dntProp);
                }
            } catch(e) {}
        })();
    """

    private const val ANTI_POPUNDER_SHIELD_JS = """
        /* 🛡️ Safeer Anti-Popunder, Anti-Clickjacking & Streaming Shield Engine */
        (function() {
            if (window._safeer_popunder_shield_active) return;
            window._safeer_popunder_shield_active = true;

            function isPlayerElement(el) {
                if (!el || el.nodeType !== 1) return true;
                var tag = (el.tagName || '').toUpperCase();
                if (tag === 'VIDEO' || tag === 'AUDIO' || tag === 'SOURCE' || tag === 'TRACK') return true;
                if (el.querySelector && el.querySelector('video, audio')) return true;
                if (el.closest && el.closest('#player, #playerContainer, #player-container, #movie_player, .mgp_container, .mgp_player, .html5-video-player, .video-player, .jwplayer, .plyr, .vjs-tech, .video-stream, .player')) return true;
                return false;
            }

            // 🚫 2. Samodejno odstranjevanje lažnih opozoril, vsiljenih modalov in bannerjev (brez vpliva na predvajalnik)
            function cleanAllAdOverlays() {
                try {
                    var adSelectors = [
                        '.reward-zone', '#reward-zone', '.fc-ab-root', '.adblock-overlay', '#adblock-modal',
                        '.adsbygoogle', 'ins.adsbygoogle', '[id^="google_ads"]', '[id^="div-gpt-ad"]',
                        '[class*="google-ad"]', '[class*="ad-banner"]', '.interstitial-ad', '.ad-modal',
                        '.adblock-notice', '.ad-wrapper:not(#player):not(#player-container):not(.html5-video-player)',
                        '.ad-container:not(#player):not(#player-container):not(#player-container-id):not(.html5-video-player)',
                        '[class*="dating-popup"]', '[id*="dating-popup"]', '[class*="fake-download"]',
                        '.download-button-ad', 'div[class*="download-arrow"]',
                        '.removeAds', '.topAd', '.bottomAd',
                        '.wideBanner', '.underPlayerAd', '.commercial-unit', '.ad-zone',
                        '.ad-banner-overlay', '.jw-ad-container', '.plyr__ad', '.vjs-ad'
                    ].join(', ');
                    
                    var adElements = document.querySelectorAll(adSelectors);
                    for (var i = 0; i < adElements.length; i++) {
                        var el = adElements[i];
                        if (!isPlayerElement(el)) {
                            try { el.remove(); } catch(e) {}
                        }
                    }

                    // Odstrani lažna sistemska opozorila (baterija poškodovana, virus zaznan)
                    var dialogs = document.querySelectorAll('[role="dialog"], [role="alertdialog"], .modal, .popup');
                    for (var d = 0; d < dialogs.length; d++) {
                        var dlg = dialogs[d];
                        if (isPlayerElement(dlg)) continue;
                        var txt = (dlg.innerText || dlg.textContent || '').trim().toLowerCase();
                        if (txt.includes('battery damaged') || txt.includes('virus detected') || 
                            txt.includes('vpn recommended') ||
                            (txt.includes('disable your ad blocker') && txt.includes('disable'))) {
                            try { dlg.remove(); } catch(e) {}
                        }
                    }
                } catch(e) {}
            }

            // ⚡ 3. Samodejno preskakovanje video oglasov
            function autoSkipVideoAds() {
                try {
                    var skipButtons = document.querySelectorAll(
                        '.videoAdUiSkipButton, .mgp_skipAdButton, .mgp_adSkip, [class*="skipAd"], ' +
                        '[class*="SkipAd"], [class*="adSkip"], [class*="ad-skip"], .video-ad-skip, ' +
                        'button[class*="skip-ad"], .skip-button, .ad-skip-button'
                    );
                    for (var s = 0; s < skipButtons.length; s++) {
                        var btn = skipButtons[s];
                        if (btn && (btn.offsetWidth > 0 || btn.offsetHeight > 0)) {
                            try { btn.click(); } catch(_) {}
                        }
                    }
                } catch(e) {}
            }

            // 🚫 4. Blokada klikov na zunanje oglasne povezave
            document.addEventListener('click', function(e) {
                var target = e.target;
                var a = target.closest ? target.closest('a') : null;
                if (a && a.href) {
                    var h = a.href.toLowerCase();
                    if (h.includes('doubleclick') || h.includes('googleads') || h.includes('monetag') ||
                        h.includes('adsterra') || h.includes('popads') ||
                        h.includes('popcash') || h.includes('hilltop') || h.includes('propu.sh') ||
                        h.includes('highperformance') || h.includes('deloplen') ||
                        h.includes('effectivegate') || h.includes('stripchat')) {
                        e.preventDefault();
                        e.stopImmediatePropagation();
                    }
                }
            }, true);

            // 🚫 5. Zaznava in nevtralizacija nevidnih celozaslonskih clickjacking prevlek
            function neutralizeClickjackingOverlays() {
                try {
                    var allDivs = document.querySelectorAll('div, a, span');
                    for (var k = 0; k < allDivs.length; k++) {
                        var node = allDivs[k];
                        if (isPlayerElement(node)) continue;
                        var style = window.getComputedStyle(node);
                        if (style.position === 'fixed' || style.position === 'absolute') {
                            var z = parseInt(style.zIndex, 10);
                            if (z > 999) {
                                var rect = node.getBoundingClientRect();
                                var w = window.innerWidth || document.documentElement.clientWidth;
                                var h = window.innerHeight || document.documentElement.clientHeight;
                                if (rect.width >= w * 0.85 && rect.height >= h * 0.85) {
                                    var text = (node.innerText || '').trim();
                                    var isAdLike = node.tagName === 'A' || style.opacity < 0.15 || 
                                                   style.backgroundColor.indexOf('rgba(0, 0, 0, 0)') !== -1 ||
                                                   style.backgroundColor === 'transparent';
                                    if (text.length === 0 && isAdLike) {
                                        node.remove();
                                    }
                                }
                            }
                        }
                    }
                } catch(e) {}
            }

            var hostNow = (location.hostname || '').toLowerCase();
            var isYouTubePage = hostNow.indexOf('youtube') !== -1 || hostNow.indexOf('youtu.be') !== -1;

            cleanAllAdOverlays();
            if (!isYouTubePage) {
                autoSkipVideoAds();
                neutralizeClickjackingOverlays();
            }
            setInterval(function() {
                cleanAllAdOverlays();
                if (!isYouTubePage) {
                    autoSkipVideoAds();
                    neutralizeClickjackingOverlays();
                }
            }, isYouTubePage ? 2500 : 500);

            if (!isYouTubePage) {
                var observer = new MutationObserver(function() {
                    cleanAllAdOverlays();
                    autoSkipVideoAds();
                    neutralizeClickjackingOverlays();
                });
                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true });
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        if (document.body) observer.observe(document.body, { childList: true, subtree: true });
                    });
                }
            }
        })();
    """

    private const val YOUTUBE_FREEDOM_MOBILE_JS = """
        (function initYouTubeFreedomAgent() {
            if (window._safeer_yt_agent_installed) return;
            window._safeer_yt_agent_installed = true;

            function isYtHost() {
                var h = (location.hostname || '').toLowerCase();
                return h.indexOf('youtube.com') !== -1 || h.indexOf('youtu.be') !== -1 || h.indexOf('youtube-nocookie.com') !== -1;
            }

            function isAdNode(item) {
                if (!item || typeof item !== 'object') return false;
                return !!(item.adSlotRenderer || item.promotedVideoRenderer || item.inFeedAdLayoutRenderer ||
                    item.promotedSparklesWebRenderer || item.promotedSparklesTextRenderer ||
                    item.promotedSparklesRenderer || item.displayAdRenderer || item.mastheadAdRenderer ||
                    item.houseAdRenderer || item.adVideoEndRenderer || item.promotedItemRenderer ||
                    item.bannerPromoRenderer || item.adInfoRenderer || item.instreamVideoAdRenderer ||
                    item.playerLegacyDesktopWatchAdsRenderer || item.adPlacementRenderer ||
                    item.reelPlayerAdRenderer);
            }

            function stripAds(obj, depth) {
                if (!obj || typeof obj !== 'object' || depth > 36) return obj;
                if (Array.isArray(obj)) {
                    for (var i = obj.length - 1; i >= 0; i--) {
                        var item = obj[i];
                        if (item && typeof item === 'object' && isAdNode(item)) {
                            obj.splice(i, 1);
                            continue;
                        }
                        stripAds(item, depth + 1);
                    }
                    return obj;
                }
                if (Array.isArray(obj.adPlacements)) obj.adPlacements = [];
                if (Array.isArray(obj.adSlots)) obj.adSlots = [];
                if (Array.isArray(obj.playerAds)) obj.playerAds = [];
                if (Array.isArray(obj.adBreaks)) obj.adBreaks = [];
                if (Array.isArray(obj.ads)) obj.ads = [];
                try { delete obj.adBreakHeartbeatParams; } catch (e1) {}
                try { delete obj.playerAdvertisement; } catch (e2) {}
                var keys = Object.keys(obj);
                for (var k = 0; k < keys.length; k++) {
                    var key = keys[k];
                    var v = obj[key];
                    if (!v || typeof v !== 'object') continue;
                    stripAds(v, depth + 1);
                }
                return obj;
            }

            function looksLikeYt(obj) {
                return !!(obj && (obj.adPlacements || obj.adSlots || obj.playerAds || obj.videoDetails ||
                    obj.contents || obj.responseContext || obj.streamingData || obj.playabilityStatus ||
                    obj.onResponseReceivedEndpoints || obj.adBreakHeartbeatParams));
            }

            function stripGlobals() {
                try {
                    if (window.ytInitialPlayerResponse) stripAds(window.ytInitialPlayerResponse, 0);
                    if (window.ytInitialData) stripAds(window.ytInitialData, 0);
                    if (window.ytplayer && window.ytplayer.config && window.ytplayer.config.args) {
                        var args = window.ytplayer.config.args;
                        if (args.raw_player_response && typeof args.raw_player_response === 'object') {
                            stripAds(args.raw_player_response, 0);
                        }
                        if (typeof args.player_response === 'string' && args.player_response.charAt(0) === '{') {
                            try {
                                var pr = JSON.parse(args.player_response);
                                stripAds(pr, 0);
                                args.player_response = JSON.stringify(pr);
                            } catch (e3) {}
                        }
                    }
                } catch (e4) {}
            }

            function hookJsonParse() {
                if (window._safeer_yt_json_hooked) return;
                window._safeer_yt_json_hooked = true;
                try {
                    var origParse = JSON.parse;
                    JSON.parse = function(text) {
                        var data = origParse.apply(this, arguments);
                        try {
                            if (data && typeof data === 'object' && looksLikeYt(data)) stripAds(data, 0);
                        } catch (e5) {}
                        return data;
                    };
                } catch (e6) {}
            }

            function hookFetchAndXhr() {
                if (window._safeer_yt_net_hooked) return;
                window._safeer_yt_net_hooked = true;
                try {
                    var origFetch = window.fetch;
                    if (typeof origFetch === 'function') {
                        window.fetch = function() {
                            return origFetch.apply(this, arguments).then(function(resp) {
                                try {
                                    var u = (resp && resp.url) ? (resp.url + '') : '';
                                    if (u.indexOf('/youtubei/') === -1) return resp;
                                    return resp.clone().json().then(function(data) {
                                        if (data && typeof data === 'object') stripAds(data, 0);
                                        return new Response(JSON.stringify(data), {
                                            status: resp.status,
                                            statusText: resp.statusText,
                                            headers: resp.headers
                                        });
                                    }).catch(function() { return resp; });
                                } catch (e7) {
                                    return resp;
                                }
                            });
                        };
                    }
                } catch (e8) {}
                try {
                    var origOpen = XMLHttpRequest.prototype.open;
                    var origSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.open = function(method, url) {
                        this._safeer_yt_url = (url || '') + '';
                        return origOpen.apply(this, arguments);
                    };
                    XMLHttpRequest.prototype.send = function() {
                        var xhr = this;
                        var target = (xhr._safeer_yt_url || '') + '';
                        if (target.indexOf('/youtubei/') !== -1) {
                            xhr.addEventListener('readystatechange', function rewriteYouTubeiResponse() {
                                if (xhr.readyState !== 4 || xhr._safeer_yt_rewritten) return;
                                try {
                                    var raw = xhr.responseText;
                                    if (!raw || raw.charAt(0) !== '{') return;
                                    var data = JSON.parse(raw);
                                    if (!data || typeof data !== 'object') return;
                                    stripAds(data, 0);
                                    var out = JSON.stringify(data);
                                    try { Object.defineProperty(xhr, 'responseText', { configurable: true, get: function() { return out; } }); } catch (e9a) {}
                                    try {
                                        Object.defineProperty(xhr, 'response', {
                                            configurable: true,
                                            get: function() { return xhr.responseType === 'json' ? data : out; }
                                        });
                                    } catch (e9b) {}
                                    xhr._safeer_yt_rewritten = true;
                                } catch (e9) {}
                            }, true);
                        }
                        return origSend.apply(this, arguments);
                    };
                } catch (e10) {}
            }

            function hookYtInitial() {
                if (window._safeer_yt_ipr_hooked) return;
                window._safeer_yt_ipr_hooked = true;
                function hookName(name) {
                    try {
                        var held = window[name];
                        if (held && typeof held === 'object') stripAds(held, 0);
                        Object.defineProperty(window, name, {
                            configurable: true,
                            enumerable: true,
                            get: function() { return held; },
                            set: function(v) {
                                held = (v && typeof v === 'object') ? stripAds(v, 0) : v;
                            }
                        });
                    } catch (e11) {}
                }
                hookName('ytInitialPlayerResponse');
                hookName('ytInitialData');
            }

            function compactText(el) {
                if (!el) return '';
                var t = ((el.getAttribute && (el.getAttribute('aria-label') || el.getAttribute('title'))) || el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim();
                if (t.length > 80) t = t.substring(0, 80);
                return t.toLowerCase();
            }

            function isBlockedSkipTarget(t) {
                if (!t) return true;
                if (t.indexOf('začnite') !== -1 || t.indexOf('zacnite') !== -1) return true;
                if (t === 'start' || t.indexOf('predvajaj') !== -1 || t.indexOf('play video') !== -1) return true;
                if (t.indexOf('preskočite čez') !== -1 || t.indexOf('skip in') !== -1 || t.indexOf('skip after') !== -1) return true;
                return false;
            }

            function isSkipLabel(t) {
                if (!t || isBlockedSkipTarget(t)) return false;
                return t === 'preskoči' || t === 'preskoci' || t === 'skip ad' || t === 'skip ads' ||
                    t.indexOf('preskoči oglas') !== -1 || t.indexOf('skip ad') !== -1 ||
                    (t === 'skip' || t === 'preskočite');
            }

            function isVisible(el) {
                if (!el) return false;
                try {
                    var r = el.getBoundingClientRect();
                    return r.width > 0 && r.height > 0;
                } catch (e12) { return false; }
            }

            function getMoviePlayer() {
                return document.getElementById('movie_player') || document.querySelector('.html5-video-player');
            }

            function playerHasAd() {
                var player = document.querySelector('#movie_player.ad-showing, #movie_player.ad-interrupting, ' +
                    '.html5-video-player.ad-showing, .html5-video-player.ad-interrupting') || getMoviePlayer();
                if (!player) return false;
                if (player.classList && (player.classList.contains('ad-showing') || player.classList.contains('ad-interrupting'))) {
                    return true;
                }
                var skipControls = player.querySelectorAll(
                    '.ytp-skip-ad-button, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, ' +
                    '.ytp-ad-skip-button-slot button, button[aria-label*="Preskoči"], button[aria-label*="Skip ad"]'
                );
                for (var i = 0; i < skipControls.length; i++) {
                    if (isVisible(skipControls[i]) && !isBlockedSkipTarget(compactText(skipControls[i]))) return true;
                }
                return false;
            }

            function clickSkipButtons() {
                var now = Date.now();
                if (now - (window._safeer_yt_last_skip || 0) < 350) return;
                var player = getMoviePlayer();
                var roots = player ? [player, document] : [document];
                var skipSelectors = [
                    '.ytp-skip-ad-button', '.ytp-ad-skip-button', '.ytp-ad-skip-button-modern',
                    '.ytp-ad-skip-button-slot button', 'button.ytp-ad-skip-button-text',
                    '.ytp-skip-ad-button__text', 'button[aria-label*="Preskoči"]',
                    'button[aria-label*="Skip ad"]', 'button[aria-label*="Skip ads"]'
                ].join(', ');
                for (var r = 0; r < roots.length; r++) {
                    var direct = roots[r].querySelectorAll(skipSelectors);
                    for (var d = 0; d < direct.length; d++) {
                        if (isVisible(direct[d]) && !isBlockedSkipTarget(compactText(direct[d]))) {
                            try { direct[d].click(); window._safeer_yt_last_skip = now; return; } catch (e13) {}
                        }
                    }
                }
                var scope = player || document;
                var nodes = scope.querySelectorAll('button, [role="button"]');
                for (var i = 0; i < nodes.length; i++) {
                    var t = compactText(nodes[i]);
                    if (isSkipLabel(t) && isVisible(nodes[i])) {
                        try { nodes[i].click(); window._safeer_yt_last_skip = now; return; } catch (e14) {}
                    }
                }
            }

            window._safeer_playerHasAd = playerHasAd;
            window._safeer_clickSkipButtons = clickSkipButtons;

            function stabilizeWatchPage() {
                try {
                    if (!isYtHost() || window._safeer_yt_bg_set) return;
                    window._safeer_yt_bg_set = true;
                    var root = document.documentElement;
                    if (root) {
                        root.style.backgroundColor = '#0f0f0f';
                        try { root.style.colorScheme = 'dark'; } catch (eStab) {}
                    }
                    if (document.body) document.body.style.backgroundColor = '#0f0f0f';
                } catch (eStab2) {}
            }

            function isWatchPath() {
                var p = location.pathname || '';
                return p.indexOf('/watch') !== -1 || p.indexOf('/shorts') !== -1;
            }

            function isMixHref(href) {
                if (!href) return false;
                href = href + '';
                return href.indexOf('list=RD') !== -1 || href.indexOf('start_radio=1') !== -1 || /[?&]list=RD/.test(href);
            }

            function mixCardSelector() {
                return 'ytm-rich-item-renderer, ytm-video-with-context-renderer, ytm-compact-video-renderer, ytm-media-item, ytm-video-card-renderer';
            }

            function mixHrefFromNode(node) {
                if (!node || !node.querySelector) return '';
                var a = node.querySelector('a[href*="list=RD"], a[href*="start_radio=1"]');
                return a ? (a.href || '') : '';
            }

            function pauseVideosIn(el) {
                if (!el || !el.querySelectorAll) return;
                var vids = el.querySelectorAll('video');
                for (var j = 0; j < vids.length; j++) {
                    try { vids[j].pause(); } catch (ePv) {}
                }
            }

            function isStickyLike(el) {
                if (!el) return false;
                try {
                    if (el.classList && (el.classList.contains('sticky-player') ||
                        el.classList.contains('miniplayer') || el.classList.contains('mini-player'))) return true;
                    var tag = (el.tagName || '').toLowerCase();
                    if (tag === 'ytm-miniplayer' || tag === 'ytm-miniplayer-bar-renderer') return true;
                    var cs = window.getComputedStyle(el);
                    return !!(cs && cs.position === 'fixed');
                } catch (eS) { return false; }
            }

            function hideStickyHomePlayer() {
                try {
                    if (isWatchPath()) return;
                    var scrolled = (window.scrollY || document.documentElement.scrollTop || 0) > 140;
                    if (!scrolled) return;
                    var candidates = document.querySelectorAll('ytm-miniplayer, ytm-miniplayer-bar-renderer');
                    for (var p = 0; p < candidates.length; p++) {
                        try {
                            var el = candidates[p];
                            pauseVideosIn(el);
                            el.style.setProperty('display', 'none', 'important');
                            el.style.setProperty('pointer-events', 'none', 'important');
                        } catch (eP) {}
                    }
                } catch (eHs) {}
            }

            function ensureWatchPlayerVisible() {
                try {
                    if (!isWatchPath()) return;
                    var pc = document.querySelector('#player-container-id') || document.querySelector('.player-container');
                    if (pc) {
                        if (pc.style.display === 'none') pc.style.removeProperty('display');
                        pc.style.setProperty('display', 'block', 'important');
                        pc.style.setProperty('visibility', 'visible', 'important');
                        pc.style.setProperty('opacity', '1', 'important');
                    }
                    var p = document.querySelector('#player') || document.querySelector('#movie_player');
                    if (p) {
                        if (p.style.display === 'none') p.style.removeProperty('display');
                        p.style.setProperty('display', 'block', 'important');
                        p.style.setProperty('visibility', 'visible', 'important');
                        p.style.setProperty('opacity', '1', 'important');
                        if (!document.fullscreenElement && p.classList.contains('ytp-fullscreen')) {
                            p.classList.remove('ytp-fullscreen');
                        }
                    }
                    var v = document.querySelector('video.video-stream') || document.querySelector('video');
                    if (v) {
                        v.style.setProperty('visibility', 'visible', 'important');
                        v.style.setProperty('opacity', '1', 'important');
                        if (!document.fullscreenElement && v.style.top && parseInt(v.style.top) > 50) {
                            v.style.top = '0px';
                        }
                    }
                } catch (eWp) {}
            }

            function tameHomeMiniplayer() {
                try {
                    if (isWatchPath()) return;
                    var mini = document.querySelectorAll(
                        'ytm-miniplayer, ytm-miniplayer-bar-renderer, ytm-miniplayer-controls-renderer'
                    );
                    for (var m = 0; m < mini.length; m++) {
                        try {
                            if (mini[m].getAttribute('data-safeer-hidden') === '1') continue;
                            pauseVideosIn(mini[m]);
                            mini[m].style.setProperty('display', 'none', 'important');
                            mini[m].style.setProperty('pointer-events', 'none', 'important');
                            mini[m].setAttribute('data-safeer-hidden', '1');
                        } catch (eN) {}
                    }
                } catch (eO) {}
            }

            function bindHomeScrollTame() {
                if (window._safeer_yt_scroll_tame) return;
                window._safeer_yt_scroll_tame = true;
                var scheduled = false;
                window.addEventListener('scroll', function() {
                    if (isWatchPath() || scheduled) return;
                    scheduled = true;
                    requestAnimationFrame(function() {
                        scheduled = false;
                        tameHomeMiniplayer();
                        hideStickyHomePlayer();
                    });
                }, { passive: true });
            }

            function tameMixPlaylistOverlay() {
                try {
                    if (!isWatchPath()) {
                        window._safeer_yt_mix_panel_tamed = false;
                        return;
                    }
                    if (window._safeer_yt_mix_panel_tamed) return;
                    var panels = document.querySelectorAll(
                        'ytm-playlist-panel-renderer, ytm-engagement-panel-section-list-renderer[target-id*="playlist"]'
                    );
                    if (!panels.length) return;
                    for (var i = 0; i < panels.length; i++) {
                        try {
                            panels[i].style.setProperty('position', 'relative', 'important');
                            panels[i].style.setProperty('max-height', '42vh', 'important');
                            panels[i].style.setProperty('overflow-y', 'auto', 'important');
                            panels[i].style.setProperty('top', 'auto', 'important');
                            panels[i].style.setProperty('bottom', 'auto', 'important');
                        } catch (ePl) {}
                    }
                    window._safeer_yt_mix_panel_tamed = true;
                } catch (ePo) {}
            }

            function hideWatchPlayerOnHome() {
                try {
                    if (isWatchPath()) return;
                    var pc = document.querySelector('#player-container-id') || document.querySelector('.player-container');
                    if (pc) {
                        pauseVideosIn(pc);
                        pc.style.setProperty('display', 'none', 'important');
                    }
                    var p = document.querySelector('#player') || document.querySelector('#movie_player');
                    if (p) {
                        pauseVideosIn(p);
                        p.style.setProperty('display', 'none', 'important');
                    }
                    var mini = document.querySelectorAll('ytm-miniplayer, ytm-miniplayer-bar-renderer');
                    for (var m = 0; m < mini.length; m++) {
                        pauseVideosIn(mini[m]);
                        mini[m].style.setProperty('display', 'none', 'important');
                    }
                } catch (eHw) {}
            }

            function bindHomeMixPlayBlock() {
                if (window._safeer_yt_mix_play_block) return;
                window._safeer_yt_mix_play_block = true;
                document.addEventListener('play', function(e) {
                    try {
                        if (isWatchPath()) return;
                        var media = e.target;
                        if (!media || media.tagName !== 'VIDEO') return;
                        media.pause();
                        var host = media.closest && media.closest(
                            'ytm-miniplayer, ytm-miniplayer-bar-renderer, #player-container-id, .player-container, .sticky-player'
                        );
                        if (host) {
                            host.style.setProperty('display', 'none', 'important');
                        }
                    } catch (ePb) {}
                }, true);
            }

            function bindMixClicks() {
                if (window._safeer_yt_mix_clicks) return;
                window._safeer_yt_mix_clicks = true;
                document.addEventListener('click', function(e) {
                    try {
                        if (isWatchPath()) return;
                        var t = e.target;
                        if (!t || !t.closest) return;
                        if (t.closest('ytm-menu-renderer, button[aria-haspopup], [aria-label*="Več dejanj"], [aria-label*="More actions"], [aria-label*="Action menu"]')) return;
                        var a = t.closest('a[href]');
                        if (a && isMixHref(a.href || '')) {
                            e.preventDefault();
                            location.href = a.href;
                            return;
                        }
                        var card = t.closest(mixCardSelector());
                        if (!card) return;
                        var href = mixHrefFromNode(card);
                        if (!isMixHref(href)) return;
                        var link = card.querySelector('a[href*="list=RD"], a[href*="start_radio=1"], a[href*="/watch"]');
                        if (link && link.href) {
                            e.preventDefault();
                            location.href = link.href;
                        }
                    } catch (eC) {}
                }, true);
            }

            if (isYtHost()) {
                stabilizeWatchPage();
                hookJsonParse();
                hookFetchAndXhr();
                hookYtInitial();
                stripGlobals();
                tameHomeMiniplayer();
                bindHomeScrollTame();
                bindHomeMixPlayBlock();
                bindMixClicks();
            }

            // 🧠 Safeer YouTube Instant Song Accelerator & Track Transition Agent
            var ytAgent = {
                lastHref: location.href,
                lastTriggerTime: 0,
                initialPlayDone: false,
                lastSkipClick: 0,
                
                init: function() {
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

                // ⚡ Bliskovito pospeši predvajanje nove skladbe brez zakasnitev
                boostPlayback: function() {
                    try {
                        var isWatchPage = location.pathname.indexOf('/watch') !== -1 || location.pathname.indexOf('/shorts') !== -1;
                        if (!isWatchPage) return;

                        // 🔄 Zaznaj zamenjavo pesmi (New Song Transition) in hipno ponastavi stanje
                        if (location.href !== this.lastHref) {
                            this.lastHref = location.href;
                            this.initialPlayDone = false;
                            var v = document.querySelector('video');
                            if (v) {
                                v._safeer_user_paused = false;
                                v.preload = 'auto';
                            }
                        }

                        var video = document.querySelector('video');
                        var moviePlayer = document.getElementById('movie_player') ||
                                          document.querySelector('.html5-video-player');

                        var now = Date.now();

                        // 🚀 Enkraten zagon predvajanja ob začetku nove skladbe (brez motenja predvajalnika)
                        if ((!video || (video.paused && !video._safeer_user_paused)) && !this.initialPlayDone) {
                            if (now - this.lastTriggerTime > 600) {
                                this.lastTriggerTime = now;
                                if (video) {
                                    try { video.play().catch(function() {}); } catch(_) {}
                                }
                                if (moviePlayer && typeof moviePlayer.playVideo === 'function') {
                                    try { moviePlayer.playVideo(); } catch(_) {}
                                }
                                if (moviePlayer && moviePlayer.classList &&
                                    (moviePlayer.classList.contains('unstarted-mode') ||
                                     moviePlayer.classList.contains('ytp-player-preparing'))) {
                                    try { moviePlayer.click(); } catch(_) {}
                                }
                                var playTriggers = document.querySelectorAll(
                                    '.ytp-large-play-button, .ytp-cued-thumbnail-overlay, .ytp-cued-thumbnail-overlay-image, ' +
                                    'button.ytp-play-button[aria-label*="Predvajaj"], button.ytp-play-button[aria-label*="Play"], ' +
                                    'button[aria-label="Predvajaj"], button[aria-label="Play"]'
                                );
                                for (var t = 0; t < playTriggers.length; t++) {
                                    try {
                                        playTriggers[t].dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
                                        playTriggers[t].click();
                                    } catch(_) {}
                                }
                            }
                        }

                        if (video && !video.paused && video.currentTime > 0.5) {
                            this.initialPlayDone = true;
                        }

                        if (!video) return;

                        video.preload = 'auto';
                        video.setAttribute('playsinline', 'true');
                        video.setAttribute('webkit-playsinline', 'true');

                        if (!video._safeer_pause_hooked) {
                            video._safeer_pause_hooked = true;
                            video.addEventListener('pause', function() {
                                if (ytAgent.initialPlayDone && !video.ended && location.href === ytAgent.lastHref) {
                                    video._safeer_user_paused = true;
                                }
                            });
                            video.addEventListener('play', function() {
                                video._safeer_user_paused = false;
                            });
                        }

                        var isAd = playerHasAd();
                        // 🚫 Samo potrjen preroll: klikni gumb za preskok, originalni video ostane pri 1x
                        if (isAd) {
                            video.muted = true;
                            clickSkipButtons();
                        } else {
                            if (video.playbackRate > 2.0) {
                                video.playbackRate = 1.0;
                            }
                            if (video.paused && !video.ended && !video._safeer_user_paused) {
                                var playPromise = video.play();
                                if (playPromise !== undefined) {
                                    playPromise.catch(function() {});
                                }
                            }
                        }

                        // Enkraten vklop zvoka ob začetku novega videa, če je bil utišan zaradi autoplay pravil
                        if (!this.initialPlayDone && video && video.muted) {
                            var unmuteBtn = document.querySelector(
                                '.ytp-unmute, button[aria-label*="Vklopite zvok"], button[aria-label*="Unmute"]'
                            );
                            if (unmuteBtn) {
                                try { unmuteBtn.click(); } catch(_) {}
                            }
                        }

                    } catch(e) {}
                },

                // 🛡️ Samodejno zdravljenje napak
                healErrors: function() {
                    try {
                        var isWatchPage = location.pathname.indexOf('/watch') !== -1 || location.pathname.indexOf('/shorts') !== -1;
                        if (!isWatchPage) return;

                        var errorContainer = document.querySelector('.ytp-error, .yt-playability-error-supported-renderers, ytm-player-error-message-renderer');
                        var retryBtn = document.querySelector('button[aria-label*="znova"], button[aria-label*="retry"], .ytp-error-content button, ytm-player-error-message-renderer button');

                        if (errorContainer || retryBtn) {
                            if (retryBtn && isVisible(retryBtn)) {
                                retryBtn.click();
                            }
                        }

                        // Odstrani gumb "Odpri aplikacijo", promocije aplikacije in modalna okna
                        var appPromos = document.querySelectorAll(
                            'ytm-open-app-button, ytm-app-promo-renderer, ytm-mealbar-promo-renderer, ytm-upsell-dialog-renderer, ' +
                            'button[aria-label*="Odpri aplikacijo"], button[aria-label*="Odpri v aplikaciji"], ' +
                            'button[aria-label*="Open app"], button[aria-label*="Open in app"]'
                        );
                        for (var p = 0; p < appPromos.length; p++) {
                            try { appPromos[p].style.display = 'none'; appPromos[p].remove(); } catch(_) {}
                        }

                        // Mix / seznam pusti pri miru — zapiranje povzroči napako in lepljiv overlay.
                        var isMix = (location.search || '').indexOf('list=') !== -1 ||
                                    (location.search || '').indexOf('start_radio=') !== -1;
                        if (!isMix) {
                            var strayOverlays = document.querySelectorAll(
                                'ytm-mealbar-promo-renderer, ytm-upsell-dialog-renderer'
                            );
                            for (var so = 0; so < strayOverlays.length; so++) {
                                try { strayOverlays[so].style.display = 'none'; } catch(_) {}
                            }
                        }
                    } catch(e) {}
                },

                // Stalni nadzorni cikel agenta
                startSupervision: function() {
                    var self = this;
                    var ticks = 0;
                    setInterval(function() {
                        ticks++;
                        if (isWatchPath()) {
                            ensureWatchPlayerVisible();
                            self.boostPlayback();
                            if (ticks % 2 === 0) self.healErrors();
                            if (ticks % 4 === 0) tameMixPlaylistOverlay();
                        } else {
                            hideWatchPlayerOnHome();
                            if (ticks % 3 === 0) tameHomeMiniplayer();
                        }
                    }, 400);

                    window.addEventListener('yt-navigate-finish', function() {
                        window._safeer_yt_mix_panel_tamed = false;
                        if (isWatchPath()) {
                            ensureWatchPlayerVisible();
                            self.boostPlayback();
                            tameMixPlaylistOverlay();
                        } else {
                            hideWatchPlayerOnHome();
                        }
                        tameHomeMiniplayer();
                    });
                    window.addEventListener('yt-page-data-updated', function() {
                        if (isWatchPath()) {
                            ensureWatchPlayerVisible();
                            self.boostPlayback();
                        } else {
                            hideWatchPlayerOnHome();
                        }
                    });
                    window.addEventListener('popstate', function() {
                        window._safeer_yt_mix_panel_tamed = false;
                        if (isWatchPath()) {
                            ensureWatchPlayerVisible();
                            self.boostPlayback();
                        } else {
                            hideWatchPlayerOnHome();
                        }
                        tameHomeMiniplayer();
                    });
                    document.addEventListener('DOMContentLoaded', function() { self.boostPlayback(); });
                }
            };

            ytAgent.init();
        })();
    """

    const val YOUTUBE_MIX_BACK_HOME_JS = """
        (function() {
            try {
                var vids = document.querySelectorAll('video');
                for (var i = 0; i < vids.length; i++) {
                    try { vids[i].pause(); } catch (e0) {}
                }
                var mini = document.querySelectorAll('ytm-miniplayer, ytm-miniplayer-bar-renderer');
                for (var m = 0; m < mini.length; m++) {
                    try { mini[m].style.setProperty('display', 'none', 'important'); } catch (e1) {}
                }
                var logo = document.querySelector(
                    'ytm-home-logo-renderer a, ytm-mobile-topbar-renderer a[href="/"], ' +
                    'a.mobile-topbar-logo, ytm-logo a, a[aria-label="YouTube"]'
                );
                if (logo) { logo.click(); return; }
                var pivot = document.querySelector(
                    'ytm-pivot-bar-item-renderer[tab-identifier="FEwhat_to_watch"], ' +
                    '[aria-label="Domača stran"], [aria-label="Home"]'
                );
                if (pivot) { pivot.click(); return; }
                location.replace('https://m.youtube.com/');
            } catch (e) {
                try { location.replace('https://m.youtube.com/'); } catch (e2) {}
            }
        })();
    """

    private const val WINDOWS_CHROME_ENVIRONMENT_JS = """
        /* 💻 100% Google Chrome on Windows 10/11 Desktop Environment Engine */
        (function() {
            if (window._safeer_win_chrome_env_active) return;
            window._safeer_win_chrome_env_active = true;

            try {
                Object.defineProperty(navigator, 'platform', { get: function() { return 'Win32'; }, configurable: true });
                Object.defineProperty(navigator, 'vendor', { get: function() { return 'Google Inc.'; }, configurable: true });
                Object.defineProperty(navigator, 'appVersion', { get: function() { return '5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36'; }, configurable: true });
                if (navigator.userAgentData) {
                    Object.defineProperty(navigator, 'userAgentData', {
                        get: function() {
                            return {
                                brands: [
                                    { brand: 'Chromium', version: '133' },
                                    { brand: 'Google Chrome', version: '133' },
                                    { brand: 'Not(A:Brand', version: '99' }
                                ],
                                mobile: false,
                                platform: 'Windows',
                                getHighEntropyValues: function() {
                                    return Promise.resolve({
                                        architecture: 'x86',
                                        bitness: '64',
                                        model: '',
                                        platform: 'Windows',
                                        platformVersion: '15.0.0',
                                        uaFullVersion: '133.0.6943.98'
                                    });
                                }
                            };
                        },
                        configurable: true
                    });
                }
            } catch(e) {}
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
            } catch(e) {}

            var stopEvents = ['visibilitychange', 'webkitvisibilitychange'];
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
        })();
    """

    private const val STREAMING_INSTANT_START_JS = """
        /* 🚀 Safeer Desktop Streaming Engine: Normal Native Playback & Preroll Ad Elimination */
        (function() {
            var hostNow = (location.hostname || '').toLowerCase();
            if (hostNow.indexOf('youtube.com') !== -1 || hostNow.indexOf('youtu.be') !== -1) return;
            if (window._safeer_instant_streaming_active) return;
            window._safeer_instant_streaming_active = true;

            function sanitizeFlashvars(fv) {
                if (!fv || typeof fv !== 'object') return;
                try {
                    fv.adRollGlobalConfig = [];
                    fv.pauseroll_url = "";
                    fv.postroll_url = "";
                    fv.hidePostPauseRoll = true;
                    fv.tubesCmsPrerollConfigType = "none";
                    fv.autoplay = "true";
                } catch(_) {}
            }

            for (var k in window) {
                if (k.startsWith('flashvars_')) {
                    sanitizeFlashvars(window[k]);
                }
            }

            function setupVideoElement(v) {
                if (!v) return;

                function clearBufferingOverlay() {
                    try {
                        var c = v.closest('.mgp_container') || document.querySelector('.mgp_container');
                        if (c) {
                            c.classList.remove('mgp_bufferingState', 'mgp_adRollReady', 'mgp_prerollState');
                            c.classList.add('mgp_playingState');
                        }
                    } catch(_) {}
                }

                if (!v._safeer_events_bound) {
                    v._safeer_events_bound = true;
                    v.addEventListener('playing', clearBufferingOverlay);
                    v.addEventListener('play', clearBufferingOverlay);
                    v.addEventListener('timeupdate', function() {
                        if (v.currentTime > 0) clearBufferingOverlay();
                    });
                    v.addEventListener('loadeddata', clearBufferingOverlay);
                    v.addEventListener('canplay', clearBufferingOverlay);
                }

                if ((!v.src && !v.currentSrc) || v.readyState === 0) {
                    var fvKey = Object.keys(window).find(function(key) { return key.startsWith('flashvars_'); });
                    if (fvKey && window[fvKey] && window[fvKey].mediaDefinitions) {
                        var defs = window[fvKey].mediaDefinitions;
                        var media = defs.find(function(m) { return m.defaultQuality || m.quality === '720' || m.format === 'hls'; }) || defs[0];
                        if (media && media.videoUrl && !v._safeer_hls_attached) {
                            v._safeer_hls_attached = true;
                            if (window.Hls && window.Hls.isSupported && window.Hls.isSupported()) {
                                var hls = new window.Hls();
                                hls.loadSource(media.videoUrl);
                                hls.attachMedia(v);
                                hls.on(window.Hls.Events.MANIFEST_PARSED, function() {
                                    var p = v.play();
                                    if (p !== undefined) p.catch(function() {});
                                    clearBufferingOverlay();
                                });
                            } else {
                                v.src = media.videoUrl;
                                var p = v.play();
                                if (p !== undefined) p.catch(function() {});
                                clearBufferingOverlay();
                            }
                        }
                    }
                }

                if (!v.paused && v.currentTime > 0) {
                    clearBufferingOverlay();
                }
            }

            function scanVideos() {
                var vids = document.querySelectorAll('video');
                for (var i = 0; i < vids.length; i++) {
                    setupVideoElement(vids[i]);
                }
            }

            scanVideos();
            document.addEventListener('DOMContentLoaded', scanVideos);
            window.addEventListener('load', scanVideos);
            var obs = new MutationObserver(scanVideos);
            obs.observe(document.documentElement || document.body, { childList: true, subtree: true });
        })();
    """

    fun getYoutubeBootstrapScript(): String {
        return GPC_AND_DNT_JS + "\n" + YOUTUBE_FREEDOM_MOBILE_JS
    }

    fun injectEarlyScript(webView: WebView, isDesktop: Boolean = false) {
        webView.evaluateJavascript(GPC_AND_DNT_JS, null)
        val cosmeticCss = CosmeticFilterEngine.buildCosmeticCss()
        injectCss(webView, cosmeticCss, "safeer-cosmetic-filter")
        if (isDesktop) {
            webView.evaluateJavascript(WINDOWS_CHROME_ENVIRONMENT_JS, null)
        }
        webView.evaluateJavascript(ANTI_POPUNDER_SHIELD_JS, null)
        webView.evaluateJavascript(YOUTUBE_FREEDOM_MOBILE_JS, null)
        webView.evaluateJavascript(STREAMING_INSTANT_START_JS, null)
    }

    fun injectOnPageFinished(webView: WebView, isDarkMode: Boolean, isDesktop: Boolean = false) {
        webView.evaluateJavascript(GPC_AND_DNT_JS, null)
        val cosmeticCss = CosmeticFilterEngine.buildCosmeticCss()
        injectCss(webView, cosmeticCss, "safeer-cosmetic-filter")
        if (isDesktop) {
            webView.evaluateJavascript(WINDOWS_CHROME_ENVIRONMENT_JS, null)
        }
        webView.evaluateJavascript(ANTI_POPUNDER_SHIELD_JS, null)
        webView.evaluateJavascript(YOUTUBE_FREEDOM_MOBILE_JS, null)
        webView.evaluateJavascript(STREAMING_INSTANT_START_JS, null)

        if (isDarkMode) {
            injectCss(webView, DARK_MODE_AMOLED_CSS, "safeer-dark-mode-style")
        } else {
            removeCss(webView, "safeer-dark-mode-style")
        }
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
                    if (old) return;
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
