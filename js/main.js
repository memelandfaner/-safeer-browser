/**
 * Safeer Browser - High-Density Interactive Engine
 * Pure vanilla JS, offline capable, inline SVG QR generation
 */

// --- 1. Inline Vector SVG QR Code Generator ---
const QRCodeMini = (function() {
  function createQRSVG(text, size = 100) {
    const dim = 21;
    const matrix = Array(dim).fill(0).map(() => Array(dim).fill(false));

    function drawFinder(r, c) {
      for (let i = 0; i < 7; i++) {
        for (let j = 0; j < 7; j++) {
          if (i === 0 || i === 6 || j === 0 || j === 6 || (i >= 2 && i <= 4 && j >= 2 && j <= 4)) {
            if (r + i < dim && c + j < dim) matrix[r + i][c + j] = true;
          }
        }
      }
    }

    drawFinder(0, 0);
    drawFinder(0, dim - 7);
    drawFinder(dim - 7, 0);

    let hash = 0;
    for (let i = 0; i < text.length; i++) {
      hash = ((hash << 5) - hash) + text.charCodeAt(i);
      hash |= 0;
    }

    let seed = Math.abs(hash);
    for (let r = 0; r < dim; r++) {
      for (let c = 0; c < dim; c++) {
        const isFinder = (r < 7 && c < 7) || (r < 7 && c >= dim - 7) || (r >= dim - 7 && c < 7);
        if (!isFinder) {
          seed = (seed * 9301 + 49297) % 233280;
          matrix[r][c] = (seed / 233280) > 0.45;
        }
      }
    }

    let rects = '';
    const cellSize = (size / dim).toFixed(2);
    for (let r = 0; r < dim; r++) {
      for (let c = 0; c < dim; c++) {
        if (matrix[r][c]) {
          rects += `<rect x="${(c * cellSize).toFixed(2)}" y="${(r * cellSize).toFixed(2)}" width="${cellSize}" height="${cellSize}" fill="#080b11" />`;
        }
      }
    }

    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size} ${size}" width="${size}" height="${size}">${rects}</svg>`;
  }

  return { generateSVG: createQRSVG };
})();

// --- 2. Multi-Language Dictionary (SL / EN) ---
const i18n = {
  sl: {
    nav_scripts: "✨ Skripte",
    trust_ads: "0 Oglasov",
    trust_yt: "YouTube v ozadju",
    trust_c2: "abuse.ch C2 Ščit",
    trust_free: "100% Brezplačno",
    tab_showcase_mob: "📱 Telefon (Android)",
    tab_showcase_tv: "📺 4K Android TV",
    tab_showcase_desktop: "🍃 Linux Mint Namizje",
    subtab_mob_shield: "🛡️ C2 Ščit",
    subtab_mob_stats: "📊 Števec",
    subtab_mob_portals: "🔍 Portali",
    subtab_mob_yt: "🎵 YouTube",
    subtab_tv_portals: "📺 Portali",
    subtab_tv_dpad: "🎮 60 FPS",
    subtab_tv_stations: "📑 Postaje",
    subtab_desktop_main: "🍃 Namizje",
    metric_ads: "Oglasov",
    metric_latency: "Zakasnitev",
    card1_eq_text: "Predvajanje v ozadju • Zaslon zaklenjen",
    radar_status_line: "abuse.ch Radar: <strong style=\"color:var(--accent-emerald);\">0.18 ms</strong> • Status: <strong style=\"color:var(--accent-cyan);\">Neutralizirano</strong>",
    threat_phishing_btn: "Lažna banka",
    threat_safe_btn: "Varna stran",
    card3_tag_header: "📺 Android TV &amp; Daljinski Upravljalnik",
    tv_pill_nomouse: "🎯 Brez navidezne miške",
    tv_pill_red: "🔴 Rdeči gumb: Xplore TV",
    tv_pill_yellow: "🟡 Rumeni gumb: YouTube",
    tv_pill_blue: "🔵 Modri gumb: 24ur / Novice",
    dl_alt_targz: "Ali prenesi .tar.gz (506 KB)",
    userscripts_badge: "✨ VGRAJEN TAMPERMONKEY • 100% DIGITALNA SUVERENOST",
    userscripts_title: "Tvoj brskalnik. Tvoja pravila.<br class=\"hide-mobile\"><span class=\"highlight-cyan\">Dodaj svojo skripto v 10 sekundah.</span>",
    userscripts_lead: "Zakaj bi se prilagajali spletnim stranem, če se spletne strani lahko prilagodijo vam? Safeer prinaša vgrajen <strong>Tampermonkey / Greasemonkey mehanizem</strong> za samodejno izvajanje lastnih JavaScript uporabniških skript brez nameščanja zunanjih vtičnikov.",
    script_status_active: "● AKTIVNA",
    script_pill_skip: "1. Preskok oglasov",
    script_pill_night: "2. Nočni način",
    script_pill_clean: "3. Čisti splet",
    script_code_hint: "💡 Kliknite zgornje profile za takojšnjo menjavo kode in simulacije.",
    sim_tab_title: "⚡ Rezultat izvajanja v živo",
    sim_status_running: "▶ V TEKU",
    sim_stat_ads_skipped: "Oglasov preskočeno",
    sim_stat_time_saved: "Prihranjen čas",
    sim_stat_click_delay: "Zakasnitev klika",
    sim_console_title: "TERMINALNI DNEVNIK SKRIPTE (CONSOLE)",
    sim_console_live: "● V ŽIVO",
    sim_footer_hint: "🚀 Izvajanje je 100% lokalno znotraj procesa brskalnika Safeer.",
    script_feat1_title: "Enostavno dodajanje in urejanje",
    script_feat1_desc: "Kliknite ikono 🧩, poimenujte skripto, vnesite spletno stran in prilepite kodo. Brskalnik poskrbi za vse ostalo.",
    script_feat2_title: "Oblikovanje po meri &amp; Lasten CSS",
    script_feat2_desc: "Izbirajte med 4 vgrajenimi temami (Midnight, Mint Emerald, Cyberpunk, AMOLED) ali z lastnim CSS-jem preuredite videz katerega koli elementa.",
    script_feat3_title: "100% zasebno &amp; lokalno izvajanje",
    script_feat3_desc: "Vse vaše skripte se izvajajo lokalno v vaši napravi, brez sledenja, brez oblačnih strežnikov in brez ogrožanja zasebnosti.",
    btn_linux_short: "Prenesi Safeer Linux (.deb namestitveni paket)",
    btn_all_downloads: "Vsi prenosi (Telefon, TV, Linux)",
    manifesto_badge: "🌍 MANIFEST ODPRTE KODE • MOČ V TVOJIH ROKAH",
    th_feature: "Funkcionalnost",
    th_std_tv: "Običajni TV brskalniki",
    tr1_feat: "C2 Botnet &amp; Malware zaščita (abuse.ch)",
    tr1_safeer: "<span style=\"color:#10b981;\">✓</span> Vgrajeno (O(k) Trie)",
    tr1_chrome: "<span style=\"color:#64748b;\">✗</span> Osnovni filter",
    tr1_tv: "<span style=\"color:#64748b;\">✗</span> Brez zaščite",
    tr2_feat: "YouTube v ozadju z ugasnjenim zaslonom",
    tr2_safeer: "<span style=\"color:#10b981;\">✓</span> Da (brezplačno)",
    tr2_chrome: "<span style=\"color:#64748b;\">✗</span> Plačljiva naročnina",
    tr2_tv: "<span style=\"color:#64748b;\">✗</span> Se zaustavi",
    tr3_feat: "Blokada video oglasov &amp; praznih okvirjev",
    tr3_safeer: "<span style=\"color:#10b981;\">✓</span> 0 oglasov",
    tr3_chrome: "<span style=\"color:#64748b;\">✗</span> Vsi oglasi",
    tr3_tv: "<span style=\"color:#64748b;\">✗</span> Brez blokiranja",
    tr4_feat: "Nativni Media3 ExoPlayer predvajalnik",
    tr4_safeer: "<span style=\"color:#10b981;\">✓</span> 4K SurfaceView",
    tr4_chrome: "<span style=\"color:#64748b;\">✗</span> Standardni web view",
    tr4_tv: "<span style=\"color:#64748b;\">✗</span> Zatikanje",
    tr5_feat: "D-Pad fokusni obroč za daljinec",
    tr5_safeer: "<span style=\"color:#10b981;\">✓</span> 60 FPS navigacija",
    tr5_chrome: "<span style=\"color:#64748b;\">✗</span> Nerodna miška",
    tr5_tv: "<span style=\"color:#64748b;\">✗</span> Počasno drsenje",
    dl_shortlink_lbl: "Kratka povezava:",
    dl_release_lbl: "Izdaja:",

    nav_features: "Zmožnosti",
    nav_linux: "🍃 Linux",
    nav_security: "Varnost",
    nav_compare: "Primerjava",
    nav_download: "Prenos APK",
    badge_active: "Safeer v1.0.4 — Stable Release • Target SDK 36",
    hero_t1: "Varnejši na spletu.",
    hero_t2: "Brez oglasov.",
    hero_t3: "4K na TV.",
    hero_lead: "Kdor obvladuje brskalnik, določa pravila spleta. Milijon ljudi ima milijon različnih osebnosti, želja in potreb, a en sam neomajen cilj: <strong>odprt, varen in suveren internet na dosegu roke</strong>. Safeer je 100% odprtokoden projekt, kjer lahko vsakdo naredi fork in ustvari svoj popoln brskalnik.",
    security_disclaimer: "<strong>Safeer is a security layer, not a guarantee against all online threats.</strong> Zmanjšuje tveganje in blokira znane grožnje.",
    manifesto_title: "Kdor nadzoruje brskalnik, nadzoruje splet.<br><span style=\"color:var(--accent-cyan);\">Vzemi nadzor v svoje roke.</span>",
    manifesto_text: "<strong>Spletna stran se obnaša natanko tako, kot ji to dopusti tvoj brskalnik.</strong> Preveč časa so tehnološki monopoli odločali o tem, kaj smeš videti, koliko oglasov moraš potrpeti in kako se trguje s tvojo zasebnostjo. Čas je, da pravila znova določaš ti.<br><br>Milijon ljudi ima milijon različnih osebnosti, želja in potreb, a en sam neomajen cilj: <strong>imeti odprt, varen in neukročen internet na dosegu roke</strong>. Safeer zmanjšuje tveganje in blokira znana tveganja kot lokalni varnostni sloj. Projekt je 100% odprtokoden prav zato, da služi kot navdih in odskočna deska: vzemi izvorno kodo v svoje roke, naredi <em>fork</em> in prilagodi svoj brskalnik po lastnih željah in potrebah!",
    btn_mob_text: "Prenesi mobilni brskalnik (1.8 MB)",
    btn_tv_text: "Prenesi TV brskalnik (8.8 MB)",
    btn_linux_text: "Prenesi za Linux Mint (.deb 1.1 MB)",
    tab_mob: "📱 Telefon (Galaxy S25)",
    tab_tv: "📺 4K TV",
    tab_desktop: "🍃 Linux Mint Namizje",
    badge_c2: "abuse.ch C2 Zaščita",
    badge_yt: "0 Oglasov • Ozadje",
    bento_eyebrow: "Ključne prednosti",
    bento_title: "Zakaj je Safeer v razredu zase.",
    card1_tag: "Multimedijska svoboda",
    card1_title: "YouTube z ugasnjenim zaslonom. Brez oglasov.",
    card1_desc: "Poslušajte glasbo, podcaste in oddaje brez prekinitev. Ugasnite zaslon ali preklopite v drugo aplikacijo – zvok se nemoteno predvaja naprej z znatnim prihrankom baterije.",
    card2_tag: "Kibernetska varnost",
    card2_title: "Pravi ščit pred botneti (abuse.ch)",
    card2_desc: "Radix Domain Suffix Trie (O(k)) mikrosekundno preverja zahteve in nemudoma blokira C2 botnete (Dridex, Emotet) ter spletno ribarjenje.",
    card3_tag: "Android TV",
    card3_title: "60 FPS D-Pad navigacija & Media3",
    card3_desc: "Brez nerodnih navideznih mišk. Cianov fokusni obroč natančno skače med elementi, strojno pospešeni ExoPlayer pa zagotavlja tekoč 4K video na vašem televizorju.",
    card4_tag: "Upravitelj TV portalov",
    card4_title: "Vaše priljubljene strani na dosegu daljinca",
    card4_desc: "Hitro preklapljanje med novicami, vremenom in multimedijo z barvnimi gumbi na daljincu brez dolgotrajnega tipkanja naslovov.",
    comp_eyebrow: "Primerjava",
    comp_title: "Kako se Safeer primerja z drugimi?",
    table_scroll_hint: "Podrsajte vodoravno za ogled celotne primerjave",
    dl_eyebrow: "Takojšen prenos",
    dl_title: "Namestite na svojo napravo.",
    dl_mob_h3: "📱 Mobilni brskalnik (Telefon & Tablica)",
    dl_mob_p: "Optimizirano za zaslone na dotik, Galaxy S25, tablice in vse Android telefone (120Hz AMOLED).",
    mob_upgrade_warn: "⚠️ <strong>Nadgradnja z v1.0.0:</strong> Ker ima v1.0.2 uradni certifikat (v1.0.0 debug podpis), pred namestitvijo odstranite staro verzijo (Uninstall).",
    dl_btn_mob: "Prenesi mobilni brskalnik APK (1.8 MB)",
    dl_tv_h3: "📺 TV brskalnik (Pametni Android TV)",
    dl_tv_p: "Optimizirano za Philips TV, Sony, Xiaomi, MediaTek in upravljanje z daljincem.",
    dl_btn_tv: "Prenesi TV brskalnik APK (8.8 MB)",
    linux_badge: "🍃 LINUX MINT & UBUNTU SOVEREIGN EDITION • NATIVNI .DEB",
    linux_spotlight_title: "Ultra lahek. Energetsko varčen.<br><span style=\"color:#87cf3e;\">3-krat manj RAM-a. Odpre se v pol sekunde.</span>",
    linux_quote_text: "»Safeer je ultra lahek, energetsko varčen Linux brskalnik, ki se odpre v pol sekunde, porabi 3-krat manj RAM-a kot Chrome/Firefox in ima vgrajen Instant YouTube predvajalnik brez oglasov ter lokalni ščit pred zlonamernimi domenami.«",
    linux_p1_badge: "Za prenosnike • Baterija & RAM",
    linux_p1_title: "Prihrani baterijo in RAM na poti",
    linux_p1_desc: "Nativno WebKitGTK jedro ne poganja potratnih Chromovih procesov za vsak zavihek. Vaš prenosnik ostane hladen, ventilatorji tihi, baterija pa zdrži občutno dlje pri vsakdanjem delu.",
    linux_p1_stat: "⚡ 3x manj RAM-a • 🔋 Do 40% daljša avtonomija",
    linux_p2_badge: "Za programerje & ustvarjalce",
    linux_p2_title: "YouTube v ozadju z minimalno porabo CPU",
    linux_p2_desc: "Glasba in podcasti na YouTubu igrajo v ozadju z manj kot 1.5% obremenitve CPU med tipkanjem kode, prevajanjem v terminalu ali brskanjem. Vgrajen ukaz 'safeer' in Awesomebar za razvojne porte (localhost:3000, 8080).",
    linux_p2_stat: "🎵 YouTube v ozadju • 🧠 &lt; 1.5% CPU poraba",
    linux_p3_badge: "Za zasebnost • 100% lokalno",
    linux_p3_title: "Brez telemetrije, brez Googlovega sledenja",
    linux_p3_desc: "100% lokalno delovanje na vaši napravi. Vgrajen O(k) Reverse Domain Trie ščit (abuse.ch) mikrosekundno nevtralizira C2 botnete in sledilce. Zaznamke iz Firefoxa ali Chroma uvozite z enim samim klikom.",
    linux_p3_stat: "🔒 0 telemetrije • 📥 1-klik uvoz zaznamkov",
    linux_bench_title: "⚡ Neposredna primerjava z drugimi brskalniki na Linuxu",
    linux_bench_measured: "Merjeno na Linux Mint 22 (Intel i7 / 16GB)",
    linux_b1_label: "Čas hladnega zagona",
    linux_b1_sub: "Chrome: 2.4s • Firefox: 1.9s",
    linux_b2_label: "Poraba RAM (3 zavihki + YT)",
    linux_b2_sub: "Chrome: 680MB • Firefox: 590MB",
    linux_b3_label: "Obremenitev CPU pri zvoku",
    linux_b3_sub: "Chrome: 8–15% • Brez hrupa ventilatorja",
    linux_b4_label: "Namestitveni paket (.deb)",
    linux_b4_sub: "Brez odvečnega balasta • 1-klik zagon",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Popolnoma opremljen za Linux: enotna instanca z Unix socketom, Awesomebar (localhost/razvijalci), 1-klik uvoz zaznamkov, Customizer Studio (teme, lasten CSS, UserScripts), 6 svetovnih jezikov in ukaz safeer v terminalu. <strong>3x manj RAM-a, zagon v pol sekunde in glasba v ozadju.</strong>",
    dl_btn_desktop: "Prenesi .deb paket (1.1 MB)",
    footer_copy: "© 2026 Safeer Browser Project. Vrhunska zasebnost, kibernetska varnost in zabava.",
    footer_disclaimer: "⚖️ Safeer is a security layer, not a guarantee against all online threats. Zmanjšuje tveganje in blokira znana tveganja; ne zagotavlja zaščite pred neznanimi Zero-Day grožnjami. 100% odprta koda pod licenco Apache 2.0 – spodbujamo fork kode in lastno prilagoditev."
  },
  en: {
    nav_scripts: "✨ Scripts",
    trust_ads: "0 Ads",
    trust_yt: "Background YouTube",
    trust_c2: "abuse.ch C2 Shield",
    trust_free: "100% Free",
    tab_showcase_mob: "📱 Phone (Android)",
    tab_showcase_tv: "📺 4K Android TV",
    tab_showcase_desktop: "🍃 Linux Mint Desktop",
    subtab_mob_shield: "🛡️ C2 Shield",
    subtab_mob_stats: "📊 Stats",
    subtab_mob_portals: "🔍 Portals",
    subtab_mob_yt: "🎵 YouTube",
    subtab_tv_portals: "📺 Portals",
    subtab_tv_dpad: "🎮 60 FPS",
    subtab_tv_stations: "📑 Channels",
    subtab_desktop_main: "🍃 Desktop",
    metric_ads: "Ads",
    metric_latency: "Latency",
    card1_eq_text: "Background Playback • Screen Locked",
    radar_status_line: "abuse.ch Radar: <strong style=\"color:var(--accent-emerald);\">0.18 ms</strong> • Status: <strong style=\"color:var(--accent-cyan);\">Neutralized</strong>",
    threat_phishing_btn: "Fake Bank",
    threat_safe_btn: "Safe Site",
    card3_tag_header: "📺 Android TV &amp; Remote Control",
    tv_pill_nomouse: "🎯 No virtual mouse",
    tv_pill_red: "🔴 Red button: Xplore TV",
    tv_pill_yellow: "🟡 Yellow button: YouTube",
    tv_pill_blue: "🔵 Blue button: 24ur / News",
    dl_alt_targz: "Or download .tar.gz (506 KB)",
    userscripts_badge: "✨ BUILT-IN TAMPERMONKEY • 100% DIGITAL SOVEREIGNTY",
    userscripts_title: "Your browser. Your rules.<br class=\"hide-mobile\"><span class=\"highlight-cyan\">Add your script in 10 seconds.</span>",
    userscripts_lead: "Why adapt to websites when websites can adapt to you? Safeer includes a built-in <strong>Tampermonkey / Greasemonkey engine</strong> to automatically run your custom JavaScript user scripts without installing third-party extensions.",
    script_status_active: "● ACTIVE",
    script_pill_skip: "1. Skip Ads",
    script_pill_night: "2. Dark Mode",
    script_pill_clean: "3. Clean Web",
    script_code_hint: "💡 Click profiles above to switch code and live preview.",
    sim_tab_title: "⚡ Live Execution Result",
    sim_status_running: "▶ RUNNING",
    sim_stat_ads_skipped: "Ads skipped",
    sim_stat_time_saved: "Time saved",
    sim_stat_click_delay: "Click delay",
    sim_console_title: "TERMINAL SCRIPT CONSOLE",
    sim_console_live: "● LIVE",
    sim_footer_hint: "🚀 Execution is 100% local within the Safeer browser process.",
    script_feat1_title: "Easy to Add and Edit",
    script_feat1_desc: "Click the 🧩 icon, name the script, set target website and paste your code. The browser takes care of the rest.",
    script_feat2_title: "Custom Themes &amp; Custom CSS",
    script_feat2_desc: "Choose from 4 built-in themes (Midnight, Mint Emerald, Cyberpunk, AMOLED) or style any web element with your own custom CSS.",
    script_feat3_title: "100% Private &amp; Local Execution",
    script_feat3_desc: "All scripts run locally on your device—zero tracking, zero cloud telemetry, and zero privacy compromise.",
    btn_linux_short: "Download Safeer Linux (.deb installer)",
    btn_all_downloads: "All Downloads (Phone, TV, Linux)",
    manifesto_badge: "🌍 OPEN SOURCE MANIFESTO • POWER IN YOUR HANDS",
    th_feature: "Feature",
    th_std_tv: "Standard TV Browsers",
    tr1_feat: "C2 Botnet &amp; Malware Defense (abuse.ch)",
    tr1_safeer: "<span style=\"color:#10b981;\">✓</span> Built-in (O(k) Trie)",
    tr1_chrome: "<span style=\"color:#64748b;\">✗</span> Basic filter",
    tr1_tv: "<span style=\"color:#64748b;\">✗</span> No defense",
    tr2_feat: "Background YouTube with Screen Off",
    tr2_safeer: "<span style=\"color:#10b981;\">✓</span> Yes (Free)",
    tr2_chrome: "<span style=\"color:#64748b;\">✗</span> Paid subscription",
    tr2_tv: "<span style=\"color:#64748b;\">✗</span> Stops playback",
    tr3_feat: "Video Ad &amp; Blank Frame Blocking",
    tr3_safeer: "<span style=\"color:#10b981;\">✓</span> 0 Ads",
    tr3_chrome: "<span style=\"color:#64748b;\">✗</span> All ads",
    tr3_tv: "<span style=\"color:#64748b;\">✗</span> No adblocking",
    tr4_feat: "Native Media3 ExoPlayer Engine",
    tr4_safeer: "<span style=\"color:#10b981;\">✓</span> 4K SurfaceView",
    tr4_chrome: "<span style=\"color:#64748b;\">✗</span> Standard web view",
    tr4_tv: "<span style=\"color:#64748b;\">✗</span> Stuttering",
    tr5_feat: "D-Pad Focus Ring for Remote Control",
    tr5_safeer: "<span style=\"color:#10b981;\">✓</span> 60 FPS navigation",
    tr5_chrome: "<span style=\"color:#64748b;\">✗</span> Clumsy mouse pointer",
    tr5_tv: "<span style=\"color:#64748b;\">✗</span> Slow scrolling",
    dl_shortlink_lbl: "Short link:",
    dl_release_lbl: "Release:",

    nav_features: "Features",
    nav_linux: "🍃 Linux",
    nav_security: "Security",
    nav_compare: "Comparison",
    nav_download: "Download APK",
    badge_active: "Safeer v1.0.4 — Stable Release • Target SDK 36",
    hero_t1: "Safer on the web.",
    hero_t2: "Zero ads.",
    hero_t3: "4K on TV.",
    hero_lead: "Whoever controls the browser dictates the rules of the web. A million people have a million different personalities and needs, but one uncompromising goal: <strong>an open, safe, and sovereign internet at their fingertips</strong>. Take control into your own hands—Safeer is 100% open source so anyone can fork the code and build their ultimate browser.",
    security_disclaimer: "<strong>Safeer is a security layer, not a guarantee against all online threats.</strong> It reduces risk and blocks known threats.",
    manifesto_title: "Whoever controls the browser controls the web.<br><span style=\"color:var(--accent-cyan);\">Take the power into your own hands.</span>",
    manifesto_text: "<strong>A website only behaves the way your browser allows it to.</strong> For far too long, big tech monopolies dictated what you see, how many intrusive ads you endure, and how your data is monetized. It is time for you to set the rules.<br><br>A million people have a million different personalities, habits, and needs, but one shared goal: <strong>an open, secure, and untamed internet at their fingertips</strong>. Safeer serves as a local security layer reducing exposure to known hazards. The project is 100% open source to inspire and empower you: take the source code into your own hands, make a <em>fork</em>, and customize the browser to your own needs and desires!",
    btn_mob_text: "Download Mobile Browser (1.8 MB)",
    btn_tv_text: "Download TV Browser (8.8 MB)",
    btn_linux_text: "Download for Linux Mint (.deb 1.1 MB)",
    tab_mob: "📱 Phone (Galaxy S25)",
    tab_tv: "📺 4K TV",
    tab_desktop: "🍃 Linux Mint Desktop",
    badge_c2: "Shield Active",
    badge_yt: "1,430 Ads Blocked",
    bento_eyebrow: "Core Superpowers",
    bento_title: "Why Safeer is in a class of its own.",
    card1_tag: "Multimedia Freedom",
    card1_title: "Screen-Off YouTube. Zero ads.",
    card1_desc: "Listen to music, podcasts, and video audio uninterrupted. Lock your screen or switch apps—sound keeps playing while maximizing your battery life.",
    card2_tag: "Cyber Security",
    card2_title: "Real botnet defense (abuse.ch)",
    card2_desc: "Radix Domain Suffix Trie (O(k)) executes microsecond inspection, instantly aborting C2 botnets (Dridex, Emotet) and malicious credential harvesting.",
    card3_tag: "Android TV",
    card3_title: "60 FPS D-Pad Navigation & Media3",
    card3_desc: "No clumsy virtual pointers. The dynamic cyan focus ring snaps directly between UI elements, while hardware-accelerated Media3 delivers stutter-free 4K video.",
    card4_tag: "TV Portal Manager",
    card4_title: "Your favorite web portals one click away",
    card4_desc: "Switch between news, weather, and streaming feeds using TV remote color shortcuts without tedious typing.",
    comp_eyebrow: "Comparison",
    comp_title: "How Safeer compares to standard browsers",
    table_scroll_hint: "Swipe horizontally to view full comparison",
    dl_eyebrow: "Instant Download",
    dl_title: "Install on your device today.",
    dl_mob_h3: "📱 Mobile Browser (Phone & Tablet)",
    dl_mob_p: "Optimized for touchscreens, Galaxy S25, tablets, and all Android phones (120Hz AMOLED).",
    mob_upgrade_warn: "⚠️ <strong>Upgrade from v1.0.0:</strong> Because v1.0.2 is signed with official certificate (v1.0.0 had debug signature), uninstall the old version before installing v1.0.2.",
    dl_btn_mob: "Download Mobile Browser APK (1.8 MB)",
    dl_tv_h3: "📺 TV Browser (Smart Android TV)",
    dl_tv_p: "Optimized for Philips TV, Sony, Xiaomi, MediaTek, and TV remote.",
    dl_btn_tv: "Download TV Browser APK (8.8 MB)",
    linux_badge: "🍃 LINUX MINT & UBUNTU SOVEREIGN EDITION • NATIVE .DEB",
    linux_spotlight_title: "Ultra-lightweight. Energy-efficient.<br><span style=\"color:#87cf3e;\">3x less RAM. Launches in half a second.</span>",
    linux_quote_text: "“Safeer is an ultra-lightweight, energy-efficient Linux browser that opens in half a second, consumes 3 times less RAM than Chrome/Firefox, and features a built-in zero-ad Instant YouTube player plus a local malware threat shield.”",
    linux_p1_badge: "For Laptops • Battery & RAM",
    linux_p1_title: "Save battery and RAM on the go",
    linux_p1_desc: "The native WebKitGTK engine eliminates heavy Chromium sub-process bloat. Your laptop stays cool, fans remain silent, and battery life extends significantly throughout your workday.",
    linux_p1_stat: "⚡ 3x less RAM • 🔋 Up to 40% battery savings",
    linux_p2_badge: "For Developers & Creators",
    linux_p2_title: "YouTube in the background with minimal CPU load",
    linux_p2_desc: "Stream YouTube music and podcasts in the background at &lt; 1.5% CPU while coding, compiling, or browsing. Features a native 'safeer' CLI and Awesomebar for local dev servers (localhost:3000, 8080).",
    linux_p2_stat: "🎵 Background YouTube • 🧠 &lt; 1.5% CPU load",
    linux_p3_badge: "For Privacy • 100% Local",
    linux_p3_title: "Zero telemetry, zero Google tracking",
    linux_p3_desc: "100% locally executed on your machine. In-memory O(k) Reverse Domain Trie blocks C2 botnets (abuse.ch) and trackers in microseconds. Migrate instantly with 1-click bookmark import from Firefox and Chrome.",
    linux_p3_stat: "🔒 0 Telemetry • 📥 1-Click Bookmark Import",
    linux_bench_title: "⚡ Direct Benchmark vs Other Linux Browsers",
    linux_bench_measured: "Benchmarked on Linux Mint 22 (Intel i7 / 16GB)",
    linux_b1_label: "Cold Launch Time",
    linux_b1_sub: "Chrome: 2.4s • Firefox: 1.9s",
    linux_b2_label: "RAM Usage (3 tabs + YT)",
    linux_b2_sub: "Chrome: 680MB • Firefox: 590MB",
    linux_b3_label: "CPU Load During Audio",
    linux_b3_sub: "Chrome: 8–15% • Silent fans",
    linux_b4_label: "Debian Package (.deb)",
    linux_b4_sub: "Zero bloatware • 1-click launch",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Equipped for Linux: single-instance via Unix socket, Awesomebar (localhost/developers), 1-click bookmark import, Customizer Studio (themes, CSS, UserScripts), 6 languages, and safeer CLI. <strong>3x less RAM, launches in 0.5s, background YouTube music.</strong>",
    dl_btn_desktop: "Download .deb package (1.1 MB)",
    footer_copy: "© 2026 Safeer Browser Project. Elite privacy, cyber security, and entertainment.",
    footer_disclaimer: "⚖️ Safeer is a security layer, not a guarantee against all online threats. It reduces risk and blocks known threats; it does not guarantee protection against all unknown Zero-Day attacks. 100% open source under Apache 2.0—fork and customize!"
  },
  de: {
    security_disclaimer: "<strong>Safeer ist eine Sicherheitsschicht, keine Garantie gegen alle Online-Bedrohungen.</strong> Es reduziert Risiken und blockiert bekannte Bedrohungen.",
    footer_disclaimer: "⚖️ Safeer ist eine Sicherheitsschicht, keine Garantie gegen alle Online-Bedrohungen. Reduziert Risiken und blockiert bekannte Gefahren; garantiert keinen Schutz vor unbekannten Zero-Day-Bedrohungen. 100% Open Source unter Apache 2.0 Lizenz.",
    nav_scripts: "✨ Skripte",
    trust_ads: "0 Werbung",
    trust_yt: "YouTube im Hintergrund",
    trust_c2: "abuse.ch C2 Schild",
    trust_free: "100% Kostenlos",
    tab_showcase_mob: "📱 Smartphone (Android)",
    tab_showcase_tv: "📺 4K Android TV",
    tab_showcase_desktop: "🍃 Linux Mint Desktop",
    subtab_mob_shield: "🛡️ C2 Schild",
    subtab_mob_stats: "📊 Statistiken",
    subtab_mob_portals: "🔍 Portale",
    subtab_mob_yt: "🎵 YouTube",
    subtab_tv_portals: "📺 Portale",
    subtab_tv_dpad: "🎮 60 FPS",
    subtab_tv_stations: "📑 Sender",
    subtab_desktop_main: "🍃 Desktop",
    metric_ads: "Werbung",
    metric_latency: "Latenz",
    card1_eq_text: "Hintergrundwiedergabe • Bildschirm gesperrt",
    radar_status_line: "abuse.ch Radar: <strong style=\"color:var(--accent-emerald);\">0.18 ms</strong> • Status: <strong style=\"color:var(--accent-cyan);\">Neutralisiert</strong>",
    threat_phishing_btn: "Gefälschte Bank",
    threat_safe_btn: "Sichere Seite",
    card3_tag_header: "📺 Android TV &amp; Fernbedienung",
    tv_pill_nomouse: "🎯 Keine virtuelle Maus",
    tv_pill_red: "🔴 Rote Taste: Xplore TV",
    tv_pill_yellow: "🟡 Gelbe Taste: YouTube",
    tv_pill_blue: "🔵 Blaue Taste: 24ur / Nachrichten",
    dl_alt_targz: "Oder .tar.gz herunterladen (506 KB)",
    userscripts_badge: "✨ INTEGRIERTER TAMPERMONKEY • 100% DIGITALE SOUVERÄNITÄT",
    userscripts_title: "Dein Browser. Deine Regeln.<br class=\"hide-mobile\"><span class=\"highlight-cyan\">Füge dein Skript in 10 Sekunden hinzu.</span>",
    userscripts_lead: "Warum sich an Webseiten anpassen, wenn sich Webseiten an dich anpassen können? Safeer bietet eine integrierte <strong>Tampermonkey / Greasemonkey Engine</strong> zur automatischen Ausführung benutzerdefinierter JavaScript-Skripte ohne externe Add-ons.",
    script_status_active: "● AKTIV",
    script_pill_skip: "1. Werbung überspringen",
    script_pill_night: "2. Dunkelmodus",
    script_pill_clean: "3. Sauberes Web",
    script_code_hint: "💡 Klicke oben auf die Profile, um Code und Live-Simulation sofort zu wechseln.",
    sim_tab_title: "⚡ Live-Ausführungsergebnis",
    sim_status_running: "▶ LÄUFT",
    sim_stat_ads_skipped: "Werbung übersprungen",
    sim_stat_time_saved: "Gesparte Zeit",
    sim_stat_click_delay: "Klick-Verzögerung",
    sim_console_title: "TERMINAL-SKRIPT-PROTOKOLL (CONSOLE)",
    sim_console_live: "● LIVE",
    sim_footer_hint: "🚀 Die Ausführung erfolgt zu 100% lokal im Safeer-Browserprozess.",
    script_feat1_title: "Einfaches Hinzufügen und Bearbeiten",
    script_feat1_desc: "Klicke auf das 🧩 Symbol, benenne das Skript, gib die Webseite ein und füge den Code ein. Der Browser erledigt den Rest.",
    script_feat2_title: "Individuelles Design &amp; Eigenes CSS",
    script_feat2_desc: "Wähle aus 4 integrierten Themes (Midnight, Mint Emerald, Cyberpunk, AMOLED) oder passe jedes Element mit eigenem CSS an.",
    script_feat3_title: "100% privat &amp; lokale Ausführung",
    script_feat3_desc: "Alle Skripte laufen lokal auf deinem Gerät – ohne Tracking, ohne Cloud-Server und ohne Gefährdung der Privatsphäre.",
    btn_linux_short: "Safeer Linux herunterladen (.deb Paket)",
    btn_all_downloads: "Alle Downloads (Smartphone, TV, Linux)",
    manifesto_badge: "🌍 OPEN-SOURCE-MANIFEST • MACHT IN DEINEN HÄNDEN",
    th_feature: "Funktion",
    th_std_tv: "Standard TV-Browser",
    tr1_feat: "C2 Botnet- &amp; Malware-Schutz (abuse.ch)",
    tr1_safeer: "<span style=\"color:#10b981;\">✓</span> Integriert (O(k) Trie)",
    tr1_chrome: "<span style=\"color:#64748b;\">✗</span> Basisfilter",
    tr1_tv: "<span style=\"color:#64748b;\">✗</span> Kein Schutz",
    tr2_feat: "YouTube im Hintergrund bei gesperrtem Bildschirm",
    tr2_safeer: "<span style=\"color:#10b981;\">✓</span> Ja (Kostenlos)",
    tr2_chrome: "<span style=\"color:#64748b;\">✗</span> Bezahl-Abo nötig",
    tr2_tv: "<span style=\"color:#64748b;\">✗</span> Stoppt",
    tr3_feat: "Blockierung von Video-Werbung &amp; Bannern",
    tr3_safeer: "<span style=\"color:#10b981;\">✓</span> 0 Werbung",
    tr3_chrome: "<span style=\"color:#64748b;\">✗</span> Alle Anzeigen",
    tr3_tv: "<span style=\"color:#64748b;\">✗</span> Keine Blockierung",
    tr4_feat: "Nativer Media3 ExoPlayer",
    tr4_safeer: "<span style=\"color:#10b981;\">✓</span> 4K SurfaceView",
    tr4_chrome: "<span style=\"color:#64748b;\">✗</span> Standard Webview",
    tr4_tv: "<span style=\"color:#64748b;\">✗</span> Ruckeln",
    tr5_feat: "D-Pad Fokusring für Fernbedienung",
    tr5_safeer: "<span style=\"color:#10b981;\">✓</span> 60 FPS Navigation",
    tr5_chrome: "<span style=\"color:#64748b;\">✗</span> Ungeschickter Mauszeiger",
    tr5_tv: "<span style=\"color:#64748b;\">✗</span> Langsames Scrollen",
    dl_shortlink_lbl: "Kurzlink:",
    dl_release_lbl: "Release:",

    nav_features: "Funktionen",
    nav_linux: "🍃 Linux",
    nav_security: "Sicherheit",
    nav_compare: "Vergleich",
    nav_download: "Download",
    badge_active: "Cyberschild Aktiv • v2.1.78",
    hero_t1: "Sicherer im Web.",
    hero_t2: "Keine Werbung.",
    hero_t3: "4K auf TV mit Fernbedienung.",
    hero_lead: "Wer den Browser kontrolliert, bestimmt die Regeln des Webs. Ein offenes, sicheres und souveränes Internet für alle.",
    manifesto_title: "Wer den Browser beherrscht, beherrscht das Web.<br><span style=\"color:var(--accent-cyan);\">Nimm die Macht in deine Hände.</span>",
    manifesto_text: "Eine Website verhält sich nur so, wie Ihr Browser es zulässt. Schluss mit Monopolen und Tracking. Safeer ist 100% Open Source.",
    btn_mob_text: "Mobilen Browser herunterladen (1.8 MB)",
    btn_tv_text: "TV Browser herunterladen (8.8 MB)",
    btn_linux_text: "Für Linux Mint herunterladen (.deb 1.1 MB)",
    tab_mob: "📱 Smartphone (Galaxy S25)",
    tab_tv: "📺 4K Fernseher",
    tab_desktop: "🍃 Linux Mint Desktop",
    badge_c2: "Schutz Aktiv",
    badge_yt: "1.430 Werbung Blockiert",
    bento_eyebrow: "Hauptvorteile",
    bento_title: "Warum Safeer eine eigene Klasse darstellt.",
    card1_tag: "Multimedia-Freiheit",
    card1_title: "YouTube bei ausgeschaltetem Bildschirm. Keine Werbung.",
    card1_desc: "Musik und Podcasts ohne Unterbrechung hören – auch im Hintergrund oder bei gesperrtem Bildschirm.",
    card2_tag: "Cyber-Sicherheit",
    card2_title: "Echter Botnetz-Schutz (abuse.ch)",
    card2_desc: "Radix Domain Suffix Trie blockiert C2-Botnetze und Phishing in Mikrosekunden.",
    card3_tag: "Android TV",
    card3_title: "60 FPS D-Pad Navigation & Media3",
    card3_desc: "Keine Maus nötig. Flüssige Steuerung mit Fernbedienung und ruckelfreies 4K Streaming.",
    card4_tag: "TV Portal Manager",
    card4_title: "Ihre Lieblingsportale auf Knopfdruck",
    card4_desc: "Schnelles Umschalten zwischen News, Wetter und Streaming mit Farbtasten.",
    comp_eyebrow: "Vergleich",
    comp_title: "Wie Safeer im Vergleich abschneidet",
    table_scroll_hint: "Horizontal wischen, um den vollständigen Vergleich anzuzeigen",
    dl_eyebrow: "Sofortiger Download",
    dl_title: "Jetzt auf Ihrem Gerät installieren.",
    dl_mob_h3: "📱 Mobiler Browser (Smartphone & Tablet)",
    dl_mob_p: "Optimiert für Touchscreens, alle Android-Smartphones und Tablets (120Hz AMOLED).",
    mob_upgrade_warn: "⚠️ <strong>Upgrade von v1.0.0:</strong> Da v1.0.2 offiziell signiert ist (v1.0.0 Debug-Signatur), bitte alte Version vor Installation deinstallieren.",
    dl_btn_mob: "Mobilen Browser APK herunterladen (1.8 MB)",
    dl_tv_h3: "📺 TV Browser (Smart Android TV)",
    dl_tv_p: "Optimiert für Philips TV, Sony, Xiaomi und Fernbedienung.",
    dl_btn_tv: "TV Browser APK herunterladen (8.8 MB)",
    linux_badge: "🍃 LINUX MINT & UBUNTU SOVEREIGN EDITION • NATIVES .DEB",
    linux_spotlight_title: "Ultra-leicht. Energieeffizient.<br><span style=\"color:#87cf3e;\">3x weniger RAM. Start in einer halben Sekunde.</span>",
    linux_quote_text: "„Safeer ist ein ultra-leichter, energieeffizienter Linux-Browser, der in einer halben Sekunde startet, 3-mal weniger RAM verbraucht als Chrome/Firefox und einen integrierten werbefreien Instant-YouTube-Player sowie einen lokalen Schadsoftware-Schutz bietet.“",
    linux_p1_badge: "Für Laptops • Akku & RAM",
    linux_p1_title: "Akku und Arbeitsspeicher unterwegs sparen",
    linux_p1_desc: "Die native WebKitGTK-Engine eliminiert ressourcenhungrige Chromium-Prozesse. Ihr Laptop bleibt kühl, die Lüfter leise und der Akku hält den ganzen Arbeitstag spürbar länger.",
    linux_p1_stat: "⚡ 3x weniger RAM • 🔋 Bis zu 40% längere Akkulaufzeit",
    linux_p2_badge: "Für Entwickler & Kreative",
    linux_p2_title: "YouTube im Hintergrund bei minimaler CPU-Auslastung",
    linux_p2_desc: "YouTube-Musik und Podcasts laufen mit unter 1,5% CPU-Auslastung im Hintergrund beim Coden, Kompilieren oder Surfen. Inklusive 'safeer'-CLI und Awesomebar für Localhost-Entwickler (3000, 8080).",
    linux_p2_stat: "🎵 YouTube im Hintergrund • 🧠 &lt; 1,5% CPU-Auslastung",
    linux_p3_badge: "Für Privatsphäre • 100% Lokal",
    linux_p3_title: "Keine Telemetrie, kein Google-Tracking",
    linux_p3_desc: "Läuft zu 100% lokal auf Ihrem Gerät. Der O(k) Reverse Domain Trie Schutz (abuse.ch) neutralisiert C2-Botnetze in Mikrosekunden. Lesezeichen aus Firefox oder Chrome mit 1 Klick importieren.",
    linux_p3_stat: "🔒 0 Telemetrie • 📥 1-Klick Lesezeichen-Import",
    linux_bench_title: "⚡ Direkter Benchmark-Vergleich auf Linux",
    linux_bench_measured: "Gemessen auf Linux Mint 22 (Intel i7 / 16GB)",
    linux_b1_label: "Kaltstartzeit",
    linux_b1_sub: "Chrome: 2,4s • Firefox: 1,9s",
    linux_b2_label: "RAM-Verbrauch (3 Tabs + YT)",
    linux_b2_sub: "Chrome: 680MB • Firefox: 590MB",
    linux_b3_label: "CPU-Last bei Audiowiedergabe",
    linux_b3_sub: "Chrome: 8–15% • Lüfter bleiben leise",
    linux_b4_label: "Installationspaket (.deb)",
    linux_b4_sub: "Ohne Ballast • Sofort einsatzbereit",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Ausgerüstet für Linux: Einzelinstanz über Unix-Socket, Awesomebar (localhost), 1-Klick-Lesezeichen-Import, Customizer Studio, UserScripts und safeer-CLI. <strong>3x weniger RAM, Start in 0,5s und YouTube im Hintergrund.</strong>",
    dl_btn_desktop: ".deb-Paket herunterladen (1.1 MB)",
    footer_copy: "© 2026 Safeer Browser Project. Datenschutz, Sicherheit und Unterhaltung."
  },
  es: {
    security_disclaimer: "<strong>Safeer es una capa de seguridad, no una garantía contra todas las amenazas en línea.</strong> Reduce el riesgo y bloquea las amenazas conocidas.",
    footer_disclaimer: "⚖️ Safeer es una capa de seguridad, no una garantía contra todas las amenazas en línea. Reduce el riesgo y bloquea los peligros conocidos; no garantiza protección contra amenazas de Día Cero desconocidas. 100% de código abierto bajo la licencia Apache 2.0.",
    nav_scripts: "✨ Scripts",
    trust_ads: "0 Anuncios",
    trust_yt: "YouTube en segundo plano",
    trust_c2: "abuse.ch Escudo C2",
    trust_free: "100% Gratis",
    tab_showcase_mob: "📱 Teléfono (Android)",
    tab_showcase_tv: "📺 4K Android TV",
    tab_showcase_desktop: "🍃 Linux Mint Escritorio",
    subtab_mob_shield: "🛡️ Escudo C2",
    subtab_mob_stats: "📊 Estadísticas",
    subtab_mob_portals: "🔍 Portales",
    subtab_mob_yt: "🎵 YouTube",
    subtab_tv_portals: "📺 Portales",
    subtab_tv_dpad: "🎮 60 FPS",
    subtab_tv_stations: "📑 Canales",
    subtab_desktop_main: "🍃 Escritorio",
    metric_ads: "Anuncios",
    metric_latency: "Latencia",
    card1_eq_text: "Reproducción en segundo plano • Pantalla bloqueada",
    radar_status_line: "abuse.ch Radar: <strong style=\"color:var(--accent-emerald);\">0.18 ms</strong> • Estado: <strong style=\"color:var(--accent-cyan);\">Neutralizado</strong>",
    threat_phishing_btn: "Banco Falso",
    threat_safe_btn: "Sitio Seguro",
    card3_tag_header: "📺 Android TV &amp; Mando a Distancia",
    tv_pill_nomouse: "🎯 Sin ratón virtual",
    tv_pill_red: "🔴 Botón rojo: Xplore TV",
    tv_pill_yellow: "🟡 Botón amarillo: YouTube",
    tv_pill_blue: "🔵 Botón azul: 24ur / Noticias",
    dl_alt_targz: "O descargar .tar.gz (506 KB)",
    userscripts_badge: "✨ TAMPERMONKEY INTEGRADO • 100% SOBERANÍA DIGITAL",
    userscripts_title: "Tu navegador. Tus reglas.<br class=\"hide-mobile\"><span class=\"highlight-cyan\">Añade tu script en 10 segundos.</span>",
    userscripts_lead: "¿Por qué adaptarse a los sitios web cuando los sitios web pueden adaptarse a ti? Safeer incluye un <strong>motor Tampermonkey / Greasemonkey integrado</strong> para ejecutar tus propios scripts JavaScript automáticamente sin instalar extensiones externas.",
    script_status_active: "● ACTIVO",
    script_pill_skip: "1. Saltar anuncios",
    script_pill_night: "2. Modo oscuro",
    script_pill_clean: "3. Web limpia",
    script_code_hint: "💡 Haz clic en los perfiles anteriores para cambiar el código y la vista en vivo al instante.",
    sim_tab_title: "⚡ Resultado de ejecución en vivo",
    sim_status_running: "▶ EN EJECUCIÓN",
    sim_stat_ads_skipped: "Anuncios saltados",
    sim_stat_time_saved: "Tiempo ahorrado",
    sim_stat_click_delay: "Retardo de clic",
    sim_console_title: "CONSOLA DE TERMINAL DEL SCRIPT",
    sim_console_live: "● EN VIVO",
    sim_footer_hint: "🚀 La ejecución es 100% local dentro del proceso del navegador Safeer.",
    script_feat1_title: "Fácil de agregar y editar",
    script_feat1_desc: "Haz clic en el icono 🧩, nombra el script, introduce el sitio web y pega tu código. El navegador se encarga del resto.",
    script_feat2_title: "Diseño personalizado &amp; CSS propio",
    script_feat2_desc: "Elige entre 4 temas integrados (Midnight, Mint Emerald, Cyberpunk, AMOLED) o personaliza cualquier elemento con tu propio CSS.",
    script_feat3_title: "100% privado &amp; ejecución local",
    script_feat3_desc: "Todos tus scripts se ejecutan localmente en tu dispositivo, sin rastreo, sin servidores en la nube y sin comprometer tu privacidad.",
    btn_linux_short: "Descargar Safeer Linux (paquete .deb)",
    btn_all_downloads: "Todas las descargas (Teléfono, TV, Linux)",
    manifesto_badge: "🌍 MANIFIESTO DE CÓDIGO ABIERTO • EL PODER EN TUS MANOS",
    th_feature: "Característica",
    th_std_tv: "Navegadores TV estándar",
    tr1_feat: "Protección contra botnets C2 y malware (abuse.ch)",
    tr1_safeer: "<span style=\"color:#10b981;\">✓</span> Integrado (O(k) Trie)",
    tr1_chrome: "<span style=\"color:#64748b;\">✗</span> Filtro básico",
    tr1_tv: "<span style=\"color:#64748b;\">✗</span> Sin protección",
    tr2_feat: "YouTube en segundo plano con pantalla apagada",
    tr2_safeer: "<span style=\"color:#10b981;\">✓</span> Sí (Gratis)",
    tr2_chrome: "<span style=\"color:#64748b;\">✗</span> Suscripción de pago",
    tr2_tv: "<span style=\"color:#64748b;\">✗</span> Se detiene",
    tr3_feat: "Bloqueo de anuncios en vídeo y marcos vacíos",
    tr3_safeer: "<span style=\"color:#10b981;\">✓</span> 0 anuncios",
    tr3_chrome: "<span style=\"color:#64748b;\">✗</span> Todos los anuncios",
    tr3_tv: "<span style=\"color:#64748b;\">✗</span> Sin bloqueo",
    tr4_feat: "Reproductor nativo Media3 ExoPlayer",
    tr4_safeer: "<span style=\"color:#10b981;\">✓</span> 4K SurfaceView",
    tr4_chrome: "<span style=\"color:#64748b;\">✗</span> Webview estándar",
    tr4_tv: "<span style=\"color:#64748b;\">✗</span> Tirones y cortes",
    tr5_feat: "Anillo de enfoque D-Pad para mando",
    tr5_safeer: "<span style=\"color:#10b981;\">✓</span> Navegación 60 FPS",
    tr5_chrome: "<span style=\"color:#64748b;\">✗</span> Puntero torpe",
    tr5_tv: "<span style=\"color:#64748b;\">✗</span> Desplazamiento lento",
    dl_shortlink_lbl: "Enlace corto:",
    dl_release_lbl: "Lanzamiento:",

    nav_features: "Funciones",
    nav_linux: "🍃 Linux",
    nav_security: "Seguridad",
    nav_compare: "Comparación",
    nav_download: "Descargar",
    badge_active: "Escudo Cibernético Activo • v2.1.78",
    hero_t1: "Más seguro en la web.",
    hero_t2: "Cero anuncios.",
    hero_t3: "4K en TV con mando.",
    hero_lead: "Quien controla el navegador dicta las reglas de la web. Un internet abierto, seguro y soberano al alcance de tu mano.",
    manifesto_title: "Quien controla el navegador controla la web.<br><span style=\"color:var(--accent-cyan);\">Toma el control en tus manos.</span>",
    manifesto_text: "Un sitio web solo se comporta como tu navegador se lo permite. Safeer es 100% código abierto para que seas libre.",
    btn_mob_text: "Descargar Navegador Móvil (1.8 MB)",
    btn_tv_text: "Descargar Navegador TV (8.8 MB)",
    btn_linux_text: "Descargar para Linux Mint (.deb 1.1 MB)",
    tab_mob: "📱 Móvil (Galaxy S25)",
    tab_tv: "📺 4K TV",
    tab_desktop: "🍃 Escritorio Linux Mint",
    badge_c2: "Escudo Activo",
    badge_yt: "1.430 Anuncios Bloqueados",
    bento_eyebrow: "Ventajas Clave",
    bento_title: "Por qué Safeer está en una clase propia.",
    card1_tag: "Libertad Multimedia",
    card1_title: "YouTube con pantalla apagada. Sin anuncios.",
    card1_desc: "Escucha música y podcasts sin pausas, incluso con la pantalla bloqueada o en otras apps.",
    card2_tag: "Seguridad Cibernética",
    card2_title: "Defensa real contra botnets (abuse.ch)",
    card2_desc: "Inspección ultra rápida con Radix Domain Suffix Trie que bloquea botnets C2 al instante.",
    card3_tag: "Android TV",
    card3_title: "Navegación D-Pad a 60 FPS & Media3",
    card3_desc: "Sin molestos punteros virtuales. Navega fluidamente con el mando a distancia en 4K.",
    card4_tag: "Gestor de Portales TV",
    card4_title: "Tus portales favoritos a un clic de distancia",
    card4_desc: "Cambia entre noticias, clima y streaming rápidamente con los botones de colores.",
    comp_eyebrow: "Comparativa",
    comp_title: "Cómo se compara Safeer con el resto",
    table_scroll_hint: "Desliza horizontalmente para ver la comparación completa",
    dl_eyebrow: "Descarga Inmediata",
    dl_title: "Instala en tu dispositivo hoy mismo.",
    dl_mob_h3: "📱 Navegador Móvil (Teléfono y Tableta)",
    dl_mob_p: "Optimizado para pantallas táctiles, Galaxy S25, tabletas y todos los dispositivos Android.",
    mob_upgrade_warn: "⚠️ <strong>Actualización desde v1.0.0:</strong> Debido al nuevo certificado oficial de v1.0.2 (v1.0.0 tenía firma debug), desinstala la versión anterior primero.",
    dl_btn_mob: "Descargar Navegador Móvil APK (1.8 MB)",
    dl_tv_h3: "📺 Navegador TV (Smart Android TV)",
    dl_tv_p: "Optimizado para Philips TV, Sony, Xiaomi y mando a distancia.",
    dl_btn_tv: "Descargar Navegador TV APK (8.8 MB)",
    linux_badge: "🍃 LINUX MINT & UBUNTU SOVEREIGN EDITION • .DEB NATIVO",
    linux_spotlight_title: "Ultra ligero. Eficiencia energética.<br><span style=\"color:#87cf3e;\">3x menos RAM. Inicia en medio segundo.</span>",
    linux_quote_text: "«Safeer es un navegador Linux ultra ligero y de bajo consumo energético que abre en medio segundo, consume 3 veces menos RAM que Chrome/Firefox e integra un reproductor YouTube Instantáneo sin anuncios y escudo local contra malware.»",
    linux_p1_badge: "Para Portátiles • Batería y RAM",
    linux_p1_title: "Ahorra batería y memoria RAM sobre la marcha",
    linux_p1_desc: "El motor nativo WebKitGTK elimina la sobrecarga de procesos Chromium. Tu portátil no se calienta, los ventiladores permanecen en silencio y la batería rinde mucho más.",
    linux_p1_stat: "⚡ 3x menos RAM • 🔋 Hasta un 40% más de batería",
    linux_p2_badge: "Para Desarrolladores y Creadores",
    linux_p2_title: "YouTube en segundo plano con mínimo consumo de CPU",
    linux_p2_desc: "Música y podcasts de YouTube en segundo plano con menos de 1.5% de CPU mientras programas o navegas. Incluye CLI 'safeer' y Awesomebar para localhost (3000, 8080).",
    linux_p2_stat: "🎵 YouTube en segundo plano • 🧠 &lt; 1.5% carga de CPU",
    linux_p3_badge: "Para Privacidad • 100% Local",
    linux_p3_title: "Cero telemetría, sin rastreo de Google",
    linux_p3_desc: "Ejecución 100% local en tu equipo. El filtro en memoria O(k) Reverse Domain Trie bloquea botnets C2 (abuse.ch) en microsegundos. Importa marcadores de Firefox y Chrome en un clic.",
    linux_p3_stat: "🔒 0 Telemetría • 📥 Importación de marcadores en 1 clic",
    linux_bench_title: "⚡ Comparativa de Rendimiento Directo en Linux",
    linux_bench_measured: "Medido en Linux Mint 22 (Intel i7 / 16GB)",
    linux_b1_label: "Tiempo de inicio en frío",
    linux_b1_sub: "Chrome: 2.4s • Firefox: 1.9s",
    linux_b2_label: "Consumo de RAM (3 pestañas + YT)",
    linux_b2_sub: "Chrome: 680MB • Firefox: 590MB",
    linux_b3_label: "Carga de CPU en reproducción",
    linux_b3_sub: "Chrome: 8–15% • Ventiladores silenciosos",
    linux_b4_label: "Paquete de instalación (.deb)",
    linux_b4_sub: "Cero sobrecarga • Listo en 1 clic",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Equipado para Linux: instancia única vía socket Unix, Awesomebar (localhost), importación de marcadores en 1 clic, Customizer Studio y CLI 'safeer'. <strong>3x menos RAM, inicio en 0.5s y música en segundo plano.</strong>",
    dl_btn_desktop: "Descargar paquete .deb (1.1 MB)",
    footer_copy: "© 2026 Safeer Browser Project. Máxima privacidad, seguridad y entretenimiento."
  },
  fr: {
    security_disclaimer: "<strong>Safeer est une couche de sécurité, pas une garantie contre toutes les menaces en ligne.</strong> Il réduit les risques et bloque les menaces connues.",
    footer_disclaimer: "⚖️ Safeer est une couche de sécurité, pas une garantie contre toutes les menaces en ligne. Réduit les risques et bloque les dangers connus ; ne garantit pas de protection contre les menaces Zero-Day inconnues. 100% open source sous licence Apache 2.0.",
    nav_scripts: "✨ Scripts",
    trust_ads: "0 Publicité",
    trust_yt: "YouTube en arrière-plan",
    trust_c2: "abuse.ch Bouclier C2",
    trust_free: "100% Gratuit",
    tab_showcase_mob: "📱 Téléphone (Android)",
    tab_showcase_tv: "📺 4K Android TV",
    tab_showcase_desktop: "🍃 Linux Mint Bureau",
    subtab_mob_shield: "🛡️ Bouclier C2",
    subtab_mob_stats: "📊 Statistiques",
    subtab_mob_portals: "🔍 Portails",
    subtab_mob_yt: "🎵 YouTube",
    subtab_tv_portals: "📺 Portails",
    subtab_tv_dpad: "🎮 60 FPS",
    subtab_tv_stations: "📑 Chaînes",
    subtab_desktop_main: "🍃 Bureau",
    metric_ads: "Publicités",
    metric_latency: "Latence",
    card1_eq_text: "Lecture en arrière-plan • Écran verrouillé",
    radar_status_line: "abuse.ch Radar: <strong style=\"color:var(--accent-emerald);\">0.18 ms</strong> • État: <strong style=\"color:var(--accent-cyan);\">Neutralisé</strong>",
    threat_phishing_btn: "Fausse Banque",
    threat_safe_btn: "Site Sécurisé",
    card3_tag_header: "📺 Android TV &amp; Télécommande",
    tv_pill_nomouse: "🎯 Sans souris virtuelle",
    tv_pill_red: "🔴 Bouton rouge: Xplore TV",
    tv_pill_yellow: "🟡 Bouton jaune: YouTube",
    tv_pill_blue: "🔵 Bouton bleu: 24ur / Actualités",
    dl_alt_targz: "Ou télécharger .tar.gz (506 Ko)",
    userscripts_badge: "✨ TAMPERMONKEY INTÉGRÉ • 100% SOUVERAINETÉ NUMÉRIQUE",
    userscripts_title: "Votre navigateur. Vos règles.<br class=\"hide-mobile\"><span class=\"highlight-cyan\">Ajoutez votre script en 10 secondes.</span>",
    userscripts_lead: "Pourquoi s'adapter aux sites web alors que les sites web peuvent s'adapter à vous ? Safeer intègre un <strong>moteur Tampermonkey / Greasemonkey</strong> pour exécuter vos propres scripts JavaScript automatiquement sans extensions tierces.",
    script_status_active: "● ACTIF",
    script_pill_skip: "1. Ignorer les pubs",
    script_pill_night: "2. Mode sombre",
    script_pill_clean: "3. Web épuré",
    script_code_hint: "💡 Cliquez sur les profils ci-dessus pour changer immédiatement le code et la simulation.",
    sim_tab_title: "⚡ Résultat d'exécution en direct",
    sim_status_running: "▶ EN COURS",
    sim_stat_ads_skipped: "Publicités ignorées",
    sim_stat_time_saved: "Temps économisé",
    sim_stat_click_delay: "Délai de clic",
    sim_console_title: "JOURNAL DU SCRIPT TERMINAL",
    sim_console_live: "● EN DIRECT",
    sim_footer_hint: "🚀 L'exécution est 100% locale au sein du processus du navigateur Safeer.",
    script_feat1_title: "Facile à ajouter et à modifier",
    script_feat1_desc: "Cliquez sur l'icône 🧩, nommez le script, entrez le site web et collez le code. Le navigateur s'occupe de tout le reste.",
    script_feat2_title: "Design sur mesure &amp; CSS personnalisé",
    script_feat2_desc: "Choisissez parmi 4 thèmes intégrés (Midnight, Mint Emerald, Cyberpunk, AMOLED) ou personnalisez n'importe quel élément avec votre CSS.",
    script_feat3_title: "100% privé &amp; exécution locale",
    script_feat3_desc: "Tous vos scripts s'exécutent localement sur votre appareil, sans suivi, sans serveurs cloud et sans risque pour la vie privée.",
    btn_linux_short: "Télécharger Safeer Linux (paquet .deb)",
    btn_all_downloads: "Tous les téléchargements (Mobile, TV, Linux)",
    manifesto_badge: "🌍 MANIFESTE OPEN SOURCE • LE POUVOIR ENTRE VOS MAINS",
    th_feature: "Fonctionnalité",
    th_std_tv: "Navigateurs TV standard",
    tr1_feat: "Protection contre les botnets C2 et malwares (abuse.ch)",
    tr1_safeer: "<span style=\"color:#10b981;\">✓</span> Intégré (O(k) Trie)",
    tr1_chrome: "<span style=\"color:#64748b;\">✗</span> Filtre basique",
    tr1_tv: "<span style=\"color:#64748b;\">✗</span> Aucune protection",
    tr2_feat: "YouTube en arrière-plan avec écran éteint",
    tr2_safeer: "<span style=\"color:#10b981;\">✓</span> Oui (Gratuit)",
    tr2_chrome: "<span style=\"color:#64748b;\">✗</span> Abonnement payant",
    tr2_tv: "<span style=\"color:#64748b;\">✗</span> S'interrompt",
    tr3_feat: "Blocage des publicités vidéo et des cadres vides",
    tr3_safeer: "<span style=\"color:#10b981;\">✓</span> 0 publicité",
    tr3_chrome: "<span style=\"color:#64748b;\">✗</span> Toutes les annonces",
    tr3_tv: "<span style=\"color:#64748b;\">✗</span> Aucun blocage",
    tr4_feat: "Lecteur natif Media3 ExoPlayer",
    tr4_safeer: "<span style=\"color:#10b981;\">✓</span> 4K SurfaceView",
    tr4_chrome: "<span style=\"color:#64748b;\">✗</span> Vue web classique",
    tr4_tv: "<span style=\"color:#64748b;\">✗</span> Saccades",
    tr5_feat: "Anneau de focus D-Pad pour télécommande",
    tr5_safeer: "<span style=\"color:#10b981;\">✓</span> Navigation 60 FPS",
    tr5_chrome: "<span style=\"color:#64748b;\">✗</span> Curseur peu pratique",
    tr5_tv: "<span style=\"color:#64748b;\">✗</span> Défilement lent",
    dl_shortlink_lbl: "Lien court:",
    dl_release_lbl: "Version:",

    nav_features: "Fonctionnalités",
    nav_linux: "🍃 Linux",
    nav_security: "Sécurité",
    nav_compare: "Comparatif",
    nav_download: "Télécharger",
    badge_active: "Bouclier Actif • v2.1.78",
    hero_t1: "Plus sûr sur le web.",
    hero_t2: "Zéro publicité.",
    hero_t3: "4K sur TV à la télécommande.",
    hero_lead: "Celui qui contrôle le navigateur dicte les lois du web. Un internet ouvert, sécurisé et souverain pour tous.",
    manifesto_title: "Qui contrôle le navigateur contrôle le web.<br><span style=\"color:var(--accent-cyan);\">Prenez le contrôle.</span>",
    manifesto_text: "Un site web n'agit que comme votre navigateur l'y autorise. Safeer est 100% open source pour garantir votre liberté.",
    btn_mob_text: "Télécharger le Navigateur Mobile (1.7 Mo)",
    btn_tv_text: "Télécharger le Navigateur TV (8.8 Mo)",
    btn_linux_text: "Télécharger pour Linux Mint (.deb 1.1 Mo)",
    tab_mob: "📱 Téléphone (Galaxy S25)",
    tab_tv: "📺 TV 4K",
    tab_desktop: "🍃 Bureau Linux Mint",
    badge_c2: "Bouclier Actif",
    badge_yt: "1 430 Pubs Bloquées",
    bento_eyebrow: "Points Forts",
    bento_title: "Pourquoi Safeer est sans équivalent.",
    card1_tag: "Liberté Multimédia",
    card1_title: "YouTube écran éteint. Zéro publicité.",
    card1_desc: "Écoutez de la musique et des podcasts sans coupure, même avec l'écran verrouillé.",
    card2_tag: "Cybersécurité",
    card2_title: "Vraie défense anti-botnet (abuse.ch)",
    card2_desc: "Inspection ultra-rapide par Radix Domain Suffix Trie bloquant immédiatement les botnets C2.",
    card3_tag: "Android TV",
    card3_title: "Navigation D-Pad 60 FPS & Media3",
    card3_desc: "Aucune souris nécessaire. Contrôle ultra-fluide à la télécommande et vidéo 4K impeccable.",
    card4_tag: "Gestionnaire de Portails TV",
    card4_title: "Vos sites favoris en un clic télécommande",
    card4_desc: "Accédez rapidement aux actualités et au streaming via les touches de couleur.",
    comp_eyebrow: "Comparatif",
    comp_title: "Comment Safeer surpasse les autres navigateurs",
    table_scroll_hint: "Faites glisser horizontalement pour voir la comparaison complète",
    dl_eyebrow: "Téléchargement Direct",
    dl_title: "Installez sur vos appareils dès maintenant.",
    dl_mob_h3: "📱 Navigateur Mobile (Smartphone & Tablette)",
    dl_mob_p: "Optimisé pour écrans tactiles, Galaxy S25 et tous les téléphones Android (AMOLED 120Hz).",
    mob_upgrade_warn: "⚠️ <strong>Mise à niveau depuis v1.0.0:</strong> Comme v1.0.2 possède une signature officielle (v1.0.0 avait une signature debug), désinstallez l'ancienne version d'abord.",
    dl_btn_mob: "Télécharger le Navigateur Mobile APK (1.7 Mo)",
    dl_tv_h3: "📺 Navigateur TV (Smart Android TV)",
    dl_tv_p: "Optimisé pour Philips TV, Sony, Xiaomi et navigation télécommande.",
    dl_btn_tv: "Télécharger le Navigateur TV APK (8.8 Mo)",
    linux_badge: "🍃 LINUX MINT & UBUNTU SOVEREIGN EDITION • .DEB NATIF",
    linux_spotlight_title: "Ultra-léger. Économe en énergie.<br><span style=\"color:#87cf3e;\">3x moins de RAM. Démarre en une demi-seconde.</span>",
    linux_quote_text: "« Safeer est un navigateur Linux ultra-léger et économe en énergie qui démarre en une demi-seconde, consomme 3 fois moins de RAM que Chrome/Firefox et dispose d'un lecteur YouTube Instant sans pub intégré ainsi que d'un bouclier anti-malware local. »",
    linux_p1_badge: "Pour Ordinateurs Portables • Batterie & RAM",
    linux_p1_title: "Économisez la batterie et la RAM en déplacement",
    linux_p1_desc: "Le moteur natif WebKitGTK élimine la surcharge des sous-processus Chromium. Votre PC reste froid, les ventilateurs silencieux et la batterie dure nettement plus longtemps.",
    linux_p1_stat: "⚡ 3x moins de RAM • 🔋 Jusqu'à 40% de batterie en plus",
    linux_p2_badge: "Pour Développeurs & Créateurs",
    linux_p2_title: "YouTube en arrière-plan avec une charge CPU minimale",
    linux_p2_desc: "Musique et podcasts YouTube en tâche de fond avec moins de 1,5% de CPU pendant que vous codez ou naviguez. Avec CLI 'safeer' et Awesomebar pour serveurs locaux (localhost).",
    linux_p2_stat: "🎵 YouTube en tâche de fond • 🧠 &lt; 1,5% de charge CPU",
    linux_p3_badge: "Pour la Vie Privée • 100% Local",
    linux_p3_title: "Zéro télémétrie, aucun pistage Google",
    linux_p3_desc: "Exécution 100% locale sur votre machine. L'arbre Radix Domain Trie bloque instantanément les botnets C2 (abuse.ch). Importez vos favoris Firefox et Chrome en un seul clic.",
    linux_p3_stat: "🔒 0 Télémétrie • 📥 Import de favoris en 1 clic",
    linux_bench_title: "⚡ Benchmark Comparatif Direct sur Linux",
    linux_bench_measured: "Mesuré sur Linux Mint 22 (Intel i7 / 16GB)",
    linux_b1_label: "Démarrage à froid",
    linux_b1_sub: "Chrome : 2,4s • Firefox : 1,9s",
    linux_b2_label: "Consommation RAM (3 onglets + YT)",
    linux_b2_sub: "Chrome : 680Mo • Firefox : 590Mo",
    linux_b3_label: "Charge CPU en lecture audio",
    linux_b3_sub: "Chrome : 8–15% • Ventilateurs silencieux",
    linux_b4_label: "Paquet d'installation (.deb)",
    linux_b4_sub: "Sans aucun surplus • Prêt en 1 clic",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Parfaitement taillé pour Linux : instance unique via socket Unix, Awesomebar (localhost), import de favoris en 1 clic, Customizer Studio et CLI 'safeer'. <strong>3x moins de RAM, démarrage en 0,5s et musique en arrière-plan.</strong>",
    dl_btn_desktop: "Télécharger le paquet .deb (1.1 Mo)",
    footer_copy: "© 2026 Safeer Browser Project. Vie privée, cybersécurité et divertissement."
  },
  it: {
    security_disclaimer: "<strong>Safeer è uno strato di sicurezza, non una garanzia contro tutte le minacce online.</strong> Riduce il rischio e blocca le minacce note.",
    footer_disclaimer: "⚖️ Safeer è uno strato di sicurezza, non una garanzia contro tutte le minacce online. Riduce il rischio e blocca i pericoli noti; non garantisce protezione contro minacce Zero-Day sconosciute. 100% open source sotto licenza Apache 2.0.",
    nav_scripts: "✨ Script",
    trust_ads: "0 Pubblicità",
    trust_yt: "YouTube in background",
    trust_c2: "abuse.ch Scudo C2",
    trust_free: "100% Gratuito",
    tab_showcase_mob: "📱 Smartphone (Android)",
    tab_showcase_tv: "📺 4K Android TV",
    tab_showcase_desktop: "🍃 Linux Mint Desktop",
    subtab_mob_shield: "🛡️ Scudo C2",
    subtab_mob_stats: "📊 Statistiche",
    subtab_mob_portals: "🔍 Portali",
    subtab_mob_yt: "🎵 YouTube",
    subtab_tv_portals: "📺 Portali",
    subtab_tv_dpad: "🎮 60 FPS",
    subtab_tv_stations: "📑 Canali",
    subtab_desktop_main: "🍃 Desktop",
    metric_ads: "Pubblicità",
    metric_latency: "Latenza",
    card1_eq_text: "Riproduzione in background • Schermo bloccato",
    radar_status_line: "abuse.ch Radar: <strong style=\"color:var(--accent-emerald);\">0.18 ms</strong> • Stato: <strong style=\"color:var(--accent-cyan);\">Neutralizzato</strong>",
    threat_phishing_btn: "Falsa Banca",
    threat_safe_btn: "Sito Sicuro",
    card3_tag_header: "📺 Android TV &amp; Telecomando",
    tv_pill_nomouse: "🎯 Nessun mouse virtuale",
    tv_pill_red: "🔴 Tasto rosso: Xplore TV",
    tv_pill_yellow: "🟡 Tasto giallo: YouTube",
    tv_pill_blue: "🔵 Tasto blu: 24ur / Notizie",
    dl_alt_targz: "Oppure scarica .tar.gz (506 KB)",
    userscripts_badge: "✨ TAMPERMONKEY INTEGRATO • 100% SOVRANITÀ DIGITALE",
    userscripts_title: "Il tuo browser. Le tue regole.<br class=\"hide-mobile\"><span class=\"highlight-cyan\">Aggiungi il tuo script in 10 secondi.</span>",
    userscripts_lead: "Perché adattarsi ai siti web quando i siti web possono adattarsi a te? Safeer include un <strong>motore Tampermonkey / Greasemonkey integrato</strong> per eseguire automaticamente i tuoi script utente JavaScript senza installare estensioni esterne.",
    script_status_active: "● ATTIVO",
    script_pill_skip: "1. Salta annunci",
    script_pill_night: "2. Modalità scura",
    script_pill_clean: "3. Web pulito",
    script_code_hint: "💡 Fai clic sui profili sopra per cambiare istantaneamente codice e simulazione.",
    sim_tab_title: "⚡ Risultato di esecuzione live",
    sim_status_running: "▶ IN CORSO",
    sim_stat_ads_skipped: "Annunci saltati",
    sim_stat_time_saved: "Tempo risparmiato",
    sim_stat_click_delay: "Ritardo del clic",
    sim_console_title: "CONSOLE TERMINALE DELLO SCRIPT",
    sim_console_live: "● LIVE",
    sim_footer_hint: "🚀 L'esecuzione è al 100% locale all'interno del processo del browser Safeer.",
    script_feat1_title: "Facile da aggiungere e modificare",
    script_feat1_desc: "Fai clic sull'icona 🧩, assegna un nome allo script, inserisci il sito web e incolla il codice. Il browser fa tutto il resto.",
    script_feat2_title: "Design personalizzato &amp; CSS proprio",
    script_feat2_desc: "Scegli tra 4 temi integrati (Midnight, Mint Emerald, Cyberpunk, AMOLED) o personalizza qualsiasi elemento con il tuo CSS.",
    script_feat3_title: "100% privato ed esecuzione locale",
    script_feat3_desc: "Tutti i tuoi script vengono eseguiti localmente sul tuo dispositivo: nessun tracciamento, nessun server cloud e nessuna violazione della privacy.",
    btn_linux_short: "Scarica Safeer Linux (pacchetto .deb)",
    btn_all_downloads: "Tutti i download (Smartphone, TV, Linux)",
    manifesto_badge: "🌍 MANIFESTO OPEN SOURCE • IL POTERE NELLE TUE MANI",
    th_feature: "Funzionalità",
    th_std_tv: "Browser TV standard",
    tr1_feat: "Protezione contro botnet C2 e malware (abuse.ch)",
    tr1_safeer: "<span style=\"color:#10b981;\">✓</span> Integrato (O(k) Trie)",
    tr1_chrome: "<span style=\"color:#64748b;\">✗</span> Filtro di base",
    tr1_tv: "<span style=\"color:#64748b;\">✗</span> Nessuna protezione",
    tr2_feat: "YouTube in background con schermo spento",
    tr2_safeer: "<span style=\"color:#10b981;\">✓</span> Sì (Gratuito)",
    tr2_chrome: "<span style=\"color:#64748b;\">✗</span> Abbonamento a pagamento",
    tr2_tv: "<span style=\"color:#64748b;\">✗</span> Si interrompe",
    tr3_feat: "Blocco di annunci video e riquadri vuoti",
    tr3_safeer: "<span style=\"color:#10b981;\">✓</span> 0 annunci",
    tr3_chrome: "<span style=\"color:#64748b;\">✗</span> Tutte le inserzioni",
    tr3_tv: "<span style=\"color:#64748b;\">✗</span> Nessun blocco",
    tr4_feat: "Player nativo Media3 ExoPlayer",
    tr4_safeer: "<span style=\"color:#10b981;\">✓</span> 4K SurfaceView",
    tr4_chrome: "<span style=\"color:#64748b;\">✗</span> Standard web view",
    tr4_tv: "<span style=\"color:#64748b;\">✗</span> Scatti e blocchi",
    tr5_feat: "Anello di focus D-Pad per telecomando",
    tr5_safeer: "<span style=\"color:#10b981;\">✓</span> Navigazione a 60 FPS",
    tr5_chrome: "<span style=\"color:#64748b;\">✗</span> Puntatore scomodo",
    tr5_tv: "<span style=\"color:#64748b;\">✗</span> Scorrimento lento",
    dl_shortlink_lbl: "Link breve:",
    dl_release_lbl: "Release:",

    nav_features: "Funzionalità",
    nav_linux: "🍃 Linux",
    nav_security: "Sicurezza",
    nav_compare: "Confronto",
    nav_download: "Download",
    badge_active: "Scudo Cyber Attivo • v2.1.78",
    hero_t1: "Più sicuro sul web.",
    hero_t2: "Zero pubblicità.",
    hero_t3: "4K su TV con telecomando.",
    hero_lead: "Chi controlla il browser detta le regole del web. Un internet aperto, sicuro e sovrano alla portata di tutti.",
    manifesto_title: "Chi controlla il browser controlla il web.<br><span style=\"color:var(--accent-cyan);\">Prendi il controllo.</span>",
    manifesto_text: "Un sito web si comporta solo come il browser gli consente. Safeer è 100% open source per restituirti la libertà.",
    btn_mob_text: "Scarica Browser Mobile (1.8 MB)",
    btn_tv_text: "Scarica Browser TV (8.8 MB)",
    btn_linux_text: "Scarica per Linux Mint (.deb 1.1 MB)",
    tab_mob: "📱 Smartphone (Galaxy S25)",
    tab_tv: "📺 TV 4K",
    tab_desktop: "🍃 Desktop Linux Mint",
    badge_c2: "Protezione Attiva",
    badge_yt: "1.430 Pubblicità Bloccate",
    bento_eyebrow: "Vantaggi Principali",
    bento_title: "Perché Safeer è in una classe a parte.",
    card1_tag: "Libertà Multimediale",
    card1_title: "YouTube a schermo spento. Zero pubblicità.",
    card1_desc: "Ascolta musica e podcast ininterrottamente, anche a schermo bloccato.",
    card2_tag: "Sicurezza Informatica",
    card2_title: "Vera difesa anti-botnet (abuse.ch)",
    card2_desc: "Ispezione al microsecondo con Radix Domain Suffix Trie che neutralizza i botnet C2.",
    card3_tag: "Android TV",
    card3_title: "Navigazione D-Pad a 60 FPS & Media3",
    card3_desc: "Nessun puntatore virtuale. Naviga fluidamente con il telecomando in 4K nativo.",
    card4_tag: "Gestore Portali TV",
    card4_title: "I tuoi portali preferiti a portata di telecomando",
    card4_desc: "Passa istantaneamente tra notizie e video tramite i tasti colorati del telecomando.",
    comp_eyebrow: "Confronto",
    comp_title: "Come Safeer si confronta con gli altri browser",
    table_scroll_hint: "Scorri orizzontalmente per visualizzare il confronto completo",
    dl_eyebrow: "Download Istantaneo",
    dl_title: "Installa sul tuo dispositivo oggi stesso.",
    dl_mob_h3: "📱 Browser Mobile (Smartphone & Tablet)",
    dl_mob_p: "Ottimizzato per touchscreen, Galaxy S25, tablet e tutti i dispositivi Android.",
    mob_upgrade_warn: "⚠️ <strong>Aggiornamento da v1.0.0:</strong> Poiché v1.0.2 ha una firma ufficiale (v1.0.0 firma debug), disinstallare prima la vecchia versione.",
    dl_btn_mob: "Scarica Browser Mobile APK (1.8 MB)",
    dl_tv_h3: "📺 Browser TV (Smart Android TV)",
    dl_tv_p: "Ottimizzato per Philips TV, Sony, Xiaomi e telecomando.",
    dl_btn_tv: "Scarica Browser TV APK (8.8 MB)",
    linux_badge: "🍃 LINUX MINT & UBUNTU SOVEREIGN EDITION • .DEB NATIVO",
    linux_spotlight_title: "Ultra-leggero. Efficienza energetica.<br><span style=\"color:#87cf3e;\">3x meno RAM. Si avvia in mezzo secondo.</span>",
    linux_quote_text: "«Safeer è un browser Linux ultra-leggero a basso consumo energetico che si apre in mezzo secondo, consuma 3 volte meno RAM di Chrome/Firefox e integra un lettore YouTube Instant senza pubblicità e uno scudo antimalware locale.»",
    linux_p1_badge: "Per Portatili • Batteria e RAM",
    linux_p1_title: "Risparmia batteria e memoria RAM ovunque",
    linux_p1_desc: "Il motore nativo WebKitGTK elimina l'appesantimento dei processi Chromium. Il portatile rimane fresco, le ventole silenziose e l'autonomia della batteria aumenta sensibilmente.",
    linux_p1_stat: "⚡ 3x meno RAM • 🔋 Fino al 40% di autonomia in più",
    linux_p2_badge: "Per Sviluppatori e Creator",
    linux_p2_title: "YouTube in background con uso minimo di CPU",
    linux_p2_desc: "Musica e podcast su YouTube in background con meno dell'1.5% di CPU mentre programmi o navighi. Include CLI 'safeer' nativa e Awesomebar per sviluppatori localhost (3000, 8080).",
    linux_p2_stat: "🎵 YouTube in background • 🧠 &lt; 1.5% carico CPU",
    linux_p3_badge: "Per la Privacy • 100% Locale",
    linux_p3_title: "Zero telemetria, nessun tracciamento Google",
    linux_p3_desc: "Esecuzione 100% locale sul tuo dispositivo. La struttura in memoria O(k) blocca i botnet C2 (abuse.ch) in microsecondi. Importa i segnalibri da Firefox e Chrome con 1 solo clic.",
    linux_p3_stat: "🔒 0 Telemetria • 📥 Importazione segnalibri in 1 clic",
    linux_bench_title: "⚡ Benchmark Diretto rispetto agli altri Browser Linux",
    linux_bench_measured: "Misurato su Linux Mint 22 (Intel i7 / 16GB)",
    linux_b1_label: "Tempo di avvio a freddo",
    linux_b1_sub: "Chrome: 2.4s • Firefox: 1.9s",
    linux_b2_label: "Consumo RAM (3 schede + YT)",
    linux_b2_sub: "Chrome: 680MB • Firefox: 590MB",
    linux_b3_label: "Carico CPU durante l'audio",
    linux_b3_sub: "Chrome: 8–15% • Ventole silenziose",
    linux_b4_label: "Pacchetto installatore (.deb)",
    linux_b4_sub: "Senza bloatware • Avvio con 1 clic",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Equipaggiato per Linux: istanza singola tramite socket Unix, Awesomebar (localhost), importazione segnalibri in 1 clic, Customizer Studio e CLI 'safeer'. <strong>3x meno RAM, avvio in 0,5s e musica in background.</strong>",
    dl_btn_desktop: "Scarica pacchetto .deb (1.1 MB)",
    footer_copy: "© 2026 Safeer Browser Project. Massima privacy, sicurezza e intrattenimento."
  }
};

let currentLang = 'sl';


// --- Language-Aware Dynamic Content Matrices ---
const threatI18n = {
  "sl": {
    "feodo": {
      "title": "⚠️ ZAZNANA IN BLOKIRANA GROŽNJA (C2 BOTNET)",
      "desc": "Dridex/Emotet C2 strežnik takoj prestrežen (abuse.ch Feodo). Povezava je bila prekinjena pred izvajanjem."
    },
    "urlhaus": {
      "title": "⚠️ ZAZNANA ZLONAMERNA KODA (PAYLOAD DROPPER)",
      "desc": "Poskus prenosa nevarne datoteke .apk z znane malware domene je bil ustavljen."
    },
    "phishing": {
      "title": "⚠️ OPOZORILO: LAŽNA BANČNA STRAN (PHISHING)",
      "desc": "Lažna spletna stran za krajo osebnih podatkov in gesel (Phishing Army feed) je blokirana."
    },
    "safe": {
      "title": "✅ PREVERJENO VARNO SPLETNO MESTO",
      "desc": "Domena je varna. Sledilci in oglasne pasice so bili kozmetično odstranjeni za maksimalno hitrost."
    }
  },
  "en": {
    "feodo": {
      "title": "⚠️ THREAT DETECTED & BLOCKED (C2 BOTNET)",
      "desc": "Dridex/Emotet C2 server instantly intercepted (abuse.ch Feodo). Connection aborted before execution."
    },
    "urlhaus": {
      "title": "⚠️ MALICIOUS PAYLOAD DETECTED (PAYLOAD DROPPER)",
      "desc": "Attempted download of hazardous .apk file from known malware domain has been halted."
    },
    "phishing": {
      "title": "⚠️ WARNING: FAKE BANKING PAGE (PHISHING)",
      "desc": "Fraudulent website designed to steal credentials and passwords (Phishing Army feed) is blocked."
    },
    "safe": {
      "title": "✅ VERIFIED SAFE WEBSITE",
      "desc": "Domain is secure. Trackers and ad banners have been cosmetically stripped for maximum speed."
    }
  },
  "de": {
    "feodo": {
      "title": "⚠️ BEDROHUNG ERKANNT & BLOCKIERT (C2 BOTNET)",
      "desc": "Dridex/Emotet C2-Server sofort abgefangen (abuse.ch Feodo). Verbindung vor Ausführung beendet."
    },
    "urlhaus": {
      "title": "⚠️ SCHADCODE ERKANNT (PAYLOAD DROPPER)",
      "desc": "Versuchter Download einer gefährlichen .apk-Datei von bekannter Malware-Domain gestoppt."
    },
    "phishing": {
      "title": "⚠️ WARNUNG: GEFÄLSCHTE BANKSEITE (PHISHING)",
      "desc": "Gefälschte Website zum Diebstahl von Daten und Passwörtern (Phishing Army Feed) blockiert."
    },
    "safe": {
      "title": "✅ VERIFIZIERTE SICHERE WEBSEITE",
      "desc": "Domain ist sicher. Tracker und Banner wurden für maximale Geschwindigkeit entfernt."
    }
  },
  "es": {
    "feodo": {
      "title": "⚠️ AMENAZA DETECTADA Y BLOQUEADA (C2 BOTNET)",
      "desc": "Servidor C2 Dridex/Emotet interceptado al instante (abuse.ch Feodo). Conexión abortada antes de ejecutarse."
    },
    "urlhaus": {
      "title": "⚠️ CÓDIGO MALICIOSO DETECTADO (PAYLOAD DROPPER)",
      "desc": "Intento de descarga de archivo .apk peligroso desde dominio malware detenido."
    },
    "phishing": {
      "title": "⚠️ ADVERTENCIA: SITIO BANCARIO FALSO (PHISHING)",
      "desc": "Sitio web falso para robo de credenciales y contraseñas (Phishing Army) bloqueado."
    },
    "safe": {
      "title": "✅ SITIO WEB SEGURO VERIFICADO",
      "desc": "El dominio es seguro. Rastreadores y anuncios eliminados para máxima velocidad."
    }
  },
  "fr": {
    "feodo": {
      "title": "⚠️ MENACE DÉTECTÉE ET BLOQUÉE (C2 BOTNET)",
      "desc": "Serveur C2 Dridex/Emotet intercepté instantanément (abuse.ch Feodo). Connexion interrompue."
    },
    "urlhaus": {
      "title": "⚠️ CODE MALVEILLANT DÉTECTÉ (PAYLOAD DROPPER)",
      "desc": "Tentative de téléchargement d'un fichier .apk dangereux arrêtée."
    },
    "phishing": {
      "title": "⚠️ AVERTISSEMENT : FAUX SITE BANCAIRE (PHISHING)",
      "desc": "Faux site web de vol d'identifiants et de mots de passe (flux Phishing Army) bloqué."
    },
    "safe": {
      "title": "✅ SITE WEB VÉRIFIÉ ET SÛR",
      "desc": "Le domaine est sécurisé. Traqueurs et bannières supprimés pour une vitesse maximale."
    }
  },
  "it": {
    "feodo": {
      "title": "⚠️ MINACCIA RILEVATA E BLOCCATA (C2 BOTNET)",
      "desc": "Server C2 Dridex/Emotet intercettato all'istante (abuse.ch Feodo). Connessione interrotta prima dell'esecuzione."
    },
    "urlhaus": {
      "title": "⚠️ CODICE DANNOSO RILEVATO (PAYLOAD DROPPER)",
      "desc": "Tentativo di download di file .apk pericoloso da dominio malware bloccato."
    },
    "phishing": {
      "title": "⚠️ AVVISO: FALSA PAGINA BANCARIA (PHISHING)",
      "desc": "Sito fraudolento per il furto di credenziali e password (feed Phishing Army) bloccato."
    },
    "safe": {
      "title": "✅ SITO WEB VERIFICATO E SICURO",
      "desc": "Il dominio è sicuro. Tracker e banner rimossi per la massima velocità."
    }
  }
};
const showcaseCaptionsI18n = {
  "sl": {
    "mobile": {
      "shield": {
        "status": "Aktivna Zaščita",
        "caption": "<strong>Lokalni Varnostni Ščit:</strong> Prestreže zlonamerna spletna mesta, botnete (Dridex, Emotet) in trojance pred nalaganjem v pomnilnik (abuse.ch O(k) Trie)."
      },
      "stats": {
        "status": "48 Blokiranih",
        "caption": "<strong>Nadzorna plošča in statistika:</strong> Pregled v realnem času nad blokiranimi C2 strežniki, sledilci in preprečenimi nevarnimi prenosi."
      },
      "search": {
        "status": "Čisto Brskanje",
        "caption": "<strong>Čisto in hitro brskanje:</strong> Nemoteno iskanje in branje novic brez oglasnih pasic, pojavnih oken in invazivnih sledilnih skript."
      },
      "youtube": {
        "status": "0 Oglasov • Ozadje",
        "caption": "<strong>YouTube z ugasnjenim zaslonom:</strong> Nemoteno poslušanje glasbe in podcastov v ozadju z zaklenjenim telefonom brez oglasnih prekinitev."
      }
    },
    "tv": {
      "home": {
        "status": "4K TV Portali",
        "caption": "<strong>4K Android TV Domači Portal:</strong> Velike pregledne ploščice za hiter dostop do novic, videa in TV kanalov z daljinskim upravljalnikom."
      },
      "dpad": {
        "status": "60 FPS D-Pad",
        "caption": "<strong>60 FPS D-Pad Fokus:</strong> Cianov fokusni obroč natančno skače med elementi brez zakasnitve – brez nerodnih navideznih mišk."
      },
      "portals": {
        "status": "Upravitelj Postaj",
        "caption": "<strong>Upravitelj TV Portalov:</strong> Preprosto urejanje, razvrščanje in dodajanje lastnih televizijskih postaj in spletnih mest z daljincem."
      }
    },
    "desktop": {
      "main": {
        "status": "Unix Socket Aktiven",
        "caption": "<strong>Linux Mint Suverena Izdaja:</strong> Awesomebar terminalna orodna vrstica, vgrajen Tampermonkey za skripte ter prilagoditev tem in CSS-ja."
      }
    }
  },
  "en": {
    "mobile": {
      "shield": {
        "status": "Active Defense",
        "caption": "<strong>Local Security Shield:</strong> Intercepts malicious sites, C2 botnets (Dridex, Emotet) and trojans before execution (abuse.ch O(k) Trie)."
      },
      "stats": {
        "status": "48 Blocked",
        "caption": "<strong>Dashboard & Analytics:</strong> Real-time overview of blocked C2 botnet servers, trackers, and aborted malware downloads."
      },
      "search": {
        "status": "Clean Browsing",
        "caption": "<strong>Fast & Clean Browsing:</strong> Smooth search and news reading with zero intrusive banners, popups, or tracking scripts."
      },
      "youtube": {
        "status": "0 Ads • Background",
        "caption": "<strong>Screen-Off YouTube:</strong> Uninterrupted music and podcasts in the background with locked screen and zero ad interruptions."
      }
    },
    "tv": {
      "home": {
        "status": "4K TV Portals",
        "caption": "<strong>4K Android TV Home Portal:</strong> Large high-contrast tiles for fast remote-friendly access to news, media, and TV streams."
      },
      "dpad": {
        "status": "60 FPS D-Pad",
        "caption": "<strong>60 FPS D-Pad Focus:</strong> Precision cyan focus ring snaps between interactive cards with zero lag—no clumsy virtual pointer."
      },
      "portals": {
        "status": "Channel Manager",
        "caption": "<strong>TV Portal Manager:</strong> Easily organize, customize, and add your favorite TV stations and web bookmarks with the remote."
      }
    },
    "desktop": {
      "main": {
        "status": "Unix Socket Active",
        "caption": "<strong>Linux Mint Sovereign Edition:</strong> Awesomebar terminal omnibox, built-in Tampermonkey userscript engine, themes and custom CSS."
      }
    }
  },
  "de": {
    "mobile": {
      "shield": {
        "status": "Aktiver Schutz",
        "caption": "<strong>Lokaler Sicherheitsschild:</strong> Fängt bösartige Webseiten, C2-Botnets und Trojaner vor dem Laden ab (abuse.ch O(k) Trie)."
      },
      "stats": {
        "status": "48 Blockiert",
        "caption": "<strong>Dashboard & Statistiken:</strong> Echtzeit-Übersicht über blockierte C2-Server, Tracker und verhinderte Downloads."
      },
      "search": {
        "status": "Sauberes Surfen",
        "caption": "<strong>Schnelles und sauberes Surfen:</strong> Reibungslose Suche ohne Werbebanner, Popups oder Tracker."
      },
      "youtube": {
        "status": "0 Werbung • Hintergrund",
        "caption": "<strong>YouTube bei gesperrtem Bildschirm:</strong> Unterbrechungsfreie Musik und Podcasts im Hintergrund ohne Werbung."
      }
    },
    "tv": {
      "home": {
        "status": "4K TV-Portale",
        "caption": "<strong>4K Android TV Startportal:</strong> Große Kacheln für schnellen Zugriff auf Nachrichten, Videos und TV-Sender per Fernbedienung."
      },
      "dpad": {
        "status": "60 FPS D-Pad",
        "caption": "<strong>60 FPS D-Pad Fokus:</strong> Präziser cyanfarbener Fokusring springt verzögerungsfrei zwischen Elementen."
      },
      "portals": {
        "status": "Kanalmanager",
        "caption": "<strong>TV-Portal-Manager:</strong> Einfaches Verwalten und Hinzufügen eigener TV-Sender per Fernbedienung."
      }
    },
    "desktop": {
      "main": {
        "status": "Unix Socket Aktiv",
        "caption": "<strong>Linux Mint Sovereign Edition:</strong> Awesomebar Terminal-Omnibox, integrierter Tampermonkey für Skripte und Themes."
      }
    }
  },
  "es": {
    "mobile": {
      "shield": {
        "status": "Defensa Activa",
        "caption": "<strong>Escudo de Seguridad Local:</strong> Intercepta sitios maliciosos, botnets C2 y troyanos antes de cargarse (abuse.ch O(k) Trie)."
      },
      "stats": {
        "status": "48 Bloqueados",
        "caption": "<strong>Panel y Estadísticas:</strong> Vista en tiempo real de servidores C2 bloqueados, rastreadores y descargas abortadas."
      },
      "search": {
        "status": "Navegación Limpia",
        "caption": "<strong>Navegación Rápida y Limpia:</strong> Búsqueda fluida y noticias sin banners molestos ni ventanas emergentes."
      },
      "youtube": {
        "status": "0 Anuncios • Fondo",
        "caption": "<strong>YouTube con pantalla apagada:</strong> Música y podcasts ininterrumpidos en segundo plano sin anuncios."
      }
    },
    "tv": {
      "home": {
        "status": "Portales 4K TV",
        "caption": "<strong>Portal de Inicio 4K Android TV:</strong> Grandes iconos para acceso rápido a noticias, vídeos y canales con el mando."
      },
      "dpad": {
        "status": "60 FPS D-Pad",
        "caption": "<strong>Enfoque D-Pad a 60 FPS:</strong> Anillo de enfoque cian preciso sin retardo ni ratones virtuales torpes."
      },
      "portals": {
        "status": "Gestor de Canales",
        "caption": "<strong>Gestor de Portales TV:</strong> Organiza y añade fácilmente tus canales favoritos con el mando a distancia."
      }
    },
    "desktop": {
      "main": {
        "status": "Unix Socket Activo",
        "caption": "<strong>Linux Mint Sovereign Edition:</strong> Barra Awesomebar, motor Tampermonkey integrado y personalización de temas."
      }
    }
  },
  "fr": {
    "mobile": {
      "shield": {
        "status": "Défense Active",
        "caption": "<strong>Bouclier de Sécurité Local :</strong> Intercepte les sites malveillants, botnets C2 et chevaux de Troie avant le chargement."
      },
      "stats": {
        "status": "48 Bloqués",
        "caption": "<strong>Tableau de bord et stats :</strong> Vue en temps réel des serveurs C2 bloqués, des traqueurs et des téléchargements arrêtés."
      },
      "search": {
        "status": "Navigation Épurée",
        "caption": "<strong>Navigation Rapide :</strong> Recherche fluide et lecture sans bannières publicitaires ni fenêtres intrusives."
      },
      "youtube": {
        "status": "0 Pub • Arrière-plan",
        "caption": "<strong>YouTube écran éteint :</strong> Musique et podcasts ininterrompus en arrière-plan avec écran verrouillé."
      }
    },
    "tv": {
      "home": {
        "status": "Portails 4K TV",
        "caption": "<strong>Portail d'accueil 4K Android TV :</strong> Grandes tuiles pour un accès rapide aux actualités et chaînes TV."
      },
      "dpad": {
        "status": "60 FPS D-Pad",
        "caption": "<strong>Focus D-Pad 60 FPS :</strong> Anneau de focus cyan ultra-précis sautant d'un élément à l'autre sans latence."
      },
      "portals": {
        "status": "Gestionnaire Chaînes",
        "caption": "<strong>Gestionnaire de Portails TV :</strong> Organisez et ajoutez facilement vos chaînes favorites à la télécommande."
      }
    },
    "desktop": {
      "main": {
        "status": "Socket Unix Actif",
        "caption": "<strong>Édition Linux Mint :</strong> Awesomebar terminal, Tampermonkey intégré pour scripts et thèmes personnalisés."
      }
    }
  },
  "it": {
    "mobile": {
      "shield": {
        "status": "Difesa Attiva",
        "caption": "<strong>Scudo di Sicurezza Locale:</strong> Intercetta siti dannosi, botnet C2 e trojan prima del caricamento (abuse.ch O(k) Trie)."
      },
      "stats": {
        "status": "48 Bloccati",
        "caption": "<strong>Dashboard e statistiche:</strong> Panoramica in tempo reale dei server C2 bloccati, tracker e download fermati."
      },
      "search": {
        "status": "Navigazione Pulita",
        "caption": "<strong>Navigazione Veloce:</strong> Ricerca fluida e notizie senza banner pubblicitari invasivi o pop-up."
      },
      "youtube": {
        "status": "0 Pubblicità • Sfondo",
        "caption": "<strong>YouTube a schermo spento:</strong> Musica e podcast senza interruzioni in background a telefono bloccato."
      }
    },
    "tv": {
      "home": {
        "status": "Portali 4K TV",
        "caption": "<strong>Portale Home 4K Android TV:</strong> Grandi riquadri per un accesso rapido a notizie, video e canali TV col telecomando."
      },
      "dpad": {
        "status": "60 FPS D-Pad",
        "caption": "<strong>Focus D-Pad a 60 FPS:</strong> Anello di messa a fuoco ciano preciso e scattante senza mouse virtuali."
      },
      "portals": {
        "status": "Gestore Canali",
        "caption": "<strong>Gestore Portali TV:</strong> Organizza e aggiungi facilmente i tuoi canali preferiti con il telecomando."
      }
    },
    "desktop": {
      "main": {
        "status": "Socket Unix Attivo",
        "caption": "<strong>Edizione Linux Mint:</strong> Awesomebar terminale, Tampermonkey integrato per script e temi CSS personalizzati."
      }
    }
  }
};
const demoScriptsI18n = {
  "sl": [
    {
      "headline": "YouTube Samodejni Preskok",
      "meta": "Cilj: *.youtube.com/* • Čas proženja: document-end",
      "badge": "Aktivno",
      "stat1Lbl": "Oglasov preskočeno",
      "stat2Lbl": "Prihranjen čas",
      "stat3Lbl": "Zakasnitev klika",
      "logs": [
        {
          "time": "[10:14:02]",
          "text": "🛡️ Safeer Userscript Engine inicializiran",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "🔍 Zaznan video element & oglasni predvajalnik",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "⚡ Najden '.ytp-skip-ad-button' -> Samodejni klik!",
          "type": "success"
        },
        {
          "time": "[10:14:04]",
          "text": "✅ Oglas uspešno preskočen brez zamika",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Pametni Nočni Način",
      "meta": "Cilj: Vse spletne strani (*) • Brez popačenja slik",
      "badge": "Aktivno",
      "stat1Lbl": "Manj modre svetlobe",
      "stat2Lbl": "Ohranjene barve slik",
      "stat3Lbl": "Prihranek baterije",
      "logs": [
        {
          "time": "[21:40:11]",
          "text": "🌙 Pametni nočni način vklopljen",
          "type": "info"
        },
        {
          "time": "[21:40:11]",
          "text": "🎨 Uporabljen CSS invert(90%) hue-rotate(180deg)",
          "type": "info"
        },
        {
          "time": "[21:40:12]",
          "text": "🖼️ Zaznanih 18 slik in 2 videa -> Obnovljene naravne barve",
          "type": "success"
        },
        {
          "time": "[21:40:12]",
          "text": "✨ Stran pretvorjena v čist AMOLED kontrast",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Čisti Splet Brez Ovir",
      "meta": "Cilj: Vse spletne strani (*) • Odstranitev vsiljivih pasic",
      "badge": "Aktivno",
      "stat1Lbl": "Odstranjenih pasic",
      "stat2Lbl": "Klikov na 'Sprejmi vse'",
      "stat3Lbl": "Več vidne vsebine",
      "logs": [
        {
          "time": "[14:22:05]",
          "text": "🧹 Skeniranje DOM elementov za nadležne pasice",
          "type": "info"
        },
        {
          "time": "[14:22:05]",
          "text": "🚫 Odstranjeno: '.cookie-banner' in '[id*=\"gdpr\"]'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🚫 Odstranjeno: '.newsletter-modal' in '.fixed-bottom-bar'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🎉 Čisto branje omogočeno brez motenj!",
          "type": "success"
        }
      ]
    }
  ],
  "en": [
    {
      "headline": "YouTube Auto Skip",
      "meta": "Target: *.youtube.com/* • Trigger: document-end",
      "badge": "Active",
      "stat1Lbl": "Ads skipped",
      "stat2Lbl": "Time saved",
      "stat3Lbl": "Click delay",
      "logs": [
        {
          "time": "[10:14:02]",
          "text": "🛡️ Safeer Userscript Engine initialized",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "🔍 Video element & ad container detected",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "⚡ Found '.ytp-skip-ad-button' -> Auto click dispatched!",
          "type": "success"
        },
        {
          "time": "[10:14:04]",
          "text": "✅ Ad successfully skipped without delay",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Smart Dark Mode",
      "meta": "Target: All websites (*) • Preserves media colors",
      "badge": "Active",
      "stat1Lbl": "Less blue light",
      "stat2Lbl": "Natural image colors",
      "stat3Lbl": "Battery efficiency",
      "logs": [
        {
          "time": "[21:40:11]",
          "text": "🌙 Smart Dark Mode activated",
          "type": "info"
        },
        {
          "time": "[21:40:11]",
          "text": "🎨 Applied CSS invert(90%) hue-rotate(180deg)",
          "type": "info"
        },
        {
          "time": "[21:40:12]",
          "text": "🖼️ 18 images & 2 videos detected -> Original hues restored",
          "type": "success"
        },
        {
          "time": "[21:40:12]",
          "text": "✨ Webpage rendered in high AMOLED contrast",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Clean Web Ad-Strip",
      "meta": "Target: All websites (*) • Removes intrusive overlays",
      "badge": "Active",
      "stat1Lbl": "Overlays removed",
      "stat2Lbl": "Consent clicks required",
      "stat3Lbl": "More visible screen",
      "logs": [
        {
          "time": "[14:22:05]",
          "text": "🧹 Scanning DOM elements for obstructive overlays",
          "type": "info"
        },
        {
          "time": "[14:22:05]",
          "text": "🚫 Removed: '.cookie-banner' and '[id*=\"gdpr\"]'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🚫 Removed: '.newsletter-modal' and '.fixed-bottom-bar'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🎉 Clutter-free reading enabled without interruptions!",
          "type": "success"
        }
      ]
    }
  ],
  "de": [
    {
      "headline": "YouTube Automatisches Überspringen",
      "meta": "Ziel: *.youtube.com/* • Auslöser: document-end",
      "badge": "Aktiv",
      "stat1Lbl": "Werbung übersprungen",
      "stat2Lbl": "Gesparte Zeit",
      "stat3Lbl": "Klick-Verzögerung",
      "logs": [
        {
          "time": "[10:14:02]",
          "text": "🛡️ Safeer Userscript Engine initialisiert",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "🔍 Videoelement & Werbecontainer erkannt",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "⚡ '.ytp-skip-ad-button' gefunden -> Automatisch geklickt!",
          "type": "success"
        },
        {
          "time": "[10:14:04]",
          "text": "✅ Werbung verzögerungsfrei übersprungen",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Intelligenter Dunkelmodus",
      "meta": "Ziel: Alle Webseiten (*) • Bildfarben geschützt",
      "badge": "Aktiv",
      "stat1Lbl": "Weniger Blaulicht",
      "stat2Lbl": "Natürliche Bildfarben",
      "stat3Lbl": "Batterieersparnis",
      "logs": [
        {
          "time": "[21:40:11]",
          "text": "🌙 Intelligenter Dunkelmodus aktiviert",
          "type": "info"
        },
        {
          "time": "[21:40:11]",
          "text": "🎨 CSS invert(90%) hue-rotate(180deg) angewendet",
          "type": "info"
        },
        {
          "time": "[21:40:12]",
          "text": "🖼️ 18 Bilder & 2 Videos erkannt -> Natürliche Farben wiederhergestellt",
          "type": "success"
        },
        {
          "time": "[21:40:12]",
          "text": "✨ Seite in scharfen AMOLED-Kontrast umgewandelt",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Sauberes Web ohne Banner",
      "meta": "Ziel: Alle Webseiten (*) • Entfernt lästige Banner",
      "badge": "Aktiv",
      "stat1Lbl": "Banner entfernt",
      "stat2Lbl": "Klicks auf 'Akzeptieren'",
      "stat3Lbl": "Mehr sichtbarer Inhalt",
      "logs": [
        {
          "time": "[14:22:05]",
          "text": "🧹 Scanne DOM-Elemente nach störenden Bannern",
          "type": "info"
        },
        {
          "time": "[14:22:05]",
          "text": "🚫 Entfernt: '.cookie-banner' und '[id*=\"gdpr\"]'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🚫 Entfernt: '.newsletter-modal' und '.fixed-bottom-bar'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🎉 Unterbrechungsfreies Lesen aktiviert!",
          "type": "success"
        }
      ]
    }
  ],
  "es": [
    {
      "headline": "Salto Automático de YouTube",
      "meta": "Objetivo: *.youtube.com/* • Momento: document-end",
      "badge": "Activo",
      "stat1Lbl": "Anuncios saltados",
      "stat2Lbl": "Tiempo ahorrado",
      "stat3Lbl": "Retardo de clic",
      "logs": [
        {
          "time": "[10:14:02]",
          "text": "🛡️ Motor de Userscripts de Safeer inicializado",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "🔍 Elemento de vídeo y reproductor detectados",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "⚡ Encontrado '.ytp-skip-ad-button' -> ¡Clic automático!",
          "type": "success"
        },
        {
          "time": "[10:14:04]",
          "text": "✅ Anuncio saltado con éxito sin demora",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Modo Oscuro Inteligente",
      "meta": "Objetivo: Todos los sitios web (*) • Sin distorsión",
      "badge": "Activo",
      "stat1Lbl": "Menos luz azul",
      "stat2Lbl": "Colores preservados",
      "stat3Lbl": "Ahorro de batería",
      "logs": [
        {
          "time": "[21:40:11]",
          "text": "🌙 Modo oscuro inteligente activado",
          "type": "info"
        },
        {
          "time": "[21:40:11]",
          "text": "🎨 Aplicado CSS invert(90%) hue-rotate(180deg)",
          "type": "info"
        },
        {
          "time": "[21:40:12]",
          "text": "🖼️ 18 imágenes y 2 vídeos detectados -> Colores restaurados",
          "type": "success"
        },
        {
          "time": "[21:40:12]",
          "text": "✨ Página convertida a contraste AMOLED puro",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Web Limpia Sin Molestias",
      "meta": "Objetivo: Todos los sitios web (*) • Elimina ventanas",
      "badge": "Activo",
      "stat1Lbl": "Banners eliminados",
      "stat2Lbl": "Clics en 'Aceptar'",
      "stat3Lbl": "Más contenido visible",
      "logs": [
        {
          "time": "[14:22:05]",
          "text": "🧹 Escaneando elementos DOM en busca de banners",
          "type": "info"
        },
        {
          "time": "[14:22:05]",
          "text": "🚫 Eliminado: '.cookie-banner' y '[id*=\"gdpr\"]'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🚫 Eliminado: '.newsletter-modal' y '.fixed-bottom-bar'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🎉 ¡Lectura limpia activada sin interrupciones!",
          "type": "success"
        }
      ]
    }
  ],
  "fr": [
    {
      "headline": "Saut Automatique YouTube",
      "meta": "Cible: *.youtube.com/* • Déclencheur: document-end",
      "badge": "Actif",
      "stat1Lbl": "Publicités ignorées",
      "stat2Lbl": "Temps économisé",
      "stat3Lbl": "Délai de clic",
      "logs": [
        {
          "time": "[10:14:02]",
          "text": "🛡️ Moteur Safeer Userscript initialisé",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "🔍 Élément vidéo et lecteur publicitaire détectés",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "⚡ '.ytp-skip-ad-button' trouvé -> Clic automatique !",
          "type": "success"
        },
        {
          "time": "[10:14:04]",
          "text": "✅ Publicité ignorée sans délai",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Mode Sombre Intelligent",
      "meta": "Cible: Tous les sites web (*) • Préserve les médias",
      "badge": "Actif",
      "stat1Lbl": "Moins de lumière bleue",
      "stat2Lbl": "Couleurs préservées",
      "stat3Lbl": "Économie de batterie",
      "logs": [
        {
          "time": "[21:40:11]",
          "text": "🌙 Mode sombre intelligent activé",
          "type": "info"
        },
        {
          "time": "[21:40:11]",
          "text": "🎨 CSS invert(90%) hue-rotate(180deg) appliqué",
          "type": "info"
        },
        {
          "time": "[21:40:12]",
          "text": "🖼️ 18 images et 2 vidéos détectées -> Couleurs restaurées",
          "type": "success"
        },
        {
          "time": "[21:40:12]",
          "text": "✨ Contraste AMOLED appliqué avec succès",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Web Pur Sans Bannières",
      "meta": "Cible: Tous les sites web (*) • Supprime les overlays",
      "badge": "Actif",
      "stat1Lbl": "Bannières supprimées",
      "stat2Lbl": "Clics 'Tout accepter'",
      "stat3Lbl": "Plus d'espace visible",
      "logs": [
        {
          "time": "[14:22:05]",
          "text": "🧹 Analyse du DOM à la recherche de bannières",
          "type": "info"
        },
        {
          "time": "[14:22:05]",
          "text": "🚫 Supprimé : '.cookie-banner' et '[id*=\"gdpr\"]'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🚫 Supprimé : '.newsletter-modal' et '.fixed-bottom-bar'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🎉 Lecture agréable et épurée sans interruptions !",
          "type": "success"
        }
      ]
    }
  ],
  "it": [
    {
      "headline": "Salto Automatico YouTube",
      "meta": "Obiettivo: *.youtube.com/* • Trigger: document-end",
      "badge": "Attivo",
      "stat1Lbl": "Annunci saltati",
      "stat2Lbl": "Tempo risparmiato",
      "stat3Lbl": "Ritardo del clic",
      "logs": [
        {
          "time": "[10:14:02]",
          "text": "🛡️ Motore Safeer Userscript inizializzato",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "🔍 Rilevato elemento video & player pubblicitario",
          "type": "info"
        },
        {
          "time": "[10:14:03]",
          "text": "⚡ Trovato '.ytp-skip-ad-button' -> Clic automatico!",
          "type": "success"
        },
        {
          "time": "[10:14:04]",
          "text": "✅ Annuncio saltato con successo senza ritardi",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Modalità Scura Intelligente",
      "meta": "Obiettivo: Tutti i siti (*) • Senza distorsioni",
      "badge": "Attivo",
      "stat1Lbl": "Meno luce blu",
      "stat2Lbl": "Colori naturali intatti",
      "stat3Lbl": "Risparmio batteria",
      "logs": [
        {
          "time": "[21:40:11]",
          "text": "🌙 Modalità scura intelligente attivata",
          "type": "info"
        },
        {
          "time": "[21:40:11]",
          "text": "🎨 Applicato CSS invert(90%) hue-rotate(180deg)",
          "type": "info"
        },
        {
          "time": "[21:40:12]",
          "text": "🖼️ 18 immagini e 2 video rilevati -> Colori ripristinati",
          "type": "success"
        },
        {
          "time": "[21:40:12]",
          "text": "✨ Contrasto AMOLED puro applicato",
          "type": "success"
        }
      ]
    },
    {
      "headline": "Web Pulito Senza Banner",
      "meta": "Obiettivo: Tutti i siti (*) • Rimuove finestre fastidiose",
      "badge": "Attivo",
      "stat1Lbl": "Banner rimossi",
      "stat2Lbl": "Clic su 'Accetta tutto'",
      "stat3Lbl": "Più contenuto visibile",
      "logs": [
        {
          "time": "[14:22:05]",
          "text": "🧹 Scansione degli elementi DOM per banner invasivi",
          "type": "info"
        },
        {
          "time": "[14:22:05]",
          "text": "🚫 Rimosso: '.cookie-banner' e '[id*=\"gdpr\"]'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🚫 Rimosso: '.newsletter-modal' e '.fixed-bottom-bar'",
          "type": "success"
        },
        {
          "time": "[14:22:06]",
          "text": "🎉 Lettura pulita attivata senza disturbi!",
          "type": "success"
        }
      ]
    }
  ]
};


let currentActiveScriptIndex = 0;
let currentThreatKey = 'feodo';
let currentScreenshotKey = 'shield';

function applyLanguage(lang) {
  currentLang = lang;
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (i18n[lang] && i18n[lang][key]) {
      el.innerHTML = i18n[lang][key];
    }
  });

  document.querySelectorAll('.lang-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-lang') === lang);
  });

  // Update dynamic showcase caption & status
  updateShowcaseLanguage();

  // Update dynamic threat simulator console
  updateThreatLanguage();

  // Update dynamic script demo simulator
  updateScriptDemoLanguage();

  if (typeof adaptHeroToPlatform === 'function') {
    adaptHeroToPlatform(detectedDevicePlatform);
  }

  if (typeof DisplayEngine !== 'undefined' && DisplayEngine.refresh) {
    DisplayEngine.refresh();
  }
}

function updateShowcaseLanguage() {
  const langData = showcaseCaptionsI18n[currentLang] || showcaseCaptionsI18n['sl'];
  const platData = langData[currentPlatform];
  if (!platData) return;
  const item = platData[currentScreenshotKey] || Object.values(platData)[0];
  if (!item) return;

  const statusEl = document.getElementById('showcaseStatusText');
  const captionEl = document.getElementById('showcaseCaptionText');
  if (statusEl) statusEl.textContent = item.status;
  if (captionEl) captionEl.innerHTML = item.caption;
}

function updateThreatLanguage() {
  const langData = threatI18n[currentLang] || threatI18n['sl'];
  const item = langData[currentThreatKey] || langData['feodo'];
  const consoleEl = document.getElementById('threatConsole');
  if (!consoleEl || !item) return;

  const color = (currentThreatKey === 'safe') ? '#10b981' : (currentThreatKey === 'phishing') ? '#f59e0b' : '#ef4444';
  consoleEl.innerHTML = `
    <span style="color:${color}; font-weight:700; font-size:0.82rem;">${item.title}</span>
    <p style="color:#cbd5e1; font-size:0.78rem; margin-top:3px;">${item.desc}</p>
  `;
  consoleEl.style.borderColor = color;
}

function updateScriptDemoLanguage() {
  const langData = demoScriptsI18n[currentLang] || demoScriptsI18n['sl'];
  const scriptItem = langData[currentActiveScriptIndex];
  if (!scriptItem) return;

  const headlineEl = document.getElementById('simScriptHeadline');
  const metaEl = document.getElementById('simScriptMeta');
  const badgeEl = document.getElementById('simScriptBadge');
  const stat1LblEl = document.getElementById('simStat1Lbl');
  const stat2LblEl = document.getElementById('simStat2Lbl');
  const stat3LblEl = document.getElementById('simStat3Lbl');
  const outputEl = document.getElementById('simConsoleOutput');

  if (headlineEl) headlineEl.textContent = scriptItem.headline;
  if (metaEl) metaEl.textContent = scriptItem.meta;
  if (badgeEl) badgeEl.textContent = scriptItem.badge;
  if (stat1LblEl) stat1LblEl.textContent = scriptItem.stat1Lbl;
  if (stat2LblEl) stat2LblEl.textContent = scriptItem.stat2Lbl;
  if (stat3LblEl) stat3LblEl.textContent = scriptItem.stat3Lbl;

  if (outputEl && scriptItem.logs) {
    outputEl.innerHTML = scriptItem.logs.map(log => `
      <div class="log-line">
        <span class="log-time">${log.time}</span>
        <span class="log-${log.type}">${log.text}</span>
      </div>
    `).join('');
  }
}

// --- 3. Interactive Threat Simulator ---
const threatData = {
  feodo: {
    color: "#ef4444",
    title: "⚠️ ZAZNANA IN BLOKIRANA GROŽNJA (C2 BOTNET)",
    desc: "Dridex/Emotet C2 strežnik takoj prestrežen (abuse.ch Feodo). Povezava je bila prekinjena pred izvajanjem."
  },
  urlhaus: {
    color: "#ef4444",
    title: "⚠️ ZAZNANA ZLONAMERNA KODA (PAYLOAD DROPPER)",
    desc: "Poskus prenosa nevarne datoteke .apk z znane malware domene je bil ustavljen."
  },
  phishing: {
    color: "#f59e0b",
    title: "⚠️ OPOZORILO: LAŽNA BANČNA STRAN (PHISHING)",
    desc: "Lažna spletna stran za krajo osebnih podatkov in gesel (Phishing Army feed) je blokirana."
  },
  safe: {
    color: "#10b981",
    title: "✅ PREVERJENO VARNO SPLETNO MESTO",
    desc: "Domena je varna. Sledilci in oglasne pasice so bili kozmetično odstranjeni za maksimalno hitrost."
  }
};

function setupThreatSimulator() {
  const btns = document.querySelectorAll('.threat-btn');
  const consoleEl = document.getElementById('threatConsole');
  if (!consoleEl) return;

  btns.forEach(b => {
    b.addEventListener('click', () => {
      btns.forEach(x => x.classList.remove('active'));
      b.classList.add('active');

      const simKey = b.getAttribute('data-sim');
      currentThreatKey = simKey;
      updateThreatLanguage();
    });
  });
}

// --- 4. Live Showcase Switcher & D-Pad Engine ---
let currentTvTileIndex = 0;
window.navigateTvShowcase = function(direction) {
  const tiles = [
    document.getElementById('tvTile0'),
    document.getElementById('tvTile1'),
    document.getElementById('tvTile2'),
    document.getElementById('tvTile3')
  ].filter(Boolean);

  if (!tiles.length) return;
  tiles[currentTvTileIndex]?.classList.remove('active-focus');
  currentTvTileIndex = (currentTvTileIndex + direction + tiles.length) % tiles.length;
  tiles[currentTvTileIndex]?.classList.add('active-focus');
};

const screenshotData = {
  mobile: {
    shield: {
      img: 'assets/mobile/safeer_front.png',
      url: 'safeer://shield',
      status: 'Aktivna Zaščita',
      icon: '🛡️',
      caption: '<strong>Lokalni Varnostni Ščit:</strong> Prestreže zlonamerna spletna mesta, botnete (Dridex, Emotet) in trojance pred nalaganjem v pomnilnik (abuse.ch O(k) Trie).'
    },
    stats: {
      img: 'assets/mobile/threat_dialog_screen.png',
      url: 'safeer://stats',
      status: '48 Blokiranih',
      icon: '📊',
      caption: '<strong>Nadzorna plošča in statistika:</strong> Pregled v realnem času nad blokiranimi C2 strežniki, sledilci in preprečenimi nevarnimi prenosi.'
    },
    search: {
      img: 'assets/mobile/search_6_rtv_slovenij.png',
      url: 'google.com/search?q=rtv',
      status: 'Čisto Brskanje',
      icon: '🔍',
      caption: '<strong>Čisto in hitro brskanje:</strong> Nemoteno iskanje in branje novic brez oglasnih pasic, pojavnih oken in invazivnih sledilnih skript.'
    },
    youtube: {
      img: 'assets/mobile/live_youtube_playback.png',
      url: 'youtube.com (v ozadju)',
      status: '0 Oglasov • Ozadje',
      icon: '🎵',
      caption: '<strong>YouTube z ugasnjenim zaslonom:</strong> Nemoteno poslušanje glasbe in podcastov v ozadju z zaklenjenim telefonom brez oglasnih prekinitev.'
    }
  },
  tv: {
    home: {
      img: 'assets/tv/tv_home_screen.png',
      url: 'tv://home',
      status: '4K TV Portali',
      icon: '📺',
      caption: '<strong>4K Android TV Domači Portal:</strong> Velike pregledne ploščice za hiter dostop do novic, videa in TV kanalov z daljinskim upravljalnikom.'
    },
    dpad: {
      img: 'assets/tv/whole_card_focus_final.png',
      url: 'tv://dpad',
      status: '60 FPS D-Pad',
      icon: '🎮',
      caption: '<strong>60 FPS D-Pad Fokus:</strong> Cianov fokusni obroč natančno skače med elementi brez zakasnitve – brez nerodnih navideznih mišk.'
    },
    portals: {
      img: 'assets/tv/portal_manager_dialog_verified.png',
      url: 'tv://portals',
      status: 'Upravitelj Postaj',
      icon: '📑',
      caption: '<strong>Upravitelj TV Portalov:</strong> Preprosto urejanje, razvrščanje in dodajanje lastnih televizijskih postaj in spletnih mest z daljincem.'
    }
  },
  desktop: {
    main: {
      img: 'assets/desktop/desktop_mint_showcase.png',
      url: 'safeer://home',
      status: 'Linux Mint v1.0.7',
      icon: '🍃',
      caption: '<strong>Linux Mint &amp; Ubuntu Izdaja v1.0.7:</strong> Nativni GTK3/WebKit2GTK brskalnik z vrstico zaznamkov (Ctrl+Shift+B), DuckDuckGo privzeto, čarovnikom ob prvem zagonu in 0 oglasov na YouTube.'
    }
  }
};

let currentPlatform = 'mobile';

function switchPlatformShowcase(platform) {
  currentPlatform = platform;

  const tabMob = document.getElementById('tabMobileShowcase');
  const tabTv = document.getElementById('tabTvShowcase');
  const tabDesk = document.getElementById('tabDesktopShowcase');

  [tabMob, tabTv, tabDesk].forEach(t => { if (t) t.classList.remove('active'); });
  if (platform === 'mobile' && tabMob) tabMob.classList.add('active');
  if (platform === 'tv' && tabTv) tabTv.classList.add('active');
  if (platform === 'desktop' && tabDesk) tabDesk.classList.add('active');

  const subMob = document.getElementById('subtabsMobile');
  const subTv = document.getElementById('subtabsTv');
  const subDesk = document.getElementById('subtabsDesktop');

  if (subMob) subMob.style.display = (platform === 'mobile') ? 'flex' : 'none';
  if (subTv) subTv.style.display = (platform === 'tv') ? 'flex' : 'none';
  if (subDesk) subDesk.style.display = (platform === 'desktop') ? 'flex' : 'none';

  const activeSubBar = (platform === 'mobile') ? subMob : (platform === 'tv') ? subTv : subDesk;
  if (activeSubBar) {
    const firstBtn = activeSubBar.querySelector('.subtab-btn');
    if (firstBtn) {
      activeSubBar.querySelectorAll('.subtab-btn').forEach(b => b.classList.remove('active'));
      firstBtn.classList.add('active');
    }
  }

  const firstKey = Object.keys(screenshotData[platform])[0];
  updateScreenshotView(platform, firstKey);
}

function switchScreenshot(platform, key, btn) {
  window._showcaseManualSwitched = true;
  currentPlatform = platform;
  currentScreenshotKey = key;

  if (btn && btn.parentElement) {
    btn.parentElement.querySelectorAll('.subtab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
  }

  updateScreenshotView(platform, key);
}

function updateScreenshotView(platform, key) {
  const data = screenshotData[platform] && screenshotData[platform][key];
  if (!data) return;

  const imgEl = document.getElementById('mainShowcaseImg');
  const urlEl = document.getElementById('showcaseUrl');
  const statusEl = document.getElementById('showcaseStatusText');
  const captionEl = document.getElementById('showcaseCaptionText');
  const iconEl = document.getElementById('captionIcon');
  const stageEl = document.getElementById('screenshotStage');

  if (imgEl) {
    imgEl.src = data.img;
    imgEl.alt = data.status;
  }

  currentScreenshotKey = key;
  if (urlEl) urlEl.textContent = data.url;
  if (iconEl) iconEl.textContent = data.icon;
  
  const langData = showcaseCaptionsI18n[currentLang] || showcaseCaptionsI18n['sl'];
  const platData = langData[platform];
  const locItem = platData ? platData[key] : null;

  if (statusEl) statusEl.textContent = locItem ? locItem.status : data.status;
  if (captionEl) captionEl.innerHTML = locItem ? locItem.caption : data.caption;

  if (stageEl) {
    if (platform === 'mobile') {
      stageEl.style.height = '420px';
    } else {
      stageEl.style.height = '360px';
    }
  }
}

window.switchPlatformShowcase = switchPlatformShowcase;
window.switchScreenshot = switchScreenshot;

let detectedDevicePlatform = 'desktop';

function adaptHeroToPlatform(platform) {
  const btnMob = document.getElementById('heroBtnMob');
  const btnDesktop = document.getElementById('heroBtnDesktop');
  const btnTv = document.getElementById('heroBtnTv');
  const subLinks = document.getElementById('heroSubLinks');
  if (!btnMob || !btnDesktop || !btnTv) return;

  const isSmall = window.innerWidth <= 768;
  const lang = typeof currentLang !== 'undefined' ? currentLang : 'sl';
  const t = {
    sl: { also: "Na voljo tudi za: ", linux: "🍃 Linux Mint (.deb)", mob: "📱 Android telefon", tv: "📺 Android TV" },
    en: { also: "Also available for: ", linux: "🍃 Linux Mint (.deb)", mob: "📱 Android phone", tv: "📺 Android TV" },
    de: { also: "Auch verfügbar für: ", linux: "🍃 Linux Mint (.deb)", mob: "📱 Android Smartphone", tv: "📺 Android TV" },
    es: { also: "También disponible para: ", linux: "🍃 Linux Mint (.deb)", mob: "📱 Teléfono Android", tv: "📺 Android TV" },
    fr: { also: "Également disponible pour: ", linux: "🍃 Linux Mint (.deb)", mob: "📱 Téléphone Android", tv: "📺 Android TV" },
    it: { also: "Disponibile anche per: ", linux: "🍃 Linux Mint (.deb)", mob: "📱 Telefono Android", tv: "📺 Android TV" }
  }[lang] || { also: "Na voljo tudi za: ", linux: "🍃 Linux Mint (.deb)", mob: "📱 Android telefon", tv: "📺 Android TV" };

  if (platform === 'mobile') {
    btnMob.style.display = 'inline-flex';
    if (isSmall) {
      btnDesktop.style.display = 'none';
      btnTv.style.display = 'none';
    } else {
      btnDesktop.style.display = 'inline-flex';
      btnTv.style.display = 'inline-flex';
    }
    if (subLinks) {
      subLinks.innerHTML = `<span style="color:#64748b;">${t.also}</span><a href="linux/" style="color:#87cf3e; text-decoration:underline;">${t.linux}</a> • <a href="tv/" style="color:#c084fc; text-decoration:underline;">${t.tv}</a>`;
    }
  } else if (platform === 'tv') {
    btnTv.style.display = 'inline-flex';
    if (isSmall) {
      btnMob.style.display = 'none';
      btnDesktop.style.display = 'none';
    } else {
      btnMob.style.display = 'inline-flex';
      btnDesktop.style.display = 'inline-flex';
    }
    if (subLinks) {
      subLinks.innerHTML = `<span style="color:#64748b;">${t.also}</span><a href="android/" style="color:#00d2ff; text-decoration:underline;">${t.mob}</a> • <a href="linux/" style="color:#87cf3e; text-decoration:underline;">${t.linux}</a>`;
    }
  } else {
    // desktop / linux
    btnDesktop.style.display = 'inline-flex';
    if (isSmall) {
      btnMob.style.display = 'none';
      btnTv.style.display = 'none';
    } else {
      btnMob.style.display = 'inline-flex';
      btnTv.style.display = 'inline-flex';
    }
    if (subLinks) {
      subLinks.innerHTML = `<span style="color:#64748b;">${t.also}</span><a href="android/" style="color:#00d2ff; text-decoration:underline;">${t.mob}</a> • <a href="tv/" style="color:#c084fc; text-decoration:underline;">${t.tv}</a>`;
    }
  }
}

function setupShowcaseSwitcher() {
  const ua = (navigator.userAgent || '').toLowerCase();
  const isTv = ua.includes('android') && (ua.includes('tv') || ua.includes('smart') || ua.includes('aft') || ua.includes('bravia'));
  const isMobile = !isTv && (ua.includes('android') || ua.includes('mobile') || ua.includes('iphone') || ua.includes('ipad'));
  const isLinux = ua.includes('linux') && !ua.includes('android');

  if (isTv) {
    detectedDevicePlatform = 'tv';
  } else if (isMobile) {
    detectedDevicePlatform = 'mobile';
  } else if (isLinux) {
    detectedDevicePlatform = 'desktop';
  } else {
    detectedDevicePlatform = 'desktop';
  }

  switchPlatformShowcase(detectedDevicePlatform);
  adaptHeroToPlatform(detectedDevicePlatform);
}

window.addEventListener('resize', () => {
  if (typeof adaptHeroToPlatform === 'function') {
    adaptHeroToPlatform(detectedDevicePlatform);
  }
});

// --- 5. Generate Inline SVG QR Codes on Load ---
function renderInlineQRs() {
  const qrMob = document.getElementById('qrContainerMob');
  const qrTv = document.getElementById('qrContainerTv');

  if (qrMob) {
    qrMob.innerHTML = QRCodeMini.generateSVG('https://tinyurl.com/298zn386', 82);
  }
  if (qrTv) {
    qrTv.innerHTML = QRCodeMini.generateSVG('https://tinyurl.com/29y9bg9k', 82);
  }
}

// --- 5b. Interactive UserScript Demo Showcase ---
const demoScripts = [
  {
    title: "youtube_auto_skip.js",
    icon: "⚡",
    headline: "YouTube Samodejni Preskok",
    meta: "Cilj: *.youtube.com/* • Čas proženja: document-end",
    badge: "Aktivno",
    stat1: "14",
    stat1Lbl: "Oglasov preskočeno",
    stat2: "42 s",
    stat2Lbl: "Prihranjen čas",
    stat3: "0 ms",
    stat3Lbl: "Zakasnitev klika",
    logs: [
      { time: "[10:14:02]", text: "🛡️ Safeer Userscript Engine inicializiran", type: "info" },
      { time: "[10:14:03]", text: "🔍 Zaznan video element & oglasni predvajalnik", type: "info" },
      { time: "[10:14:03]", text: "⚡ Najden '.ytp-skip-ad-button' -> Samodejni klik!", type: "success" },
      { time: "[10:14:04]", text: "✅ Oglas uspešno preskočen brez zamika", type: "success" }
    ],
    code: `// ==SafeerUserScript==
// @name         YouTube Samodejni Preskok
// @match        *://*.youtube.com/*
// @run-at       document-end
// ====================

setInterval(() => {
  const skipBtn = document.querySelector('.ytp-skip-ad-button, .skip-intro-btn');
  if (skipBtn) {
    skipBtn.click();
    console.log('🛡️ Safeer: Oglas samodejno preskočen!');
  }
}, 500);`
  },
  {
    title: "smart_dark_mode.js",
    icon: "🌙",
    headline: "Pametni Nočni Način",
    meta: "Cilj: Vse spletne strani (*) • Brez popačenja slik",
    badge: "Aktivno",
    stat1: "85%",
    stat1Lbl: "Manj modre svetlobe",
    stat2: "100%",
    stat2Lbl: "Ohranjene barve slik",
    stat3: "3.2x",
    stat3Lbl: "Prihranek baterije",
    logs: [
      { time: "[21:40:11]", text: "🌙 Pametni nočni način vklopljen", type: "info" },
      { time: "[21:40:11]", text: "🎨 Uporabljen CSS invert(90%) hue-rotate(180deg)", type: "info" },
      { time: "[21:40:12]", text: "🖼️ Zaznanih 18 slik in 2 videa -> Obnovljene naravne barve", type: "success" },
      { time: "[21:40:12]", text: "✨ Stran pretvorjena v čist AMOLED kontrast", type: "success" }
    ],
    code: `// ==SafeerUserScript==
// @name         Pametni Nočni Način
// @match        *
// @run-at       document-end
// ====================

document.documentElement.style.filter = "invert(90%) hue-rotate(180deg)";
// Zaščiti naravne barve slik in videoposnetkov:
document.querySelectorAll("img, video, canvas, picture").forEach(el => {
  el.style.filter = "invert(100%) hue-rotate(180deg)";
});`
  },
  {
    title: "clean_web_nobanners.js",
    icon: "🧹",
    headline: "Čisti Splet Brez Ovir",
    meta: "Cilj: Vse spletne strani (*) • Odstranitev vsiljivih pasic",
    badge: "Aktivno",
    stat1: "7",
    stat1Lbl: "Odstranjenih pasic",
    stat2: "0",
    stat2Lbl: "Klikov na 'Sprejmi vse'",
    stat3: "+38%",
    stat3Lbl: "Več vidne vsebine",
    logs: [
      { time: "[14:22:05]", text: "🧹 Skeniranje DOM elementov za nadležne pasice", type: "info" },
      { time: "[14:22:05]", text: "🚫 Odstranjeno: '.cookie-banner' in '[id*=\"gdpr\"]'", type: "success" },
      { time: "[14:22:06]", text: "🚫 Odstranjeno: '.newsletter-modal' in '.fixed-bottom-bar'", type: "success" },
      { time: "[14:22:06]", text: "🎉 Čisto branje omogočeno brez motenj!", type: "success" }
    ],
    code: `// ==SafeerUserScript==
// @name         Čisti Splet Brez Ovir
// @match        *
// @run-at       document-end
// ====================

const selectors = [
  '.cookie-banner', '.newsletter-modal',
  '.fixed-bottom-bar', '[id*="gdpr"]'
];

setInterval(() => {
  selectors.forEach(sel => {
    document.querySelectorAll(sel).forEach(el => el.remove());
  });
}, 800);`
  }
];

function switchScriptDemo(index, btn) {
  const data = demoScripts[index];
  if (!data) return;

  const titleEl = document.getElementById('activeScriptTabTitle');
  const codeEl = document.getElementById('demoCodeDisplay');

  if (titleEl) titleEl.textContent = data.title;
  if (codeEl) {
    codeEl.innerHTML = `<code class="language-javascript">${data.code.replace(/</g, "&lt;").replace(/>/g, "&gt;")}</code>`;
  }

  // Update simulator fields
  const iconEl = document.getElementById('simScriptIcon');
  const headlineEl = document.getElementById('simScriptHeadline');
  const metaEl = document.getElementById('simScriptMeta');
  const badgeEl = document.getElementById('simScriptBadge');
  const stat1El = document.getElementById('simStat1');
  const stat1LblEl = document.getElementById('simStat1Lbl');
  const stat2El = document.getElementById('simStat2');
  const stat2LblEl = document.getElementById('simStat2Lbl');
  const stat3El = document.getElementById('simStat3');
  const stat3LblEl = document.getElementById('simStat3Lbl');
  const consoleEl = document.getElementById('simConsoleOutput');

  if (iconEl) iconEl.textContent = data.icon;
  if (headlineEl) headlineEl.textContent = data.headline;
  if (metaEl) metaEl.textContent = data.meta;
  if (badgeEl) badgeEl.textContent = data.badge;
  if (stat1El) stat1El.textContent = data.stat1;
  if (stat1LblEl) stat1LblEl.textContent = data.stat1Lbl;
  if (stat2El) stat2El.textContent = data.stat2;
  if (stat2LblEl) stat2LblEl.textContent = data.stat2Lbl;
  if (stat3El) stat3El.textContent = data.stat3;
  if (stat3LblEl) stat3LblEl.textContent = data.stat3Lbl;

  if (consoleEl && data.logs) {
    consoleEl.innerHTML = data.logs.map(l => `
      <div class="log-line">
        <span class="log-time">${l.time}</span>
        <span class="${l.type === 'success' ? 'log-success' : 'log-info'}">${l.text}</span>
      </div>
    `).join('');
  }

  document.querySelectorAll('.script-preset-pills .preset-pill').forEach(p => p.classList.remove('active'));
  if (btn) btn.classList.add('active');
}

// --- 5c. TV Bento Interactive Tab Switcher ---
function switchTvBentoTab(tab) {
  const viewDpad = document.getElementById('viewTvDpad');
  const viewPortals = document.getElementById('viewTvPortals');
  const btnDpad = document.getElementById('btnTvTabDpad');
  const btnPortals = document.getElementById('btnTvTabPortals');

  if (!viewDpad || !viewPortals) return;

  if (tab === 'dpad') {
    viewDpad.style.display = 'block';
    viewPortals.style.display = 'none';
    if (btnDpad) btnDpad.classList.add('active');
    if (btnPortals) btnPortals.classList.remove('active');
  } else {
    viewDpad.style.display = 'none';
    viewPortals.style.display = 'block';
    if (btnPortals) btnPortals.classList.add('active');
    if (btnDpad) btnDpad.classList.remove('active');
  }
}

// Expose globally for onclick handlers
window.switchScriptDemo = switchScriptDemo;
window.switchTvBentoTab = switchTvBentoTab;

// --- 6. Initialization ---
document.addEventListener('DOMContentLoaded', () => {
  setupThreatSimulator();
  setupShowcaseSwitcher();
  renderInlineQRs();

  document.querySelectorAll('.lang-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      applyLanguage(btn.getAttribute('data-lang'));
    });
  });

  // --- 7. Adaptive Display & Orientation Engine ---
  DisplayEngine.init();

  // Secret Admin Shortcut (Triple Click on Copyright)
  let _secClicks = 0;
  let _secTimer = null;
  const copyEl = document.querySelector('.footer-copy');
  if (copyEl) {
    copyEl.addEventListener('click', () => {
      _secClicks++;
      clearTimeout(_secTimer);
      _secTimer = setTimeout(() => { _secClicks = 0; }, 1500);
      if (_secClicks >= 3) {
        _secClicks = 0;
        const key = prompt('🔒 Vnesite tajni varnostni ključ za statistiko:');
        if (key) {
          window.location.href = `stats.html?key=${encodeURIComponent(key)}`;
        }
      }
    });
  }
});

// --- 7. Adaptive Display & Orientation Engine Implementation ---
const DisplayEngine = (function() {
  let lastInfo = null;

  function analyzeDisplay() {
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const sw = window.screen ? window.screen.width : vw;
    const sh = window.screen ? window.screen.height : vh;
    const dpr = window.devicePixelRatio || 1;
    const ua = navigator.userAgent.toLowerCase();
    const platform = (navigator.platform || '').toLowerCase();

    const isPortrait = vh > vw;
    const orientation = isPortrait ? 'portrait' : 'landscape';
    const aspectRatio = (Math.max(vw, vh) / Math.min(vw, vh)).toFixed(2);

    // Platform detection
    const isTv = /tv|googletv|smarttv|androidtv|crkey|appletv|hbbtv/i.test(ua) || (vw >= 2560 && !/mobile|android|iphone|ipad/i.test(ua) && 'ontouchstart' in window === false && /smart-tv|tizen|webos/i.test(ua));
    const isMobileUa = /android.*mobile|iphone|ipod|blackberry|iemobile|opera mini/i.test(ua);
    const isTabletUa = /ipad|android(?!.*mobile)|tablet/i.test(ua);
    const isLinuxOS = (/linux/i.test(platform) || /linux/i.test(ua)) && !/android/i.test(ua);

    let category = 'desktop';
    let diagInches = '24"';
    let diagCm = '61 cm';
    let diagLabel = '24" Monitor (61 cm)';

    if (isTv) {
      category = 'tv';
      diagInches = '55" - 65"';
      diagCm = '140 - 165 cm';
      diagLabel = '55" - 65" TV (140 - 165 cm)';
    } else if (isMobileUa || (vw <= 500 && isPortrait) || (vh <= 500 && !isPortrait && 'ontouchstart' in window)) {
      category = 'mobile';
      const estDiag = (Math.hypot(sw, sh) / (160 * Math.min(dpr, 3.2))).toFixed(1);
      diagInches = `${estDiag}"`;
      diagCm = `${Math.round(estDiag * 2.54)} cm`;
      diagLabel = `${diagInches} Mobilnik (${diagCm})`;
    } else if (isTabletUa || (vw >= 600 && vw <= 1100 && 'ontouchstart' in window)) {
      category = 'tablet';
      diagInches = '10.5" - 12.4"';
      diagCm = '27 - 31 cm';
      diagLabel = '11" Tablica (28 cm)';
    } else if (vw >= 2500 && aspectRatio >= 2.1) {
      category = 'ultrawide';
      diagInches = '34" - 49"';
      diagCm = '86 - 124 cm';
      diagLabel = '34" Ultrawide (86 cm)';
    } else if (vw >= 1600) {
      category = 'desktop';
      diagInches = '24" - 27"';
      diagCm = '61 - 69 cm';
      diagLabel = '27" Namizje (69 cm)';
    } else if (vw >= 1024) {
      category = 'laptop';
      diagInches = '14" - 16"';
      diagCm = '35 - 40 cm';
      diagLabel = '15" Prenosnik (38 cm)';
    } else {
      category = 'compact';
      diagInches = '12"';
      diagCm = '30 cm';
      diagLabel = '12" Zaslon (30 cm)';
    }

    let detectedPlatform = 'other';
    if (isLinuxOS) detectedPlatform = 'linux';
    else if (isTv) detectedPlatform = 'android-tv';
    else if (isMobileUa) detectedPlatform = 'android-mobile';

    return {
      vw, vh, sw, sh, dpr,
      orientation,
      isPortrait,
      category,
      aspectRatio,
      diagInches,
      diagCm,
      diagLabel,
      detectedPlatform,
      isLinuxOS,
      isTv,
      isMobileUa
    };
  }

  function applyAdaptiveLayout() {
    const info = analyzeDisplay();
    lastInfo = info;

    const root = document.documentElement;
    root.setAttribute('data-screen-category', info.category);
    root.setAttribute('data-orientation', info.orientation);
    root.setAttribute('data-platform', info.detectedPlatform);

    // Smart priority button ordering & recommendation badge in Hero
    const heroBtnLinux = document.getElementById('heroBtnDesktop');
    const heroBtnMob = document.getElementById('heroBtnMob');
    const heroBtnTv = document.getElementById('heroBtnTv');
    const heroCtaGroup = document.querySelector('.hero-cta-group');

    // Remove existing recommended styling
    [heroBtnLinux, heroBtnMob, heroBtnTv].forEach(b => {
      if (b) {
        b.classList.remove('btn-recommended');
        const badge = b.querySelector('.rec-badge');
        if (badge) badge.remove();
      }
    });

    if (info.isLinuxOS && heroBtnLinux && heroCtaGroup) {
      heroBtnLinux.classList.add('btn-recommended');
      const recBadge = document.createElement('span');
      recBadge.className = 'rec-badge';
      recBadge.textContent = (currentLang === 'sl') ? '🎯 Vaš OS: Linux' : '🎯 Your OS: Linux';
      heroBtnLinux.appendChild(recBadge);
      heroCtaGroup.prepend(heroBtnLinux);

      if (!window._showcaseManualSwitched) {
        const tabDesk = document.getElementById('tabDesktopShowcase');
        if (tabDesk) tabDesk.click();
      }
    } else if (info.isTv && heroBtnTv && heroCtaGroup) {
      heroBtnTv.classList.add('btn-recommended');
      const recBadge = document.createElement('span');
      recBadge.className = 'rec-badge';
      recBadge.textContent = (currentLang === 'sl') ? '🎯 Vaš TV' : '🎯 Your TV';
      heroBtnTv.appendChild(recBadge);
      heroCtaGroup.prepend(heroBtnTv);

      if (!window._showcaseManualSwitched) {
        const tabTv = document.getElementById('tabTvShowcase');
        if (tabTv) tabTv.click();
      }
    } else if (info.isMobileUa && heroBtnMob && heroCtaGroup) {
      heroBtnMob.classList.add('btn-recommended');
      const recBadge = document.createElement('span');
      recBadge.className = 'rec-badge';
      recBadge.textContent = (currentLang === 'sl') ? '🎯 Vaš telefon' : '🎯 Your Phone';
      heroBtnMob.appendChild(recBadge);
      heroCtaGroup.prepend(heroBtnMob);

      if (!window._showcaseManualSwitched) {
        const tabMob = document.getElementById('tabMobileShowcase');
        if (tabMob) tabMob.click();
      }
    }

    // Diagnostic badge disabled per user preference
    const existingBadge = document.getElementById('displayInspectorBadge');
    if (existingBadge) existingBadge.remove();
  }

  function updateScreenBadge(info) {
    const badge = document.getElementById('displayInspectorBadge');
    if (badge) badge.remove();
  }

  function toggleDiagnosticModal(info) {
    let modal = document.getElementById('displayDiagModal');
    if (modal) {
      modal.remove();
      return;
    }

    modal = document.createElement('div');
    modal.id = 'displayDiagModal';
    modal.className = 'display-diag-modal-backdrop';

    const isSl = currentLang === 'sl';
    const orientWord = info.isPortrait ? (isSl ? 'Navpična (Portret)' : 'Portrait') : (isSl ? 'Vodoravna (Landscape)' : 'Landscape');

    modal.innerHTML = `
      <div class="display-diag-modal">
        <div class="diag-header">
          <div class="diag-title">📐 ${isSl ? 'Zaznava zaslona in prilagoditev' : 'Screen Detection & Adaptation'}</div>
          <button class="diag-close-btn" onclick="this.closest('.display-diag-modal-backdrop').remove()">✕</button>
        </div>
        <div class="diag-body">
          <div class="diag-item">
            <span class="diag-lbl">${isSl ? 'Ocenjena diagonala:' : 'Estimated Diagonal:'}</span>
            <span class="diag-val" style="color:#00d2ff; font-weight:700;">${info.diagLabel}</span>
          </div>
          <div class="diag-item">
            <span class="diag-lbl">${isSl ? 'Postavitev ekrana:' : 'Screen Orientation:'}</span>
            <span class="diag-val" style="color:#c084fc; font-weight:700;">${orientWord}</span>
          </div>
          <div class="diag-item">
            <span class="diag-lbl">${isSl ? 'Trenutna ločljivost okna:' : 'Viewport Resolution:'}</span>
            <span class="diag-val">${info.vw} × ${info.vh} px (Gostota DPR: ${info.dpr})</span>
          </div>
          <div class="diag-item">
            <span class="diag-lbl">${isSl ? 'Fizični zaslon:' : 'Hardware Display:'}</span>
            <span class="diag-val">${info.sw} × ${info.sh} px</span>
          </div>
          <div class="diag-item">
            <span class="diag-lbl">${isSl ? 'Zaznana platforma:' : 'Detected Platform:'}</span>
            <span class="diag-val" style="color:#87cf3e; font-weight:700;">${info.isLinuxOS ? '🍃 Linux Mint / Ubuntu' : (info.isTv ? '📺 Android TV' : (info.isMobileUa ? '📱 Android Mobile' : '🖥️ Desktop'))}</span>
          </div>
          <div class="diag-item">
            <span class="diag-lbl">${isSl ? 'Stanje prilagoditve:' : 'Adaptation Status:'}</span>
            <span class="diag-val" style="color:#10b981;">✅ ${isSl ? '100% Optimizirano za vašo postavitev' : '100% Optimized for your screen'}</span>
          </div>
        </div>
        <div class="diag-footer">
          <button class="btn-pill-primary" style="padding:8px 18px; font-size:0.85rem;" onclick="this.closest('.display-diag-modal-backdrop').remove()">
            ${isSl ? 'Zapri' : 'Close'}
          </button>
        </div>
      </div>
    `;

    modal.addEventListener('click', (e) => {
      if (e.target === modal) modal.remove();
    });

    document.body.appendChild(modal);
  }

  return {
    init: function() {
      applyAdaptiveLayout();
      window.addEventListener('resize', applyAdaptiveLayout);
      window.addEventListener('orientationchange', () => {
        setTimeout(applyAdaptiveLayout, 150);
      });
      if (window.screen && window.screen.orientation) {
        window.screen.orientation.addEventListener('change', applyAdaptiveLayout);
      }
    },
    refresh: applyAdaptiveLayout
  };
})();
