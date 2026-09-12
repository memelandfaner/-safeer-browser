# Safeer Mobile 1.0.15

Fewer ads, sponsor segments skipped, a better history and a wider net against web traps:

- Ads: the full EasyList list of ad rules (about 52,000 network rules) is compiled into an indexed filter engine inside the browser, so ad scripts, trackers and empty ad placeholders on news sites are blocked before they load. The list is refreshed in the background together with the threat lists.
- SponsorBlock: sponsor, self-promotion and "subscribe" segments in YouTube videos are skipped with a short notice. The lookup sends only the first characters of a hash of the video id, never the video itself (k-anonymity); it can be switched off in the settings.
- History: the history dialog has a search field, entries are grouped by day and de-duplicated, a long press opens a link in a new tab, copies it or deletes it, and the address bar suggests visited pages and bookmarks while you type.
- Threat lists: HaGeZi Threat Intelligence Feeds (mini) and HaGeZi Fake (fake shops and scams) and the SI-CERT list of phishing domains confirmed in Slovenia join ThreatFox, URLhaus and Phishing Army.
- Fake banks: pages that ask for a card number under the guise of a fine, a parcel fee or a tax refund are stopped as a card trap; tax-number and PIN fields count as credentials; every warning reminds you that a bank never calls to ask for codes or to install remote-access software.
- Home page logo displays correctly again.

All checks run on the phone; no address or page content is sent anywhere.

Slovensko: Manj oglasov (celoten seznam EasyList je vgrajen v brskalnik in blokira oglasne skripte, sledilce in prazne oglasne okvirje), preskok sponzorskih odsekov v videih na YouTubu (SponsorBlock; pošlje se le del zgoščene vrednosti oznake videa, izklop v nastavitvah), zgodovina z iskanjem, razvrstitvijo po dnevih in predlogi v naslovni vrstici, novi seznami nevarnih strani (HaGeZi, SI-CERT) ter past za podatke kartice pod pretvezo kazni, paketa ali vračila davka. Vse preverjanje poteka na telefonu.
