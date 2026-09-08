# Safeer Mobile 1.0.11

- Spletni prikaz se konča nad Androidovo navigacijsko vrstico, tako da spodnji gumbi in fiksna vnosna polja strani ostanejo dostopni.
- Na Androidu 11 in novejšem se prostor za sistemske vrstice, izrez zaslona in tipkovnico upošteva v nativni postavitvi. Višini tipkovnice in navigacijske vrstice se ne seštevata.
- Upoštevani so tudi stranski odmiki pri ležečem zaslonu. Ob izhodu iz celozaslonskega videa se odmiki obnovijo.
- Android 9/10 ohranita sistemsko prilagajanje okna in `adjustResize`, brez izmišljenih odmikov pri ničelnih sistemskih vrednostih.

## Preizkus na telefonu

1. Namestite `Safeer-Mobile.apk` kot posodobitev obstoječe aplikacije.
2. Odprite stran s fiksnim spodnjim vnosnim poljem (npr. pogovor na sliki). Pri zaprti tipkovnici morajo biti vsi spodnji gumbi nad sistemskimi gumbi.
3. Odprite in zaprite tipkovnico; vnosno polje mora ostati dosegljivo, po zaprtju pa ne sme ostati prazen pas v velikosti tipkovnice.
4. Ponovite z navigacijskimi kretnjami in v ležečem načinu.
5. Odprite celozaslonski video, zapustite celozaslonski način in znova preverite spodnji rob strani.

Preizkus na fizičnem telefonu še ni opravljen: ob pripravi popravka telefon ni bil povezan prek ADB.
