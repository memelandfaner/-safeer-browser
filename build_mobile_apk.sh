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
    --target-sdk-version 35 \
    -o "$BUILD_DIR/resources.apk" \
    --java "$BUILD_DIR/gen" \
    "$BUILD_DIR/compiled_res.zip"

echo "☕ 2/5: Prevajam Kotlin izvorno kodo (kotlinc)..."
"$KOTLINC" -cp "$TOOLS_DIR/android.jar:$BUILD_DIR/gen" \
    -d "$BUILD_DIR/classes" \
    -jvm-target 1.8 \
    "$DIR/src/main/kotlin/com/safeer/mobile/browser/"*.kt \
    "$BUILD_DIR/gen/com/safeer/mobile/browser/R.java"

echo "⚡ 3/5: Prevajam v Dalvik Executable (D8)..."
java -cp "$TOOLS_DIR/r8.jar" com.android.tools.r8.D8 \
    --min-api 28 \
    --output "$BUILD_DIR/dex" \
    --lib "$TOOLS_DIR/android.jar" \
    "$BUILD_DIR/classes/com/safeer/mobile/browser/"*.class \
    "$KOTLIN_LIB"

echo "📦 4/5: Sestavljam APK paket..."
cp "$BUILD_DIR/resources.apk" "$BUILD_DIR/unaligned.apk"
cd "$BUILD_DIR/dex"
jar -uf "$BUILD_DIR/unaligned.apk" classes.dex
cd "$DIR"

echo "✍️ 5/5: Podpisujem APK paket z namenskim produkcijskim ključem..."
KEYSTORE_DIR="$DIR/keystore"
DEFAULT_KEYSTORE="$KEYSTORE_DIR/safeer-release.jks"
RELEASE_KEYSTORE="${RELEASE_KEYSTORE:-$DEFAULT_KEYSTORE}"
RELEASE_KEY_ALIAS="${RELEASE_KEY_ALIAS:-safeer-browser}"

# Preberi geslo iz okolja ali lokalne zaščitene datoteke (brez privzetega gesla v skripti)
if [ -z "$RELEASE_KEY_PASS" ] && [ -f "$KEYSTORE_DIR/.release_pass" ]; then
    RELEASE_KEY_PASS="$(cat "$KEYSTORE_DIR/.release_pass")"
fi

if [ -z "$RELEASE_KEY_PASS" ]; then
    echo "❌ Napaka: Geslo produkcijskega ključa (RELEASE_KEY_PASS) ni nastavljeno."
    echo "👉 Nastavite okoljsko spremenljivko: export RELEASE_KEY_PASS=\"...\""
    echo "👉 Ali shranite geslo v lokalno datoteko $KEYSTORE_DIR/.release_pass (ki je v .gitignore)"
    exit 1
fi

if [ ! -f "$RELEASE_KEYSTORE" ]; then
    echo "🔑 Generiram namenski produkcijski release keystore ($RELEASE_KEYSTORE)..."
    mkdir -p "$KEYSTORE_DIR"
    keytool -genkeypair -v \
        -keystore "$RELEASE_KEYSTORE" \
        -alias "$RELEASE_KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$RELEASE_KEY_PASS" \
        -keypass "$RELEASE_KEY_PASS" \
        -dname "CN=Safeer Mobile Browser, OU=Safeer Security, O=Safeer, L=Ljubljana, ST=Slovenia, C=SI"
fi

java -jar "$TOOLS_DIR/uber-apk-signer.jar" \
    --apks "$BUILD_DIR/unaligned.apk" \
    --out "$BUILD_DIR/signed" \
    --ks "$RELEASE_KEYSTORE" \
    --ksAlias "$RELEASE_KEY_ALIAS" \
    --ksPass "$RELEASE_KEY_PASS" \
    --ksKeyPass "$RELEASE_KEY_PASS" \
    --allowResign

SIGNED_APK=$(find "$BUILD_DIR/signed" -type f -iname "*signed.apk" | head -n 1)
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
(cd "$RELEASE_DIR" && sha256sum safeer-mobile-release.apk safeer-browser-release.apk > "$RELEASE_DIR/SHA256SUMS")

# Sinhronizacija v spletno mapo za prenos (če je nastavljena)
if [ -n "$WEB_MOB_DIR" ] && [ -d "$WEB_MOB_DIR" ]; then
    cp -f "$FINAL_APK" "$WEB_MOB_DIR/Safeer-Mobile.apk"
    cp -f "$FINAL_APK" "$WEB_MOB_DIR/Safeer-Browser.apk"
    cp -f "$DIR/SHA256SUMS" "$WEB_MOB_DIR/SHA256SUMS"
    echo "🌐 Sinhronizirano v $WEB_MOB_DIR"
fi

echo ""
echo "=========================================================="
echo "🎉 ZGRAJEN SIGNED SAFEER MOBILE APK: $DIR/Safeer-Mobile.apk"
echo "Package ID: com.safeer.mobile.browser"
echo "=========================================================="
ls -lh "$DIR/Safeer-Mobile.apk"
