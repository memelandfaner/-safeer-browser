# 🛡️ Safeer Browser (Mobile Security Edition)

**Safeer Browser** je napreden, visoko-varen mobilni spletni brskalnik za Android, posebej optimiziran za **Samsung Galaxy S25 (120Hz Dynamic AMOLED 2X, Android One UI 8.5)** s pravo **kibernetsko zaščito pred Botnet C2 strežniki, zlonamerno programsko opremo (Malware), spletnim ribarjenjem (Phishing) ter agresivnimi oglasnimi mrežami**.

---

## 🛑 Ključne Varnostne Značilnosti

### 1. 🛡️ Namenski Threat Block Shield (abuse.ch Feodo, URLhaus & ThreatFox)
- **Viri groženj**:
  - **abuse.ch Feodo Tracker**: Blokada C2 (Command & Control) botnet strežnikov (Dridex, Emotet, TrickBot, QakBot).
  - **abuse.ch URLhaus**: Blokada domen in povezav za razširjanje zlonamerne kode (Malware distribution).
  - **abuse.ch ThreatFox**: Blokada indikatorjev napadov (IOC).
  - **Phishing Army**: Zaščita pred lažnim predstavljanjem in krajo bančnih/uporabniških podatkov.
  - **StevenBlack Unified**: Blokada nevarnih stavniških in preusmeritvenih domen.
- **🔒 Zero-Bypass Pravilo**: Za nevarne C2/malware domene **ne veljajo nobene video/m3u8/embed izjeme**.
- **⚠️ Main-Frame Varnostni Opozorilni Zaslon (Security Interstitial Page)**:
  - Ob poskusu obiska nevarne domene brskalnik prikaže eleganten rdeč opozorilni zaslon z razlago grožnje, virom zaznave ter možnostjo varne vrnitve ali odklepa na lastno odgovornost.
- **🔑 SHA-256 Integriteta**: Preverjanje kontrolnih vsot pri posodabljanju seznamov groženj v ozadju.

### 2. ⚡ Visoko-zmogljiv Radix / Domain Suffix Trie ($O(k)$)
- Preverjanje domen v mikrosekundah brez obremenjevanja procesorja ali počasnih nizovnih zank.
- Avtomatsko prestrezanje vseh poddomen (`sub.evil-server.cc` -> `evil-server.cc`).

### 3. 🎯 Path-Aware Rule Matching & Odprava Mrtvih Pravil
- Pravilno razdeljevanje domen in poti za prestrezanje vzorcev kot so `yandex.ru/metrika`, `/pagead/`, `/ads/`, `/pixel.`, `gtag/js`, `collect?v=`.

### 4. 🎨 EasyList Kozmetično Filtriranje & YouTube Predvajalnik
- Odstranitev praznih oglasnih okvirjev preko injiciranja CSS pravil (`##.ad-slot, ##[id^="google_ads"]`).
- SmartTube/Brave mehanizem za samodejni preskok YouTube oglasov in neprekinjeno predvajanje v ozadju (Background Audio Playback z ugasnjenim zaslonom).

---

## 📲 Hitra Namestitev na Telefon

```bash
./install_mobile.sh
```

Ali ročna namestitev zgrajenega paketa `Safeer-Browser.apk`:
```bash
adb install -r Safeer-Browser.apk
```

---

## 🛠️ Gradnja iz Izvorne Kode

```bash
./build_mobile_apk.sh
```
Skripta uporablja AAPT2, Kotlinc, D8 in Uber-Apk-Signer ter ustvari podpisan, optimiziran APK paket.
