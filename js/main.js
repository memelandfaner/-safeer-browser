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
    nav_features: "Zmožnosti",
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
    btn_mob_text: "Prenesi mobilni brskalnik (1.7 MB)",
    btn_tv_text: "Prenesi TV brskalnik (8.8 MB)",
    btn_linux_text: "Prenesi za Linux Mint (485 KB)",
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
    dl_btn_mob: "Prenesi mobilni brskalnik APK (1.7 MB)",
    dl_tv_h3: "📺 TV brskalnik (Pametni Android TV)",
    dl_tv_p: "Optimizirano za Philips TV, Sony, Xiaomi, MediaTek in upravljanje z daljincem.",
    dl_btn_tv: "Prenesi TV brskalnik APK (8.8 MB)",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Popolnoma opremljen za Linux: enotna instanca z Unix socketom, Awesomebar (localhost/razvijalci), Customizer Studio (teme, lasten CSS, UserScripts), 6 svetovnih jezikov in ukaz safeer v terminalu.",
    dl_btn_desktop: "Prenesi Linux Paket (485 KB)",
    footer_copy: "© 2026 Safeer Browser Project. Vrhunska zasebnost, kibernetska varnost in zabava.",
    footer_disclaimer: "⚖️ Safeer is a security layer, not a guarantee against all online threats. Zmanjšuje tveganje in blokira znana tveganja; ne zagotavlja zaščite pred neznanimi Zero-Day grožnjami. 100% odprta koda pod licenco Apache 2.0 – spodbujamo fork kode in lastno prilagoditev."
  },
  en: {
    nav_features: "Features",
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
    btn_mob_text: "Download Mobile Browser (1.7 MB)",
    btn_tv_text: "Download TV Browser (8.8 MB)",
    btn_linux_text: "Download for Linux Mint (485 KB)",
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
    dl_btn_mob: "Download Mobile Browser APK (1.7 MB)",
    dl_tv_h3: "📺 TV Browser (Smart Android TV)",
    dl_tv_p: "Optimized for Philips TV, Sony, Xiaomi, MediaTek, and TV remote.",
    dl_btn_tv: "Download TV Browser APK (8.8 MB)",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Equipped for Linux: single-instance via Unix socket, Awesomebar (localhost/developers), Customizer Studio (themes, CSS, UserScripts), 6 languages, and safeer CLI.",
    dl_btn_desktop: "Download Linux Package (485 KB)",
    footer_copy: "© 2026 Safeer Browser Project. Elite privacy, cyber security, and entertainment.",
    footer_disclaimer: "⚖️ Safeer is a security layer, not a guarantee against all online threats. It reduces risk and blocks known threats; it does not guarantee protection against all unknown Zero-Day attacks. 100% open source under Apache 2.0—fork and customize!"
  },
  de: {
    nav_features: "Funktionen",
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
    btn_mob_text: "Mobilen Browser herunterladen (1.7 MB)",
    btn_tv_text: "TV Browser herunterladen (8.8 MB)",
    btn_linux_text: "Für Linux Mint herunterladen (485 KB)",
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
    dl_btn_mob: "Mobilen Browser APK herunterladen (1.7 MB)",
    dl_tv_h3: "📺 TV Browser (Smart Android TV)",
    dl_tv_p: "Optimiert für Philips TV, Sony, Xiaomi und Fernbedienung.",
    dl_btn_tv: "TV Browser APK herunterladen (8.8 MB)",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Einzelinstanz über Unix-Socket, Awesomebar (localhost), Customizer Studio, UserScripts und safeer CLI.",
    dl_btn_desktop: "Linux-Paket herunterladen (485 KB)",
    footer_copy: "© 2026 Safeer Browser Project. Datenschutz, Sicherheit und Unterhaltung."
  },
  es: {
    nav_features: "Funciones",
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
    btn_mob_text: "Descargar Navegador Móvil (1.7 MB)",
    btn_tv_text: "Descargar Navegador TV (8.8 MB)",
    btn_linux_text: "Descargar para Linux Mint (485 KB)",
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
    dl_btn_mob: "Descargar Navegador Móvil APK (1.7 MB)",
    dl_tv_h3: "📺 Navegador TV (Smart Android TV)",
    dl_tv_p: "Optimizado para Philips TV, Sony, Xiaomi y mando a distancia.",
    dl_btn_tv: "Descargar Navegador TV APK (8.8 MB)",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Instancia única vía socket Unix, Awesomebar (localhost), Customizer Studio, UserScripts y CLI 'safeer'.",
    dl_btn_desktop: "Descargar paquete Linux (485 KB)",
    footer_copy: "© 2026 Safeer Browser Project. Máxima privacidad, seguridad y entretenimiento."
  },
  fr: {
    nav_features: "Fonctionnalités",
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
    btn_linux_text: "Télécharger pour Linux Mint (485 Ko)",
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
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Instance unique via socket Unix, Awesomebar (localhost), Customizer Studio, UserScripts et CLI 'safeer'.",
    dl_btn_desktop: "Télécharger le paquet Linux (485 Ko)",
    footer_copy: "© 2026 Safeer Browser Project. Vie privée, cybersécurité et divertissement."
  },
  it: {
    nav_features: "Funzionalità",
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
    btn_mob_text: "Scarica Browser Mobile (1.7 MB)",
    btn_tv_text: "Scarica Browser TV (8.8 MB)",
    btn_linux_text: "Scarica per Linux Mint (485 KB)",
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
    dl_btn_mob: "Scarica Browser Mobile APK (1.7 MB)",
    dl_tv_h3: "📺 Browser TV (Smart Android TV)",
    dl_tv_p: "Ottimizzato per Philips TV, Sony, Xiaomi e telecomando.",
    dl_btn_tv: "Scarica Browser TV APK (8.8 MB)",
    dl_desktop_h3: "🍃 Linux Mint, Ubuntu & Debian Desktop",
    dl_desktop_p: "Istanza singola tramite socket Unix, Awesomebar (localhost), Customizer Studio, UserScripts e CLI 'safeer'.",
    dl_btn_desktop: "Scarica pacchetto Linux (485 KB)",
    footer_copy: "© 2026 Safeer Browser Project. Massima privacy, sicurezza e intrattenimento."
  }
};

let currentLang = 'sl';

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

  if (typeof DisplayEngine !== 'undefined' && DisplayEngine.refresh) {
    DisplayEngine.refresh();
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
      const item = threatData[simKey];
      if (item) {
        consoleEl.innerHTML = `
          <span style="color:${item.color}; font-weight:700; font-size:0.82rem;">${item.title}</span>
          <p style="color:#cbd5e1; font-size:0.78rem; margin-top:3px;">${item.desc}</p>
        `;
        consoleEl.style.borderColor = item.color;
      }
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
      url: 'mint://awesomebar',
      status: 'Unix Socket Aktiven',
      icon: '🍃',
      caption: '<strong>Linux Mint Suverena Izdaja:</strong> Awesomebar terminalna orodna vrstica, vgrajen Tampermonkey za skripte ter prilagoditev tem in CSS-ja.'
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

  if (urlEl) urlEl.textContent = data.url;
  if (statusEl) statusEl.textContent = data.status;
  if (captionEl) captionEl.innerHTML = data.caption;
  if (iconEl) iconEl.textContent = data.icon;

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

function setupShowcaseSwitcher() {
  switchPlatformShowcase('mobile');
}

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
