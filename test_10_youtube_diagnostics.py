#!/usr/bin/env python3
import subprocess
import time
import os
import json

DEVICE = "192.168.0.216:34527"
OUTPUT_DIR = "/home/janez/Namizje/Neimenovana mapa/safeer-browser/yt_diagnostics_10"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# 10 različnih videoposnetkov: glasba, podkasti, gaming, novice, uradni kanali
VIDEOS = [
    ("01_Eminem_WithoutMe", "YVkUvmDQ3HY", "Eminem - Without Me"),
    ("02_Rick_Astley", "dQw4w9WgXcQ", "Rick Astley - Never Gonna Give You Up"),
    ("03_Despacito", "kJQP7kiw5Fk", "Luis Fonsi - Despacito"),
    ("04_Eminem_LoseYourself", "_Yhyp-_hX2s", "Eminem - Lose Yourself"),
    ("05_Queen_Bohemian", "fJ9rUzIMcZQ", "Queen - Bohemian Rhapsody"),
    ("06_Lofi_Girl", "jfKfPfyJRdk", "Lofi Hip Hop Radio"),
    ("07_Linkin_Park_InTheEnd", "eVTXPUF4Oz4", "Linkin Park - In the End"),
    ("08_Ed_Sheeran_ShapeOfYou", "JGwWNGJdvx8", "Ed Sheeran - Shape of You"),
    ("09_Maroon5_Sugar", "09R8_2nJtjg", "Maroon 5 - Sugar"),
    ("10_Adele_RollingInTheDeep", "rYEDA3JcQqw", "Adele - Rolling in the Deep")
]

print(f"🚀 [AGENT DIAGNOSTIKA] Začenjam testiranje 10 videoposnetkov na Samsung Galaxy S25 ({DEVICE})...\n")

# Clear logcat first
subprocess.run(["adb", "-s", DEVICE, "logcat", "-c"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

results = []

for idx, (slug, vid_id, title) in enumerate(VIDEOS, start=1):
    url = f"https://m.youtube.com/watch?v={vid_id}"
    print(f"[{idx}/10] 🎬 Testiram: {title} (ID: {vid_id})")
    
    # Prebudi telefon
    subprocess.run(["adb", "-s", DEVICE, "shell", "input", "keyevent", "224"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(0.2)
    subprocess.run(["adb", "-s", DEVICE, "shell", "input", "keyevent", "82"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(0.2)

    # Zaženi v Safeer Browserju
    cmd_start = ["adb", "-s", DEVICE, "shell", "am", "start", "-n", "com.example.safeerbrowser/.MainActivity", "-d", url]
    subprocess.run(cmd_start, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    # Počakaj 4 sekunde za predvajanje
    time.sleep(4.0)
    
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
    status = "⚠️ OPOZORILO" if has_error else "✅ BREZHIBNO"
    
    print(f"   📸 Slika: {shot_path} | Status: {status}")
    results.append({
        "index": idx,
        "title": title,
        "vid_id": vid_id,
        "screenshot": shot_path,
        "status": status,
        "has_error": has_error
    })

print("\n" + "="*70)
print("📊 POVZETEK TESTIRANJA 10 VIDEOPOSNETKOV:")
print("="*70)
for r in results:
    print(f"[{r['index']:02d}/10] {r['status']} - {r['title']}")

with open(os.path.join(OUTPUT_DIR, "results.json"), "w") as f:
    json.dump(results, f, indent=2)
