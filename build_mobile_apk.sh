#!/usr/bin/env bash
# ==============================================================================
# 🚀 BUILD SCRIPT: SAFEER MOBILE BROWSER (BOTNET C2 & MALWARE SHIELD)
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Samodejno odkrivanje orodij za gradnjo (Build Tools Discovery)
if [ -n "$ANDROID_BUILD_TOOLS" ] && [ -d "$ANDROID_BUILD_TOOLS" ]; then
    TOOLS_DIR="$ANDROID_BUILD_TOOLS"
elif [ -d "$DIR/.tools" ]; then
    TOOLS_DIR="$DIR/.tools"
elif [ -d "$DIR/../streamN-TV2/android_tv/.tools" ]; then
    TOOLS_DIR="$DIR/../streamN-TV2/android_tv/.tools"
elif [ -d "$HOME/.tools/android" ]; then
    TOOLS_DIR="$HOME/.tools/android"
elif [ -d "/home/janez/Namizje/Neimenovana mapa/streamN-TV2/android_tv/.tools" ]; then
    TOOLS_DIR="/home/janez/Namizje/Neimenovana mapa/streamN-TV2/android_tv/.tools"
else
    TOOLS_DIR=""
fi

if [ -z "$TOOLS_DIR" ] || [ ! -d "$TOOLS_DIR" ]; then
    echo "❌ Napaka: Orodja za gradnjo niso bila najdena."
    echo "👉 Nastavite okoljsko spremenljivko: export ANDROID_BUILD_TOOLS=/pot/do/orodij"
    echo "👉 Zahtevana orodja v mapi: kotlinc/, aapt2, r8.jar, android.jar, uber-apk-signer.jar"
    exit 1
fi

KOTLINC="$TOOLS_DIR/kotlinc/bin/kotlinc"
KOTLIN_LIB="$TOOLS_DIR/kotlinc/lib/kotlin-stdlib.jar"
BUILD_DIR="$DIR/build"
RELEASE_DIR="$DIR/Release/Artifacts"

echo "=========================================================="
echo "🛡️ GRADIM SAFEER MOBILE BROWSER (KOTLIN APK)"
echo "=========================================================="

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen"
mkdir -p "$BUILD_DIR/classes"
mkdir -p "$BUILD_DIR/dex"
mkdir -p "$RELEASE_DIR"

echo "⚙️ 1/5: Prevajam Android XML vire (AAPT2)..."
"$TOOLS_DIR/aapt2" compile --dir "$DIR/res" -o "$BUILD_DIR/compiled_res.zip"
"$TOOLS_DIR/aapt2" link -I "$TOOLS_DIR/android.jar" \
    --manifest "$DIR/AndroidManifest.xml" \
    --rename-manifest-package "com.safeer.mobile.browser" \
    -A "$DIR/assets" \
    --min-sdk-version 28 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name "1.0.0" \
    -o "$BUILD_DIR/resources.apk" \
    --java "$BUILD_DIR/gen" \
    "$BUILD_DIR/compiled_res.zip"

echo "☕ 2/5: Prevajam Kotlin izvorno kodo (kotlinc)..."
"$KOTLINC" -cp "$TOOLS_DIR/android.jar:$BUILD_DIR/gen" \
    -d "$BUILD_DIR/classes" \
    -jvm-target 1.8 \
    "$DIR/src/main/kotlin/com/example/safeerbrowser/"*.kt \
    "$BUILD_DIR/gen/com/example/safeerbrowser/R.java"

echo "⚡ 3/5: Prevajam v Dalvik Executable (D8)..."
java -cp "$TOOLS_DIR/r8.jar" com.android.tools.r8.D8 \
    --min-api 28 \
    --output "$BUILD_DIR/dex" \
    --lib "$TOOLS_DIR/android.jar" \
    "$BUILD_DIR/classes/com/example/safeerbrowser/"*.class \
    "$KOTLIN_LIB"

echo "📦 4/5: Sestavljam APK paket..."
cp "$BUILD_DIR/resources.apk" "$BUILD_DIR/unaligned.apk"
cd "$BUILD_DIR/dex"
jar -uf "$BUILD_DIR/unaligned.apk" classes.dex
cd "$DIR"

echo "✍️ 5/5: Podpisujem APK paket z uber-apk-signer..."
if [ -n "$RELEASE_KEYSTORE" ] && [ -f "$RELEASE_KEYSTORE" ]; then
    echo "🔑 Uporabljam produkcijski podpisni ključ ($RELEASE_KEYSTORE)..."
    java -jar "$TOOLS_DIR/uber-apk-signer.jar" \
        --apks "$BUILD_DIR/unaligned.apk" \
        --out "$BUILD_DIR/signed" \
        --ks "$RELEASE_KEYSTORE" \
        --ksAlias "${RELEASE_KEY_ALIAS:-safeer}" \
        --ksPass "${RELEASE_KEY_PASS:-safeer123}" \
        --allowResign
else
    echo "ℹ️ Podpisujem z v3 debug/beta podpisom (nastavite RELEASE_KEYSTORE za produkcijo)..."
    java -jar "$TOOLS_DIR/uber-apk-signer.jar" \
        --apks "$BUILD_DIR/unaligned.apk" \
        --out "$BUILD_DIR/signed" \
        --allowResign
fi

SIGNED_APK=$(find "$BUILD_DIR/signed" -type f -name "*Signed.apk" | head -n 1)
if [ -z "$SIGNED_APK" ] || [ ! -f "$SIGNED_APK" ]; then
    echo "❌ Napaka: Podpisan APK paket ni bil ustvarjen v $BUILD_DIR/signed"
    exit 1
fi

FINAL_APK="$RELEASE_DIR/safeer-mobile-release.apk"
cp "$SIGNED_APK" "$FINAL_APK"
cp "$FINAL_APK" "$RELEASE_DIR/safeer-browser-release.apk"
cp "$FINAL_APK" "$DIR/Safeer-Mobile.apk"
cp "$FINAL_APK" "$DIR/Safeer-Browser.apk"

# Generiranje uradnih SHA256SUMS kontrolnih vsot
cd "$DIR"
sha256sum Safeer-Browser.apk Safeer-Mobile.apk > "$DIR/SHA256SUMS"
sha256sum "Release/Artifacts/safeer-mobile-release.apk" "Release/Artifacts/safeer-browser-release.apk" > "$RELEASE_DIR/SHA256SUMS"

# Sinhronizacija v spletno mapo za prenos
WEB_MOB_DIR="${WEB_MOB_DIR:-/home/janez/Namizje/safeer-web/assets/mobile}"
if [ -d "$WEB_MOB_DIR" ]; then
    cp -f "$FINAL_APK" "$WEB_MOB_DIR/Safeer-Mobile.apk"
    cp -f "$FINAL_APK" "$WEB_MOB_DIR/Safeer-Browser.apk"
    cp -f "$DIR/SHA256SUMS" "$WEB_MOB_DIR/SHA256SUMS"
    echo "🌐 Sinhronizirano v safeer-web/assets/mobile/"
fi

echo ""
echo "=========================================================="
echo "🎉 ZGRAJEN SIGNED SAFEER MOBILE APK: $DIR/Safeer-Mobile.apk"
echo "Package ID: com.safeer.mobile.browser"
echo "=========================================================="
ls -lh "$DIR/Safeer-Mobile.apk"
