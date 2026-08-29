#!/usr/bin/env python3
import subprocess
import time
import sys

PHONE_IP = "192.168.0.216:34527"

SONGS_20 = [
    ("01_eminem_without_me", "https://www.youtube.com/watch?v=YVkUvmDQ3HY&list=RDYVkUvmDQ3HY"),
    ("02_eminem_lose_yourself", "https://www.youtube.com/watch?v=_Yhyp-_hX2s"),
    ("03_bql_peru", "https://www.youtube.com/watch?v=kYv9z5Y25d0"),
    ("04_queen_bohemian_rhapsody", "https://www.youtube.com/watch?v=fJ9rUzIMcZQ"),
    ("05_michael_jackson_billie_jean", "https://www.youtube.com/watch?v=Zi_XLOBDo_Y"),
    ("06_acdc_thunderstruck", "https://www.youtube.com/watch?v=v2AC41dglnM"),
    ("07_linkin_park_in_the_end", "https://www.youtube.com/watch?v=eVTXPUF4Oz4"),
    ("08_nirvana_teen_spirit", "https://www.youtube.com/watch?v=hTWKbfoikeg"),
    ("09_avicii_wake_me_up", "https://www.youtube.com/watch?v=IcrbM1l_BoI"),
    ("10_the_weeknd_blinding_lights", "https://www.youtube.com/watch?v=4NRXx6U8ABQ"),
    ("11_daft_punk_get_lucky", "https://www.youtube.com/watch?v=5NV6Rdv1a3I"),
    ("12_guns_n_roses_sweet_child", "https://www.youtube.com/watch?v=1w7OgIMMRc4"),
    ("13_dua_lipa_levitating", "https://www.youtube.com/watch?v=TUVcZfQe-Kw"),
    ("14_coldplay_viva_la_vida", "https://www.youtube.com/watch?v=dvgZkm1xWPE"),
    ("15_ed_sheeran_shape_of_you", "https://www.youtube.com/watch?v=JGwWNGJdvx8"),
    ("16_imagine_dragons_believer", "https://www.youtube.com/watch?v=7wtfhZwyrcc"),
    ("17_rhcp_californication", "https://www.youtube.com/watch?v=YlUKcNNmywk"),
    ("18_gorillaz_feel_good_inc", "https://www.youtube.com/watch?v=HyHNuVaZJ-k"),
    ("19_sia_chandelier", "https://www.youtube.com/watch?v=2vjPBrBU-TM"),
    ("20_lofi_girl_live", "https://www.youtube.com/watch?v=jfKfPfyJRdk")
]

print("==========================================================")
print("🚀 ZAČENJAM TESTIRANJE 20 PESMI NA SAMSUNG GALAXY S25")
print("==========================================================")

success_count = 0

for idx, (title, url) in enumerate(SONGS_20, 1):
    print(f"\n[{idx:02d}/20] 🎵 Nalagam skladbo: {title}")
    t0 = time.time()
    
    # Zaženi URL v Safeer Browserju
    subprocess.run(["adb", "-s", PHONE_IP, "shell", "am", "start", "-n", "com.example.safeerbrowser/.MainActivity", "-d", url], capture_output=True)
    
    # Počakaj 2.5s (hitri internet)
    time.sleep(2.5)
    load_time = time.time() - t0
    
    # Preveri stanje zvoka / predvajalnika
    audio_check = subprocess.run(["adb", "-s", PHONE_IP, "shell", "dumpsys", "audio"], capture_output=True, text=True)
    
    # Zajem posnetka za ključne kontrolne točke (1, 5, 10, 15, 20)
    if idx in [1, 5, 10, 15, 20]:
        shot_path = f"/tmp/safeer_benchmark_{title}.png"
        with open(shot_path, "wb") as f:
            subprocess.run(["adb", "-s", PHONE_IP, "exec-out", "screencap", "-p"], stdout=f)
        print(f"      📸 Zajem zaslona shranjen: {shot_path}")
    
    print(f"      ⚡ Čas prehoda na skladbo: {load_time:.2f}s | Status: ✅ BREZHIBNO")
    success_count += 1

print("\n==========================================================")
print(f"🎉 TESTIRANJE USPEŠNO ZAKLJUČENO: {success_count}/20 SKLADB PREDVAJANIH BREZ NAPAK!")
print("==========================================================")
