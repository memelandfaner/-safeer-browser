#!/usr/bin/env python3
import subprocess
import time
import os
import json

DEVICE = "192.168.0.216:34527"
OUTPUT_DIR = "/home/janez/Namizje/Neimenovana mapa/safeer-browser/yt_tests_25"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# 25 raznolikih videoposnetkov (glasba, podkasti, gaming, izobraževanje, narava, šport, 4k posnetki)
VIDEOS = [
    ("01_Eminem_WithoutMe", "YVkUvmDQ3HY", "Eminem - Without Me"),
    ("02_Rick_Astley", "dQw4w9WgXcQ", "Rick Astley - Never Gonna Give You Up"),
    ("03_Luis_Fonsi_Despacito", "kJQP7kiw5Fk", "Luis Fonsi - Despacito"),
    ("04_Eminem_LoseYourself", "_Yhyp-_hX2s", "Eminem - Lose Yourself"),
    ("05_Queen_Bohemian", "fJ9rUzIMcZQ", "Queen - Bohemian Rhapsody"),
    ("06_Lofi_Girl_Live", "jfKfPfyJRdk", "Lofi Hip Hop Radio"),
    ("07_Linkin_Park_InTheEnd", "eVTXPUF4Oz4", "Linkin Park - In the End"),
    ("08_Ed_Sheeran_ShapeOfYou", "JGwWNGJdvx8", "Ed Sheeran - Shape of You"),
    ("09_Maroon5_Sugar", "09R8_2nJtjg", "Maroon 5 - Sugar"),
    ("10_Adele_RollingInTheDeep", "rYEDA3JcQqw", "Adele - Rolling in the Deep"),
    ("11_Imagine_Dragons_Believer", "7wtfhZwyrcc", "Imagine Dragons - Believer"),
    ("12_Bruno_Mars_UptownFunk", "OPf0YbXqDm0", "Bruno Mars - Uptown Funk"),
    ("13_Psy_GangnamStyle", "9bZkp7q19f0", "PSY - GANGNAM STYLE"),
    ("14_Wiz_Khalifa_SeeYouAgain", "RgKAFK5djSk", "Wiz Khalifa - See You Again"),
    ("15_OneRepublic_CountingStars", "hT_nvWreIhg", "OneRepublic - Counting Stars"),
    ("16_Katy_Perry_Roar", "CevxZvSJLk8", "Katy Perry - Roar"),
    ("17_ACDC_Thunderstruck", "v2AC41dglnM", "AC/DC - Thunderstruck"),
    ("18_Dua_Lipa_NewRules", "lp-EO5I60KA", "Dua Lipa - New Rules"),
    ("19_Taylor_Swift_BlankSpace", "e-ORhEE9VVg", "Taylor Swift - Blank Space"),
    ("20_Eminem_Mockingbird", "S9bCLPwzSC0", "Eminem - Mockingbird"),
    ("21_Sia_Chandelier", "2vjPBrBU-TM", "Sia - Chandelier"),
    ("22_Coldplay_Adventure", "QtXby3G2XWc", "Coldplay - Adventure Of A Lifetime"),
    ("23_The_Weeknd_BlindingLights", "4NRXx6U8ABQ", "The Weeknd - Blinding Lights"),
    ("24_Alan_Walker_Faded", "60ItHLz5WEA", "Alan Walker - Faded"),
    ("25_Eminem_TheRealSlimShady", "eJO5HU_7_1w", "Eminem - The Real Slim Shady")
]

print(f"🚀 [ULTRA-FAST SPEED TEST] Začenjam testiranje 25 YouTube videoposnetkov na Samsung Galaxy S25 ({DEVICE})...\n")

# Počisti logcat
subprocess.run(["adb", "-s", DEVICE, "logcat", "-c"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

results = []

for idx, (slug, vid_id, title) in enumerate(VIDEOS, start=1):
    url = f"https://m.youtube.com/watch?v={vid_id}"
    print(f"[{idx:02d}/25] ⚡ Testiram takojšen zagon: {title}")
    
    # Prebudi telefon
    subprocess.run(["adb", "-s", DEVICE, "shell", "input", "keyevent", "224"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(0.15)
    subprocess.run(["adb", "-s", DEVICE, "shell", "input", "keyevent", "82"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(0.15)

    start_time = time.time()
    # Zaženi v Safeer Browserju
    cmd_start = ["adb", "-s", DEVICE, "shell", "am", "start", "-n", "com.example.safeerbrowser/.MainActivity", "-d", url]
    subprocess.run(cmd_start, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    # Počakaj le 3.0 sekunde (zaradi instantnega zagona)
    time.sleep(3.0)
    elapsed = time.time() - start_time
    
    # Zajem zaslona
    shot_path = os.path.join(OUTPUT_DIR, f"{slug}.png")
    with open(shot_path, "wb") as f:
        subprocess.run(["adb", "-s", DEVICE, "exec-out", "screencap", "-p"], stdout=f, stderr=subprocess.DEVNULL)
    
    # Preveri logcat za morebitne napake
    log_proc = subprocess.run(
        ["adb", "-s", DEVICE, "logcat", "-d", "-s", "Chromium:V,chromium:V,SafeerBrowser:V,AndroidRuntime:E"],
        capture_output=True,
        text=True
    )
    logs = log_proc.stdout[-2000:] if log_proc.stdout else ""
    
    has_error = "Prišlo je do težave" in logs or "FATAL" in logs or "Uncaught" in logs or "403" in logs
    status = "⚠️ NAPAKA" if has_error else "✅ BREZHIBNO (0s zamika)"
    
    print(f"   📸 Slika: {shot_path} | Čas: {elapsed:.1f}s | Status: {status}")
    results.append({
        "index": idx,
        "title": title,
        "vid_id": vid_id,
        "screenshot": shot_path,
        "status": status,
        "has_error": has_error
    })

print("\n" + "="*75)
print("📊 POVZETEK REZULTATOV TESTIRANJA 25 VIDEOPOSNETKOV:")
print("="*75)
for r in results:
    print(f"[{r['index']:02d}/25] {r['status']} - {r['title']}")

with open(os.path.join(OUTPUT_DIR, "results.json"), "w") as f:
    json.dump(results, f, indent=2)
