# 🛡️ Safeer Browser (Mobile Security Edition)

**Safeer Browser** je sodoben odprtokodni mobilni spletni brskalnik za Android z napredno večslojno zaščito, zasnovan za zmanjšanje izpostavljenosti znanim Botnet C2 strežnikom, zlonamerni programski opremi (Malware), spletnemu ribarjenju (Phishing) ter agresivnim sledilnim in oglasnim mrežam.

---

## 🛑 Ključne Varnostne in Zasebnostne Značilnosti

### 1. 🛡️ Večslojni Threat Shield z Atomsko Zamenjavo (Atomic Trie Swap)
- **Viri groženj v živo (Live HTTPS Feeds)**:
  - **abuse.ch Feodo Tracker**: Zaznava in blokada C2 (Command & Control) botnet strežnikov (Dridex, Emotet, TrickBot, QakBot).
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
- **📝 SHA-256 Revizijsko Beleženje**:
  - Ob vsakem prenosu seznama brskalnik izračuna SHA-256 kontrolno vsoto za varnostni revizijski dnevnik (Audit Log) za sledljivost posodobitev.

### 2. 🛡️ Stroga Zaščita Zasebnosti in Dovoljenj (Zero Auto-Grant)
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

### 6. 🎨 EasyList Kozmetično Filtriranje & Optimizacija Medijev
- Odstranitev praznih oglasnih okvirjev preko injiciranja CSS pravil (`##.adsbygoogle, ##ins.adsbygoogle, ##iframe[src*="doubleclick"]`).
- Vgrajena skripta za nemoteno predvajanje medijev v ozadju z ugasnjenim zaslonom.
- Uravnotežen `MIXED_CONTENT_COMPATIBILITY_MODE`, ki omogoča nemoteno predvajanje zakonitih medijskih tokov brez izpostavljanja aktivnim skriptnim napadom.

---

## ⚡ Namestitev in Povezave (Installation & Verification)

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

- **Varnostna omejitev**: Noben spletni brskalnik ali varnostni filter ne more zagotoviti 100 % zaščite pred vsemi novimi ali neznanimi grožnjami (Zero-Day). Safeer Browser uporablja več nivojev zaščite za občutno zmanjšanje napadalne površine in tveganja.
- **Vsebine in licence**: Kozmetično filtriranje oglasov uporablja odprta pravila skupnosti EasyList. Varnostne sezname zagotavljajo abuse.ch, Phishing Army in StevenBlack pod ustreznimi odprtimi pogoji uporabe.
- **Licenca**: Projekt je izdan pod licenco [Apache License 2.0](LICENSE).
- **Zasebnost**: Podrobnosti o ravnanju s podatki najdete v dokumentu [PRIVACY_POLICY.md](PRIVACY_POLICY.md).
