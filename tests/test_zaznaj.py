# -*- coding: utf-8 -*-
"""Zaznavalnik sivega plakata mora prepoznati pravo napako.

Plakat nariše Android WebView, ne nas brskalnik, zato je napaka ista na telefonu
in na televizorju. Vzorci so resnicni posnetki zaslona: eden pred popravkom in
dva po njem. Ce kdo zaznavalnik omehca toliko, da plakata ne vidi vec, ta test pade.
"""
import os
import sys
import unittest

from PIL import Image

KOREN = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(KOREN, "tools"))

import zaznaj  # noqa: E402

VZORCI = os.path.join(KOREN, "tests", "vzorci")


def slika(ime):
    return Image.open(os.path.join(VZORCI, ime))


class SiviPlakat(unittest.TestCase):

    def test_plakat_pred_popravkom_je_zaznan(self):
        self.assertTrue(zaznaj.plakat_prisoten(slika("plakat-pred.png")),
                        "plakata ne prepozna vec: " + zaznaj.opis(slika("plakat-pred.png")))

    def test_crn_prehod_po_popravku_ni_plakat(self):
        self.assertFalse(zaznaj.plakat_prisoten(slika("plakat-po.png")),
                         "crn prehod zmotno steje za plakat")

    def test_slika_videa_ni_plakat(self):
        self.assertFalse(zaznaj.plakat_prisoten(slika("video.png")),
                         "navadna slika videa zmotno steje za plakat")


if __name__ == "__main__":
    unittest.main()
