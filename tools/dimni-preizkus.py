#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Dimni preizkus na pravi napravi: pet stvari, ki morajo delovati vedno.

Preizkusa se tista razlicica, ki je na napravi namescena -- skripta nicesar ne
gradi in ne podpisuje. Tako jo je mogoce pognati tudi nad APK-jem, ki gre v
izdajo, in ne nad nakljucno lokalno gradnjo.

  python3 tools/dimni-preizkus.py [IP:vrata]

Brez naprave se konca s kodo 0 in jasno pove, da ni preizkusal nicesar -- tako
ga je varno klicati tudi tam, kjer telefona ni.

Preverimo:
  1. brskalnik se zazene in se ne sesuje
  2. seznami groznj so naloženi
  3. blokiranje oglasov res blokira
  4. SponsorBlock najde odseke za znani video
  5. pred videom ni sive ploskve z gumbom (zaznaj.py)
"""
from __future__ import annotations

import http.server
import io
import os
import re
import socket
import subprocess
import sys
import tempfile
import threading
import time
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import zaznaj  # noqa: E402

from PIL import Image  # noqa: E402

PAKET = "com.safeer.mobile.browser"
DEJAVNOST = f"{PAKET}/{PAKET}.MainActivity"
PRIVZETA_NAPRAVA = "192.168.0.87:38007"

# Tri znana oglasna omrezja; vsa tri so na seznamu EasyList in med vgrajenimi pravili.
OGLASNI_NASLOVI = [
    "https://googleads.g.doubleclick.net/pagead/id",
    "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
    "https://securepubads.g.doubleclick.net/tag/js/gpt.js",
]
TESTNA_STRAN = """<!doctype html><html lang="sl"><head><meta charset="utf-8">
<title>Safeer: preizkus blokiranja</title></head><body>
<h1>Preizkus blokiranja oglasov</h1>
{zahtevki}
</body></html>"""
# Ta video ima v SponsorBlocku sponzorski odsek (preverjeno 12. 9. 2026).
SPONZOR_VIDEO = "https://m.youtube.com/watch?v=p_65gktSY1c"
VIDEO = "https://m.youtube.com/watch?v=UXg_mF6Hjqw"

# Izrez zaslona telefona brez sistemske vrstice in brez spodnjega dela strani.
VRH, DNO = 0.10, 0.45

OZNAKE = ["SafeerSecurity:I", "SafeerAdBlock:D", "SafeerSponsorBlock:I",
          "SafeerCrashHandler:E", "AndroidRuntime:E"]


class Naprava:
    def __init__(self, naslov):
        self.naslov = naslov

    def adb(self, *a, timeout=90):
        return subprocess.run(["adb", "-s", self.naslov] + list(a),
                              capture_output=True, text=True, timeout=timeout)

    def ziva(self):
        r = self.adb("get-state", timeout=15)
        return r.returncode == 0 and "device" in r.stdout

    def razlicica(self):
        r = self.adb("shell", "dumpsys", "package", PAKET, timeout=30)
        m = re.search(r"versionName=(\S+)", r.stdout)
        return m.group(1) if m else "?"

    def pocisti_dnevnik(self):
        self.adb("logcat", "-c", timeout=30)

    def dnevnik(self):
        return self.adb("logcat", "-d", "-s", *OZNAKE, timeout=60).stdout

    def zazeni(self):
        self.adb("shell", "am", "force-stop", PAKET)
        time.sleep(1)
        self.adb("shell", "am", "start", "-n", DEJAVNOST)

    def zaslon_spi(self) -> bool:
        r = self.adb("shell", "dumpsys", "power", timeout=30)
        return "mWakefulness=Asleep" in r.stdout or "mWakefulness=Dozing" in r.stdout

    def prebudi(self):
        """Vrne True, ce je bil zaslon prej ugasnjen (in ga je treba na koncu vrniti)."""
        if not self.zaslon_spi():
            return False
        self.adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        time.sleep(3)
        return True

    def uspavaj(self):
        self.adb("shell", "input", "keyevent", "KEYCODE_SLEEP")

    def odpri(self, url):
        """Vedno svez zagon: ce je ista stran ze odprta, Android namere ne dostavi
        kot novega nalaganja in zahtevkov sploh ni -- preizkus bi bil nakljucen."""
        self.adb("shell", "am", "force-stop", PAKET)
        time.sleep(1.5)
        self.adb("shell", "am", "start", "-a", "android.intent.action.VIEW",
                 "-d", url, "-n", DEJAVNOST)

    def zaslon(self) -> Image.Image:
        # Naprava lahko zaspi sredi preizkusa (lasten casovnik): tedaj bi bili vsi
        # posnetki crni in preizkus bi padel po krivem.
        if self.zaslon_spi():
            self.adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
            time.sleep(3)
        p = subprocess.run(["adb", "-s", self.naslov, "exec-out", "screencap", "-p"],
                           capture_output=True, timeout=60)
        return Image.open(io.BytesIO(p.stdout))


def preizkus_zagon(n: Naprava) -> tuple[bool, str]:
    n.pocisti_dnevnik()
    n.zazeni()
    time.sleep(12)
    r = n.adb("shell", "pidof", PAKET, timeout=30)
    if not r.stdout.strip():
        return False, "brskalnik po zagonu ne tece"
    return True, "brskalnik tece"


def preizkus_seznami(n: Naprava) -> tuple[bool, str]:
    """Seznami groznj se nalozijo v ozadju; damo jim do 60 s."""
    for _ in range(12):
        dnevnik = n.dnevnik()
        m = re.search(r"Seznami v uporabi: (.{0,120})", dnevnik)
        if m:
            return True, m.group(1).strip()
        time.sleep(5)
    return False, "sporocila 'Seznami v uporabi' ni v dnevniku"


def moj_naslov(proti: str) -> str:
    """Naslov tega racunalnika v istem omrezju kot naprava."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect((proti.split(":")[0], 9))
        return s.getsockname()[0]
    finally:
        s.close()


class TihStreznik(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *a):  # brez sumov v izpisu
        pass


def preizkus_blokiranje(n: Naprava) -> tuple[bool, str]:
    """Nasa lastna stran zahteva tri znane oglasne naslove; vsi trije morajo pasti."""
    mapa = tempfile.mkdtemp(prefix="safeer-oglasi-")
    zahtevki = "\n".join(
        f'<img src="{u}" width="1" height="1" alt="">' for u in OGLASNI_NASLOVI)
    Path(mapa, "index.html").write_text(
        TESTNA_STRAN.format(zahtevki=zahtevki), encoding="utf-8")

    streznik = http.server.ThreadingHTTPServer(
        ("0.0.0.0", 0),
        lambda *a, **k: TihStreznik(*a, directory=mapa, **k))
    threading.Thread(target=streznik.serve_forever, daemon=True).start()
    vrata = streznik.server_address[1]
    naslov = f"http://{moj_naslov(n.naslov)}:{vrata}/index.html"

    try:
        n.pocisti_dnevnik()
        n.odpri(naslov)
        time.sleep(20)
        dnevnik = n.dnevnik()
    finally:
        streznik.shutdown()

    blokirani = [u for u in OGLASNI_NASLOVI if u.split("//")[1].split("/")[0] in dnevnik]
    if not blokirani:
        return False, "niti en od treh znanih oglasnih naslovov ni bil blokiran"
    if len(blokirani) < len(OGLASNI_NASLOVI):
        manjka = [u for u in OGLASNI_NASLOVI if u not in blokirani]
        return False, "niso blokirani: " + ", ".join(manjka)
    return True, f"blokirani vsi trije znani oglasni naslovi ({len(blokirani)}/3)"


def preizkus_sponsorblock(n: Naprava) -> tuple[bool, str]:
    n.pocisti_dnevnik()
    n.odpri(SPONZOR_VIDEO)
    time.sleep(35)
    m = re.search(r"video \S+: (\d+) odsekov za preskok", n.dnevnik())
    if not m:
        return False, "SponsorBlock ni javil odsekov (strežnik ali omrezje?)"
    if int(m.group(1)) == 0:
        return False, "SponsorBlock je javil 0 odsekov za video, ki jih ima"
    return True, f"{m.group(1)} odsekov za preskok"


def preizkus_plakat(n: Naprava) -> tuple[bool, str]:
    n.odpri(VIDEO)
    for _ in range(20):
        time.sleep(0.6)
        slika = n.zaslon()
        if zaznaj.plakat_prisoten(slika, vrh=VRH, dno=DNO):
            return False, "siva ploskev z gumbom: " + zaznaj.opis(slika, vrh=VRH, dno=DNO)
    return True, "v 20 kadrih med nalaganjem videa ni sivega plakata"


def preizkus_brez_sesutja(n: Naprava) -> tuple[bool, str]:
    dnevnik = n.dnevnik()
    vrstice = [v for v in dnevnik.splitlines()
               if "FATAL EXCEPTION" in v or "Uncaught exception" in v]
    if vrstice:
        return False, vrstice[0][:160]
    return True, "v dnevniku ni nobenega sesutja"


PREIZKUSI = [
    ("brskalnik se zazene", preizkus_zagon),
    ("seznami groženj", preizkus_seznami),
    ("blokiranje oglasov", preizkus_blokiranje),
    ("SponsorBlock", preizkus_sponsorblock),
    ("sivi plakat pred videom", preizkus_plakat),
    ("brez sesutja", preizkus_brez_sesutja),
]


def main() -> int:
    naslov = sys.argv[1] if len(sys.argv) > 1 else PRIVZETA_NAPRAVA
    n = Naprava(naslov)
    if not n.ziva():
        subprocess.run(["adb", "connect", naslov], capture_output=True, timeout=30)
        if not n.ziva():
            print(f"Naprave {naslov} ni. Nisem preizkusil nicesar.")
            return 0

    spal = n.prebudi()
    if spal:
        print("Zaslon je spal; prebudil sem ga in ga bom na koncu spet ugasnil.")

    print(f"Preizkusam na {naslov}, razlicica {n.razlicica()}\n")
    padlo = []
    for ime, preizkus in PREIZKUSI:
        vredu, sporocilo = preizkus(n)
        print(f"  {ime:28} {'V REDU' if vredu else 'PADLO'}")
        print(f"      {sporocilo}")
        if not vredu:
            padlo.append(ime)

    if spal:
        n.uspavaj()

    print()
    if padlo:
        print("PADLO: " + ", ".join(padlo))
        return 1
    print("Vse v redu.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
