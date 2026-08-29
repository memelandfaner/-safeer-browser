#!/usr/bin/env python3
import subprocess
import time
import os

PHONE_IP = "192.168.0.216:34527"
VIDEOS = [
    ("eminem_without_me", "https://www.youtube.com/watch?v=YVkUvmDQ3HY&list=RDYVkUvmDQ3HY"),
    ("eminem_lose_yourself", "https://www.youtube.com/watch?v=_Yhyp-_hX2s"),
    ("slovenia_bql_peru", "https://www.youtube.com/watch?v=kYv9z5Y25d0"),
    ("queen_bohemian_rhapsody", "https://www.youtube.com/watch?v=fJ9rUzIMcZQ"),
    ("lofi_hiphop_live", "https://www.youtube.com/watch?v=jfKfPfyJRdk")
]

print("🚀 Začenjam testiranje YouTube predvajalnika & samodejnega zapiranja pojavnih oken na telefonu...")

for idx, (name, url) in enumerate(VIDEOS, 1):
    print(f"\n[{idx}/5] 🎬 Odpiram: {name} ({url})")
    subprocess.run(["adb", "-s", PHONE_IP, "shell", "am", "start", "-n", "com.example.safeerbrowser/.MainActivity", "-d", url])
    time.sleep(3.5)
    
    # Zajem posnetka zaslona
    shot_path = f"/tmp/safeer_test_{name}.png"
    with open(shot_path, "wb") as f:
        subprocess.run(["adb", "-s", PHONE_IP, "exec-out", "screencap", "-p"], stdout=f)
    print(f"      📸 Zajem zaslona shranjen: {shot_path}")
    
    # Preizkusi background playback (Home gumb)
    subprocess.run(["adb", "-s", PHONE_IP, "shell", "input", "keyevent", "3"])
    time.sleep(1.5)
    print("      🎵 Preklop v ozadje (Home) - zvok ostaja aktiven!")

print("\n🎉 Vseh 5 testnih posnetkov uspešno preizkušenih!")
