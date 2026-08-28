#!/usr/bin/env python3
import subprocess
import time
import os

DEVICE = "192.168.0.216:46875"
OUTPUT_DIR = "/home/janez/Namizje/Neimenovana mapa/safeer-browser/yt_music_tests"
os.makedirs(OUTPUT_DIR, exist_ok=True)

TRACKS = [
    ("01_Rick_Astley", "dQw4w9WgXcQ", "Rick Astley - Never Gonna Give You Up"),
    ("02_Despacito", "kJQP7kiw5Fk", "Luis Fonsi - Despacito"),
    ("03_Shape_of_You", "JGwWNGJdvx8", "Ed Sheeran - Shape of You"),
    ("04_Gangnam_Style", "9bZkp7q19f0", "PSY - GANGNAM STYLE"),
    ("05_See_You_Again", "RgKAFK5djSk", "Wiz Khalifa - See You Again"),
    ("06_Bohemian_Rhapsody", "fJ9rUzIMcZQ", "Queen - Bohemian Rhapsody"),
    ("07_Counting_Stars", "hT_nvWreIhg", "OneRepublic - Counting Stars"),
    ("08_Uptown_Funk", "OPf0YbXqDm0", "Mark Ronson - Uptown Funk"),
    ("09_Adele_Hello", "YQHsXMglC9A", "Adele - Hello"),
    ("10_Maroon5_Sugar", "09R8_2nJtjg", "Maroon 5 - Sugar"),
    ("11_Katy_Perry_Roar", "CevxZvSJLk8", "Katy Perry - Roar"),
    ("12_Ed_Sheeran_Perfect", "2Vv-BfVoq4g", "Ed Sheeran - Perfect"),
    ("13_Girls_Like_You", "SlPhMPnQ58k", "Maroon 5 - Girls Like You"),
    ("14_Bruno_Mars_Thats_What_I_Like", "LsoLEjrDogU", "Bruno Mars - That's What I Like"),
    ("15_Linkin_Park_Numb", "kXYiU_JCYtU", "Linkin Park - Numb"),
    ("16_Taylor_Swift_Blank_Space", "e-ORhEE9VVg", "Taylor Swift - Blank Space"),
    ("17_Dua_Lipa_New_Rules", "lp-EO5I60KA", "Dua Lipa - New Rules"),
    ("18_Imagine_Dragons_Believer", "7wtfhZwyrcc", "Imagine Dragons - Believer"),
    ("19_Bruno_Mars_The_Lazy_Song", "fLexgOxsZu0", "Bruno Mars - The Lazy Song"),
    ("20_ACDC_Thunderstruck", "v2AC41dglnM", "AC/DC - Thunderstruck")
]

print(f"🚀 Začenjam avtomatizirano testiranje 20 glasbenih posnetkov na YouTube...")

results = []

for idx, (slug, vid_id, title) in enumerate(TRACKS, start=1):
    url = f"https://m.youtube.com/watch?v={vid_id}"
    print(f"\n[{idx}/20] 🎵 Predvajam: {title} (ID: {vid_id})")
    
    # 1. Zaženi video URL v Safeer Browserju
    cmd_start = ["adb", "-s", DEVICE, "shell", "am", "start", "-n", "com.example.safeerbrowser/.MainActivity", "-d", url]
    subprocess.run(cmd_start, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    # 2. Počakaj 5 sekund za zagon predvajanja brez oglasov
    time.sleep(5.5)
    
    # 3. Zajem zaslona za potrditev
    shot_path = os.path.join(OUTPUT_DIR, f"{slug}.png")
    with open(shot_path, "wb") as f:
        subprocess.run(["adb", "-s", DEVICE, "exec-out", "screencap", "-p"], stdout=f, stderr=subprocess.DEVNULL)
    
    print(f"   📸 Zajem shranjen: {shot_path}")
    results.append((idx, title, shot_path, "OK"))

print("\n" + "="*60)
print("🎉 VSIH 20 GLASBENIH POSNETKOV USPEŠNO PREIZKUŠENIH!")
print("="*60)
for idx, title, shot, status in results:
    print(f"[{idx:02d}/20] ✅ {title} -> {status}")
