# 🛡️ Safeer Browser (Mobile Security Edition)

### **Safeer Browser v1.0.1 — Stable Release**
*Open-source Android browser with local malware, phishing, C2 and tracker protection.*

> ⚠️ **Safeer is a security layer, not a guarantee against all online threats.**  
> Safeer zmanjšuje tveganje in blokira znane grožnje; ne zagotavlja zaščite pred vsemi novimi ali neznanimi grožnjami (Zero-Day).

**Safeer Browser** je sodoben odprtokodni mobilni spletni brskalnik za Android z napredno večslojno zaščito, zasnovan za bistveno zmanjšanje izpostavljenosti znanim Botnet C2 strežnikom, zlonamerni programski opremi (Malware), spletnemu ribarjenju (Phishing) ter agresivnim sledilnim in oglasnim mrežam.

---

## 🌟 Odprta Koda, Navdih in Spodbuda k Lastnemu Razvoju (Fork & Customize)

> **Kdor obvladuje brskalnik, določa pravila spleta.**  
> Milijon uporabnikov ima milijon različnih potreb, okusov in prioritet. Safeer je 100 % odprtokoden projekt pod licenco [Apache 2.0](LICENSE) prav zato, da služi kot odprta platforma in navdih za skupnost.  
>  
> 👉 **Vabljeni k ustvarjanju lastnih vejic (Fork)!**  
> Vzemite izvorno kodo v svoje roke, prilagodite varnostne sezname, spremenite grafično podobo, dodajte lastne bližnjice ali preizkusite nove eksperimentalne funkcionalnosti. Internet je boljši, ko ima vsakdo možnost ustvariti brskalnik po svojih lastnih željah in potrebah.

---

## 🛑 Ključne Varnostne in Zasebnostne Značilnosti

### 1. 🛡️ Večslojni Threat Shield z Atomsko Zamenjavo (Atomic Trie Swap)
- **Viri groženj v živo (Live HTTPS Feeds)**:
  - **abuse.ch ThreatFox IOC**: Zaznava in blokada C2 (Command & Control) botnet strežnikov in indikatorjev napada.
  - **abuse.ch URLhaus**: Blokada domen in gostiteljev za razširjanje zlonamerne kode (Malware distribution).
  - **Phishing Army Extended**: Zaščita pred lažnim predstavljanjem in poskusi kraje osebnih podatkov.
- **Semenska baza (Seed Database)**:
  - Vgrajeni indikatorji napadov (**abuse.ch ThreatFox IOC**) in zlonamerna omrežja (**StevenBlack Unified**).
- **🔄 Brezprekinitvena Atomska Zamenjava (Atomic Swap)**:
  - Ob posodobitvi seznamov v ozadju se zgradi novo drevo groženj in se atomsko zamenja (`swapThreatTrie`), s čimer se prepreči ohranjanje zastarelih lažnih zaznav.
- **🔒 Zero-Bypass Pravilo**:
  - Za nevarne C2/malware domene **ne veljajo nobene video ali embed izjeme**.
- **🔑 Enokratni Kriptografski Žetoni za Obvoz (One-Time Token Interstitial)**:
  - Ob poskusu obiska nevarne domene se prikaže opozorilni zaslon. Morebiten obvoz na lastno odgovornost (`safeer://bypass-threat`) je zaščiten z naključnim enokratnim žetonom (UUID), vezanim na točno določeno domeno in časovno veljavnost (5 min). Zunanje spletne strani ne morejo sprožiti neavtoriziranega odklepa.
- **📝 Kriptografska Verifikacija & SHA-256 Revizija Feedov**:
  - Ob vsakem prenosu varnostnih seznamov brskalnik preveri strukturne markerje avtentičnosti izdajatelja (`abuse.ch`, `Phishing Army`), meje veljavne velikosti ter prag minimalnega števila pravil. Preprečena je uveljavitev neveljavnih podatkov ali prestreženih HTML portalov. Izračunana kontrolna vsota SHA-256 se zabeleži v varnostni revizijski dnevnik in shrani v varovano lokalno shrambo.

### 2. 🛡️ Stroga Zaščita Zasebnosti in Dovoljenj (Zero Auto-Grant)
- **Brezkompromisna zapora mešanih vsebin (`MIXED_CONTENT_NEVER_ALLOW`)**:
  - Dosledno upoštevanje priporočil Android Security: brskalnik popolnoma prepoveduje nalaganje nezaščitenih HTTP elementov na HTTPS povezavah.
- **Izolacija Content Providerjev (`allowContentAccess = false`)**:
  - Popolnoma onemogočen dostop do `content://` shem prek spletnih vsebin, kar odpravlja vektorje napadov na sistemske ponudnike podatkov.
- **Pripravljenost na Android 16 (Target SDK 36)**:
  - Polna usklajenost z najnovejšimi standardi Android varnostnega modela.
- **Interaktivna privolitev za strojne vire**:
  - Brskalnik **nikoli avtomatsko ne odobri** dostopa do mikrofona, kamere ali DRM zaščitenih medijev (`onPermissionRequest`). Uporabnik je vedno vprašan s potrditvenim oknom z jasnim izpisom gostitelja (`origin`). Ob preklicu se klic varno zavrne (`deny()`).
- **Nadzor nad geolokacijo**:
  - Dostop do geografske lokacije zahteva izrecno potrditev uporabnika. Ob zavrnitvi ali zaprtju dialoga je dostop blokiran.
- **Popolna podpora za varno prijavo (OAuth 2.0 / SSO) & Zaščita pred popunderji**:
  - Varno odpiranje avtentikacijskih oken (`onCreateWindow`) zgolj ob neposredni uporabniški interakciji (`isUserGesture`), kar popolnoma prepreči samodejne popunderje in hkrati zagotavlja nemoteno delovanje Google, GitHub in bančnih prijav.
- **Kanonična zaščita pred Path Traversal**:
  - `MobileFileProvider` preverja kanonične poti (`canonicalFile`), kar preprečuje pobeg iz predvidenih map prek `../` ali simbolnih povezav.

### 3. 🧹 Kirurško Čiščenje Sledilnih Parametrov v URL-jih (Query Tracker Stripping)
- **Avtomatska nevtralizacija sledilcev**:
  - Ob kliku na povezave ali vnosu v naslovno vrstico se iz URL-jev kirurško odstranijo sledilni parametri za medstransko profiliranje:
    `utm_source`, `utm_medium`, `utm_campaign`, `utm_content`, `utm_term`, `fbclid`, `gclid`, `msclkid`, `twclid`, `ttclid`, `yclid`, `mc_eid`, `gad_source`, `gbraid`, `wbraid`, `dclid`, `zanpid`, `igshid`.
- **Ničelna kolateralna škoda (Zero UX Regression)**:
  - Stroga zaščita avtentikacijskih parametrov (OAuth 2.0 / SSO: `code`, `state`, `token`, `session_state`, `access_token`, `client_id`, `redirect_uri`), plačilnih portalov (Stripe, bančni portali: `session_id`, `payment_id`, `amount`, `return_url`), iskalnih poizvedb (`q`, `query`, `search`) in multimedijskih parametrov (`v`, `t`, `list`).

### 4. 🌐 Global Privacy Control (W3C GPC) & Do Not Track (DNT)
- **DOM / JavaScript API**:
  - `navigator.globalPrivacyControl = true` in `navigator.doNotTrack = "1"` sta privzeto uveljavljena pred izvajanjem katerekoli spletne skripte, kar samodejno sporoči platformam za upravljanje soglasij (CMP: OneTrust, Cookiebot, Klaro itd.) zahtevo za prepoved prodaje ali deljenja osebnih podatkov.
- **HTTP Zahtevki**:
  - Samodejno pošiljanje privzetih HTTP glav `Sec-GPC: 1` in `DNT: 1` pri vseh navigacijskih zahtevkih.

### 5. ⚡ Vrhunsko Blokiranje Oglasov & Proti-Clickjacking Zaščita
- **Razširjen Suffix Trie ($O(k)$)**:
  - Vgrajen obsežen register več kot 120 oglasnih borz, agresivnih popunder omrežij, lažnih potisnih obvestil ter vedenjske telemetrije.
- **Nevtralizacija prosojnih celozaslonskih prevlek**:
  - Zaznava in takojšnje brisanje nevidnih clickjacking slojev, ki jih spletna mesta uporabljajo za sprožitev oglasov ob prvem dotiku zaslona ali predvajalnika.
- **1x1 Prozorni GIF Nadomestek**:
  - Blokirane oglasne slike se nadomestijo z nevidnim 1x1 GIF-om, kar prepreči prikazovanje grdih zlomljenih okvirjev slik.

### 6. 🎨 Vgrajeno Kozmetično Filtriranje & Optimizacija Medijev
- Vgrajena CSS pravila za skrivanje oglasnih elementov (Element Hiding Rules po vzoru EasyList selektorjev), ki brez zunanjih odvisnosti odstranijo prazne oglasne okvirje (`.adsbygoogle`, `iframe[src*="doubleclick"]` ipd.).
- Baterijsko optimiziran cikel: aplikacija ne uporablja agresivnih WakeLock zaklepov in spoštuje sistemski življenjski cikel Android WebView.
- Brezkompromisen varnostni način `MIXED_CONTENT_NEVER_ALLOW`, ki onemogoča kakršnokoli nalaganje nešifriranih elementov na zaščitenih spletnih straneh.

### 7. ⚙️ Uporabniške Nastavitve, Persistenca & Podpora za Intente (v1.1)
- **Trajna hramba nastavitev (`PreferencesManager` / `SharedPreferences`)**:
  - Samodejno shranjevanje stikal: AdBlock ščit, AMOLED Temni način, Piškotki 3. oseb, JavaScript.
  - Možnost izbire privzetega iskalnika: **Google**, **DuckDuckGo** ali **Brave Search** z neposrednim iskanjem prek Omniboxa.
  - Kumulativni števec preprečenih oglasov in groženj (se ne ponastavi ob ponovnem zagonu aplikacije).
- **Prijavljeni prenosi s piškotki (`DownloadHandler`)**:
  - Samodejno posredovanje `Cookie` in `Referer` glav v `DownloadManager`, kar preprečuje napake `403 Forbidden` pri prenosu datotek iz prijavljenih uporabniških računov (oblak, forumi, repozitoriji).
- **Sistemska integracija Android (`ACTION_SEND` & `ACTION_WEB_SEARCH`)**:
  - Brskalnik podpira sprejemanje deljenih povezav in besedil iz drugih aplikacij (WhatsApp, Telegram, e-pošta) ter obdelavo sistemskih iskalnih zahtevkov.
- **Popolno čiščenje podatkov brskanja**:
  - Vgrajeno orodje za takojšen izbris piškotkov (`removeAllCookies`), predpomnilnika (`clearCache`), spletne shrambe (`deleteAllData`) in zgodovine brskanja.
- **Pristen User-Agent**:
  - Uporaba naravnega sistemskega WebView User-Agenta namesto statičnih fiksnih nizov, kar preprečuje napačne bot-checke in zmanjšuje tveganje za zlom strani.

---

## ⚡ Namestitev in Povezave (Installation & Verification)

> [!IMPORTANT]
> **Pomembno ob posodobitvi**: Različica v1.0.1 je podpisana z uradnim Safeer produkcijskim certifikatom (`CN=Safeer Mobile Browser`). Če imate na napravi še nameščeno staro razvojno (debug) različico, jo morate pred namestitvijo najprej odstraniti, saj Android zaradi varnosti ne dovoljuje neposredne nadgradnje aplikacije z drugačnim podpisnim certifikatom.

### 📥 1. Uradne APK Datoteke in Preverjanje Celovitosti:
* **Uradni Release APK**: [Safeer-Browser.apk](https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/Safeer-Browser.apk)
* **Release Artefakt**: [safeer-browser-release.apk](https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/Release/Artifacts/safeer-browser-release.apk)
* **Kontrolne vsote**: [SHA256SUMS](https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/SHA256SUMS)

Preverjanje celovitosti prenesenega paketa v terminalu:
```bash
sha256sum -c SHA256SUMS
```

### 📲 2. Samodejna namestitev prek uradnega terminalskega ukaza:
```bash
curl -sL https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/install_safeer.sh | bash
```

### 📱 3. Namestitev prek orodja ADB:
```bash
adb install -r Safeer-Browser.apk
```

---

## 🛠️ Gradnja iz Izvorne Kode

```bash
./build_mobile_apk.sh
```

Skripta samostojno prevede vire z AAPT2, prevede Kotlin kodo s `kotlinc`, generira DEX z `D8`, podpiše paket z `uber-apk-signer` ter samodejno osveži kontrolne vsote `SHA256SUMS`. Lokacijo orodij je mogoče prilagoditi prek okoljske spremenljivke `ANDROID_BUILD_TOOLS`.

---

## ⚖️ Pravno Obvestilo in Omejitev Odgovornosti (Disclaimer)

- **Varnostna omejitev**: **Safeer is a security layer, not a guarantee against all online threats.** Noben spletni brskalnik ali varnostni filter ne more zagotoviti 100 % ali absolutne zaščite pred vsemi novimi, ciljanimi ali še neznanimi grožnjami (Zero-Day). Safeer deluje kot lokalni varnostni sloj, ki bistveno zmanjšuje tveganje in blokira znana škodljiva vozlišča, zlonamerne domene in sledilce.
- **Vsebine in licence**: Kozmetično filtriranje oglasov uporablja odprta pravila skupnosti EasyList. Varnostne sezname zagotavljajo abuse.ch, Phishing Army in StevenBlack pod ustreznimi odprtimi pogoji uporabe.
- **Licenca in Prilagajanje (Forking)**: Projekt je izdan pod licenco [Apache License 2.0](LICENSE). Prosto ga klonirajte, delite, predelujte in prilagajajte po lastnih željah in potrebah.
- **Zasebnost**: Podrobnosti o ravnanju s podatki najdete v dokumentu [PRIVACY_POLICY.md](PRIVACY_POLICY.md).
