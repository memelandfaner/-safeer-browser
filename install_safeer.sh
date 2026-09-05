#!/usr/bin/env bash
# ==============================================================================
# 🛡️ 1-KLIK NAMESTITEV: SAFEER MOBILE BROWSER (SAMSUNG GALAXY & ANDROID)
# ==============================================================================
set -e

APK_URL="https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/Safeer-Browser.apk"
SHA_URL="https://raw.githubusercontent.com/memelandfaner/-safeer-browser/main/SHA256SUMS"
TEMP_APK="/tmp/Safeer-Browser.apk"
TEMP_SHA="/tmp/SAFEER_SHA256SUMS"

echo "=========================================================="
echo "🛡️ SAFEER BROWSER: 1-KLIK NAMESTITEV"
echo "=========================================================="

echo "📥 Prenašam najnovejšo različico Safeer Browser APK in kontrolne vsote..."
curl -L -s -o "$TEMP_APK" "$APK_URL"
curl -L -s -o "$TEMP_SHA" "$SHA_URL"

if [ ! -s "$TEMP_APK" ]; then
    echo "❌ Napaka pri prenosu APK paketa."
    exit 1
fi

echo "✅ APK uspešno prenesen ($(du -h "$TEMP_APK" | cut -f1))"

# Preverjanje celovitosti s SHA-256
if [ -s "$TEMP_SHA" ]; then
    EXPECTED_SHA=$(grep "Safeer-Browser.apk" "$TEMP_SHA" | head -n 1 | awk '{print $1}')
    if [ -n "$EXPECTED_SHA" ]; then
        ACTUAL_SHA=$(sha256sum "$TEMP_APK" | awk '{print $1}')
        if [ "$EXPECTED_SHA" = "$ACTUAL_SHA" ]; then
            echo "🔒 SHA-256 celovitost potrjena ($ACTUAL_SHA)"
        else
            echo "⚠️ Opozorilo: SHA-256 vsota se ne ujema (Pričakovano: $EXPECTED_SHA, Dobljeno: $ACTUAL_SHA)"
        fi
    fi
fi

# Preveri prisotnost ADB
if command -v adb >/dev/null 2>&1; then
    DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -n 1 | awk '{print $1}')
    if [ -n "$DEVICE" ]; then
        echo "📱 Zaznana ADB naprava: $DEVICE"
        adb -s "$DEVICE" install -r "$TEMP_APK"
        echo "🚀 Zaganjam Safeer Browser..."
        adb -s "$DEVICE" shell am start -n "com.safeer.mobile.browser/.MainActivity" || adb -s "$DEVICE" shell monkey -p com.safeer.mobile.browser 1
        echo "=========================================================="
        echo "🎉 SAFEER BROWSER JE USPEŠNO NAMEŠČEN IN ZAGNAN!"
        echo "=========================================================="
        exit 0
    fi
fi

# Če ni zaznane ADB naprave
cp "$TEMP_APK" "./Safeer-Browser.apk"
echo "ℹ️ APK paket je shranjen v: $(pwd)/Safeer-Browser.apk"
echo "👉 Lahko ga ročno namestite na svoj telefon."
