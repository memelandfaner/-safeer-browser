# 🛡️ Safeer Browser (Mobile Security Edition)

**Safeer Browser** je napreden, visoko-varen mobilni spletni brskalnik za Android, posebej optimiziran za sodobne mobilne naprave (npr. **Samsung Galaxy S25**, 120Hz Dynamic AMOLED 2X) s strojno podprto **kibernetsko zaščito pred Botnet C2 strežniki, zlonamerno programsko opremo (Malware), spletnim ribarjenjem (Phishing) ter agresivnimi oglasnimi mrežami**.

---

## 🛑 Ključne Varnostne in Zasebnostne Značilnosti

### 1. 🛡️ Avtomatiziran Threat Shield z Atomsko Zamenjavo (Atomic Trie Swap)
- **Viri groženj v živo (Live HTTPS Feeds)**:
  - **abuse.ch Feodo Tracker**: Blokada C2 (Command & Control) botnet strežnikov (Dridex, Emotet, TrickBot, QakBot).
  - **abuse.ch URLhaus**: Blokada domen in gostiteljev za razširjanje zlonamerne kode (Malware distribution).
  - **Phishing Army Extended**: Zaščita pred lažnim predstavljanjem in krajo bančnih ter osebnih podatkov.
- **Semenska baza (Seed Database)**:
  - Vgrajeni indikatorji napadov (**abuse.ch ThreatFox IOC**) in zlonamerna omrežja (**StevenBlack Unified**).
- **🔄 Brezprekinitvena Atomska Zamenjava (Atomic Swap)**:
  - Ob posodobitvi seznamov v ozadju se zgradi novo drevo groženj in se atomsko zamenja (`swapThreatTrie`), s čimer se prepreči ohranjanje zastarelih lažnih zaznav.
- **🔒 Zero-Bypass Pravilo**:
  - Za nevarne C2/malware domene **ne veljajo nobene video ali embed izjeme**.
- **🔑 Enokratni Kriptografski Žetoni za Obvoz (One-Time Token Interstitial)**:
  - Ob poskusu obiska nevarne domene se prikaže rdeč opozorilni zaslon. Morebiten obvoz na lastno odgovornost (`safeer://bypass-threat`) je zaščiten z naključnim enokratnim žetonom (UUID), vezanim na točno določeno domeno in časovno veljavnost (5 min). Zunanje spletne strani ne morejo sprožiti neavtoriziranega odklepa.

### 2. 🛡️ Stroga Zaščita Zasebnosti in Dovoljenj (Zero Auto-Grant)
- **Interaktivna privolitev za strojne vire**:
  - Brskalnik **nikoli avtomatsko ne odobri** dostopa do mikrofona, kamere ali DRM zaščitenih medijev (`onPermissionRequest`). Uporabnik je vedno eksplicitno vprašan s potrditvenim oknom z jasnim izpisom gostitelja (`origin`).
- **Nadzor nad geolokacijo**:
  - Dostop do geografske lokacije zahteva izrecno potrditev uporabnika. Ob preklicu ali zavrnitvi je dostop blokiran.
- **Blokada piškotkov tretjih oseb**:
  - `setAcceptThirdPartyCookies(false)` preprečuje medstransko sledenje (cross-site tracking).
- **Kanonična zaščita pred Path Traversal**:
  - `MobileFileProvider` preverja kanonične poti (`canonicalFile`), kar preprečuje pobeg iz predvidenih map prek `../` ali simbolnih povezav.

### 3. ⚡ Visoko-zmogljiv Radix / Domain Suffix Trie ($O(k)$)
- Preverjanje domen v mikrosekundah brez obremenjevanja procesorja ali počasnih nizovnih zank.
- Avtomatsko prestrezanje vseh poddomen (`sub.evil-server.cc` -> `evil-server.cc`).

### 4. 🎨 EasyList Kozmetično Filtriranje & Optimizacija Medijev
- Odstranitev praznih oglasnih okvirjev preko injiciranja CSS pravil (`##.ad-slot, ##[id^="google_ads"]`).
- Vgrajena skripta za neprekinjeno predvajanje medijev v ozadju z ugasnjenim zaslonom.
- Uravnotežen `MIXED_CONTENT_COMPATIBILITY_MODE`, ki omogoča nemoteno predvajanje zakonitih medijskih tokov brez izpostavljanja aktivnim skriptnim napadom.

---

## ⚡ Hitra 1-Klik Namestitev & Kratke Povezave (Quick Install)

### 🚀 1. Kratka povezava za vpis v brskalnik na telefonu:
Vnesite v poljuben brskalnik na telefonu za takojšen prenos APK:
* 👉 **`tinyurl.com/safeer-apk`** (ali `https://tinyurl.com/safeer-apk`)
* 👉 Alternativa: **`tinyurl.com/safeer-mobi`**
* 👉 Rezervna povezava: **`da.gd/xfcGi`**

### 📲 2. 1-Vrstični samodejni namestitveni ukaz prek terminala:
```bash
curl -sL https://tinyurl.com/install-safeer | bash
```
*(Ali z uradne GitHub povezave: `curl -sL https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/install_safeer.sh | bash`)*

### 📥 3. Neposredna povezava do uradne APK datoteke:
* **Najnovejši Release APK**: [Safeer-Browser.apk](https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/Safeer-Browser.apk)
* **Release Artifact**: [safeer-browser-release.apk](https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/Release/Artifacts/safeer-browser-release.apk)

### 📱 4. Lokalna namestitev prek ADB:
```bash
adb -s 192.168.0.216:34527 install -r Safeer-Browser.apk
```

---

## 🛠️ Gradnja iz Izvorne Kode

```bash
./build_mobile_apk.sh
```
Skripta uporablja AAPT2, Kotlinc, D8 in Uber-Apk-Signer ter ustvari podpisan, optimiziran APK paket.
