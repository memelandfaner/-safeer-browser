# Safeer Browser 1.0.19

- HTTPS strani privzeto ne smejo nalagati nezaščitenih HTTP vsebin. Stara spletna mesta, ki to zahtevajo, lahko zato izgubijo del vsebine; napak certifikatov ni mogoče obiti.
- Števec na začetni strani ne prišteva več trenutne seje k že shranjenim blokadam. Sočasne blokade se shranjujejo zaporedno.
- Ob ponovnem zagonu se obnovijo naslovi, vrstni red, izbira zavihka in namizni način. Naloži se samo izbrani zavihek. Shrani se največ 200 zavihkov, vedno tudi izbrani.
- Neaktivni zavihki se lahko po 10 minutah oziroma ob pomanjkanju pomnilnika uspavajo. Aktivni zavihek, zaznano predvajanje, celozaslonski video, zaznani urejeni obrazci in prijavne strani so izvzeti. Strani z vdelanimi okvirji ali nepreverljivim stanjem ostanejo naložene.
- Ob uspavanju se zgodovina naprej/nazaj hrani v pomnilniku. Po koncu procesa se obnovijo samo naslovi; vneseni obrazci, pozicija videa in stanje spletnih aplikacij se ne shranjujejo na disk.
- Če spletni pogon odpove, se prizadeti pogled odstrani. Zavihek ostane prazen in se ne naloži sam; stran se znova naloži šele, ko to izbere uporabnik.
- Brisanje podatkov brskanja zapre tudi odprte zavihke in zamenja shranjeno sejo s prazno začetno stranjo.
- Za vsak varnostni vir sta vidna čas zadnjega uspešnega preverjanja in napaka. Tudi odgovor 304 (seznam je nespremenjen) osveži čas preverjanja. Napaka ohrani stare veljavne sezname in se ne prikaže kot uspeh.
- GitHub Actions izvaja celotni regresijski paket na JDK 17, vključno s kriptografskimi testi, DNS, prijavami in zavihki.
- Opis projekta je usklajen z izvorno različico; nepodprta trditev o 85 % prihranku podatkov je odstranjena. Nastavitve berejo različico iz paketa.

Uspavanje zavihkov, predvajanje v ozadju, obnova seje in okrevanje po sesutju spletnega pogona so preizkušeni na dejanski napravi (Android, avtomatiziran preizkus na telefonu).
