# 🛡️ Pravilnik o Zasebnosti (Privacy Policy)

**Zadnja posodobitev**: September 2026  
**Projekt**: Safeer Browser (Mobile Security Edition)  
**Koda**: [https://github.com/memelandfaner/-safeer-browser](https://github.com/memelandfaner/-safeer-browser)

---

## 1. 🔒 Temeljna Zaveza k Zasebnosti
Safeer Browser je zasnovan z načelom popolne ničelne telemetrije (Zero-Telemetry by Design). Vaša zasebnost je temeljna človekova pravica, zato:
- **Brez beleženja**: Safeer Browser ne zbira, ne shranjuje in ne prenaša vaše zgodovine brskanja, vnesenih spletnih naslovov ali osebnih podatkov na zunanje strežnike.
- **Brez uporabniških računov**: Za uporabo brskalnika ni potrebna registracija, e-poštni naslov ali prijava.
- **Brez sledilne telemetrije**: V kodi ni nobenih analitičnih SDK-jev (brez Firebase Analytics, brez Google Analytics, brez Facebook Pixel).

---

## 2. 🛡️ Lokalno Procesiranje Varnostnih Filtrov
Vsa zaščita deluje izključno lokalno na vaši napravi:
- **Threat Shield**: Preverjanje domen zoper Botnet C2, zlonamerno kodo (Malware) in spletno ribarjenje (Phishing) poteka v lokalnem pomnilniku prek hitrega drevesa pripon (*Domain Suffix Trie*). Nobena poizvedba o obiskani domeni se ne pošilja v oblak.
- **Preprečevanje sledenja v URL-jih**: Parametri za medstransko sledenje (`utm_*`, `fbclid`, `gclid` itd.) se kirurško odstranijo neposredno v napravi pred posredovanjem zahtevka.
- **Kozmetično filtriranje oglasov**: Odstranjevanje oglasnih elementov se izvaja neposredno v lokalnem pogonu WebView.

---

## 3. 🌐 Varnostni Seznami Tretjih Oseb (Threat Intelligence Feeds)
Za zaznavanje novih spletnih groženj brskalnik občasno (največ enkrat na 24 ur) prenese javno dostopne varnostne sezname neposredno prek šifrirane HTTPS povezave iz zaupanja vrednih virov:
- **abuse.ch** (ThreatFox IOC, URLhaus)
- **Phishing Army Extended**

Ob prenosu se izračuna kriptografska kontrolna vsota SHA-256 za preverjanje celovitosti podatkov. Ti seznami ne vsebujejo nobenih osebnih podatkov in se uporabljajo izključno za posodobitev lokalnega registra groženj.

---

## 4. 🔑 Standardi W3C: GPC in DNT
Safeer Browser pri vsakem spletnem zahtevku in v DOM okolju samodejno uveljavlja:
- `Sec-GPC: 1` in `navigator.globalPrivacyControl = true` (Global Privacy Control)
- `DNT: 1` in `navigator.doNotTrack = "1"` (Do Not Track)

S tem brskalnik avtomatsko sporoči obiskanim spletnim mestom in sistemom za upravljanje privolitev (CMP), da uporabnik prepoveduje prodajo ali deljenje svojih osebnih podatkov.

---

## 5. 📱 Dovoljenja Naprave (Android Permissions)
Safeer Browser se drži načela minimalnih privilegijev:
- **Kamera in mikrofon**: Zahtevata izrecno potrditev uporabnika prek sistemskega pogovornega okna ob vsaki zahtevi spletnega mesta (`Zero Auto-Grant`).
- **Lokacija**: Uporabi se le, če uporabnik izrecno klikne »Dovoli« v potrditvenem oknu.
- **Lokalna shramba**: Dostop do datotek je omejen na varno aplikacijsko mapo in mapo za javne prenose. V aplikaciji ni dovoljen pobeg izpredvidenih map (*Path Traversal Protection*).

---

## 6. ⚖️ Odprta Koda in Preglednost
Celotna izvorna koda projekta je javno dostopna pod licenco **Apache License 2.0**. Vsak uporabnik ali varnostni raziskovalec lahko neodvisno preveri skladnost delovanja aplikacije z zgornjimi načeli.
