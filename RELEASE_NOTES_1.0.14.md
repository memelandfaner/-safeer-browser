# Safeer Mobile 1.0.14

Update of the protection against web traps (Safeer Threat Shield):

- Fake online banks: an address that imitates a bank (for example `nlb-klik-prijava.com`, `otpbamka.si` or look-alike letters from another alphabet) and a page on a foreign address that presents itself as a bank while showing a password, SMS code or card field get a warning with "Back", "Open the real site" and "Continue for this session". Back from the warning skips the fake page.
- Real banks work undisturbed: Slovenian banks and savings banks, their banking groups, PayPal, Revolut, N26, Wise and the pages used for logins and card payments (Bankart, Halcom, SI-PASS, 3-D Secure) are never blocked by ad rules or phishing lists and get no cosmetic or pop-up scripts.
- Threat lists on every start without slowing the browser: ThreatFox, URLhaus and Phishing Army lists saved by the previous run are loaded in the background at once; about 12 seconds after start the browser checks for newer lists (an unchanged list is one small request) and then every 6 hours. Before, the downloaded lists were lost at every restart within 24 hours.
- Prepared layer for the verified, signed Safeer threat list (Ed25519), checked in the background after every start.

All checks run on the phone; no address or page content is sent anywhere.

Slovensko: Posodobljena zaščita pred spletnimi pastmi. Brskalnik opozori pred lažnimi spletnimi bankami (naslov, ki posnema banko, ali stran na tujem naslovu, ki se predstavlja kot banka in zahteva geslo, kodo SMS ali podatke kartice) in ponudi gumb za pravo stran banke. Prave banke in plačilne strani delujejo nemoteno. Seznami nevarnih strani se ob vsakem zagonu takoj naložijo v ozadju in približno 12 sekund po zagonu preverijo za posodobitve, zato zagon in nalaganje strani nista upočasnjena. Vse preverjanje poteka na telefonu.
