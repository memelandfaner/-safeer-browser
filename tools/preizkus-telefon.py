#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Preizkus na pravi napravi: pred videom ne sme biti sive ploskve z gumbom.

  python3 tools/preizkus-telefon.py [IP:vrata]

Brez naprave se konca s kodo 0 in pove, da ni preizkusal nicesar.
"""
from __future__ import annotations

import io
import os
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import zaznaj  # noqa: E402

from PIL import Image  # noqa: E402

PAKET = "com.safeer.mobile.browser"
PRIVZETA_NAPRAVA = "192.168.0.87:38007"
VIDEO = "https://m.youtube.com/watch?v=UXg_mF6Hjqw"

# Na telefonu je predvajalnik pod naslovno vrstico, v zgornji tretjini zaslona.
VRH, DNO = 0.10, 0.45


def adb(naprava, *a, timeout=60):
    return subprocess.run(["adb", "-s", naprava] + list(a),
                          capture_output=True, text=True, timeout=timeout)


def zaslon(naprava) -> Image.Image:
    p = subprocess.run(["adb", "-s", naprava, "exec-out", "screencap", "-p"],
                       capture_output=True, timeout=60)
    return Image.open(io.BytesIO(p.stdout))


def main() -> int:
    naprava = sys.argv[1] if len(sys.argv) > 1 else PRIVZETA_NAPRAVA
    r = adb(naprava, "get-state", timeout=15)
    if r.returncode != 0 or "device" not in r.stdout:
        print("Naprave %s ni; nic nisem preizkusil." % naprava)
        return 0

    print("Preizkusam na %s\n" % naprava)
    adb(naprava, "shell", "am", "force-stop", PAKET)
    time.sleep(1)
    adb(naprava, "shell", "am", "start", "-a", "android.intent.action.VIEW",
        "-d", VIDEO, PAKET)

    najdeni = []
    for i in range(20):
        time.sleep(0.6)
        s = zaslon(naprava)
        if zaznaj.plakat_prisoten(s, VRH, DNO):
            najdeni.append((i, zaznaj.opis(s, VRH, DNO)))

    if najdeni:
        print("  sivi plakat pred videom    PADLO")
        print("      v %d kadrih, npr. kader %d: %s" % (len(najdeni), najdeni[0][0],
                                                        najdeni[0][1]))
        print("\nPADLO: popravek, ki smo ga ze naredili, se je vrnil.")
        return 1

    print("  sivi plakat pred videom    V REDU")
    print("      v 20 kadrih med nalaganjem videa ni sivega plakata")
    print("\nPopravek se drzi.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
