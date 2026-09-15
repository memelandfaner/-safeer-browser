#!/usr/bin/env bash
# ==============================================================================
# Gradnja brez podpisa -- za CI.
#
# Doslej se je ta brskalnik prevajal samo na enem prenosniku. Ce bi se prevajanje
# pokvarilo, tega ne bi opazil nihce do naslednje izdaje.
#
# Ta skripta naredi prve stiri korake gradnje (viri, Kotlin, DEX, paket) in se
# ustavi pred podpisovanjem. Podpisni kljuc ostane doma in ga tu ni; namen ni
# izdelati paketa za uporabnike, ampak dokazati, da se koda prevede.
#
# Pricakuje:
#   ANDROID_HOME   -- Android SDK (aapt2, d8.jar, android.jar)
#   KOTLINC        -- pot do kotlinc (privzeto: kotlinc iz PATH)
# ==============================================================================
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$DIR/build-ci"

: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    echo "NAPAKA: ANDROID_HOME ni nastavljen."
    exit 1
fi

# Najnovejsa razlicica build-tools, ki je na voljo.
BUILD_TOOLS="$(find "$ANDROID_HOME/build-tools" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -n 1)"
AAPT2="$BUILD_TOOLS/aapt2"
D8_JAR="$BUILD_TOOLS/lib/d8.jar"

# Platforma mora ustrezati --target-sdk-version v AndroidManifest/gradnji.
CILJNI_SDK=36
ANDROID_JAR="$ANDROID_HOME/platforms/android-$CILJNI_SDK/android.jar"
if [ ! -f "$ANDROID_JAR" ]; then
    ANDROID_JAR="$(find "$ANDROID_HOME/platforms" -maxdepth 2 -name android.jar | sort -V | tail -n 1)"
fi

KOTLINC="${KOTLINC:-kotlinc}"

for orodje in "$AAPT2" "$D8_JAR" "$ANDROID_JAR"; do
    [ -e "$orodje" ] || { echo "NAPAKA: manjka $orodje"; exit 1; }
done
command -v "$KOTLINC" >/dev/null 2>&1 || [ -x "$KOTLINC" ] || { echo "NAPAKA: kotlinc ni najden ($KOTLINC)"; exit 1; }

echo "aapt2:      $AAPT2"
echo "d8:         $D8_JAR"
echo "android.jar:$ANDROID_JAR"
echo "kotlinc:    $KOTLINC"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex"

echo "1/4: Android XML viri (aapt2)"
"$AAPT2" compile --dir "$DIR/res" -o "$BUILD_DIR/compiled_res.zip"
"$AAPT2" link -I "$ANDROID_JAR" \
    --manifest "$DIR/AndroidManifest.xml" \
    --rename-manifest-package "com.safeer.mobile.browser" \
    -A "$DIR/assets" \
    --min-sdk-version 28 \
    --target-sdk-version "$CILJNI_SDK" \
    -o "$BUILD_DIR/resources.apk" \
    --java "$BUILD_DIR/gen" \
    "$BUILD_DIR/compiled_res.zip"

LIB_CLASSPATH=""
for lib in "$DIR"/libs/*.jar; do LIB_CLASSPATH="$LIB_CLASSPATH:$lib"; done

mapfile -d '' -t KT_VIRI < <(find "$DIR/src/main/kotlin" -name '*.kt' -print0 | sort -z)
if [ "${#KT_VIRI[@]}" -eq 0 ]; then
    echo "NAPAKA: pod src/main/kotlin ni nobene datoteke .kt."
    exit 1
fi

echo "2/4: Kotlin (${#KT_VIRI[@]} datotek)"
"$KOTLINC" -J-Xmx3g -cp "$ANDROID_JAR:$BUILD_DIR/gen$LIB_CLASSPATH" \
    -d "$BUILD_DIR/classes" \
    -jvm-target 1.8 \
    "${KT_VIRI[@]}" \
    "$BUILD_DIR/gen/com/safeer/mobile/browser/R.java"

mapfile -d '' -t RAZREDI < <(find "$BUILD_DIR/classes" -name '*.class' -print0 | sort -z)

# kotlin-stdlib pobere kotlinc iz svoje mape lib/
KOTLIN_BIN="$(command -v "$KOTLINC" || echo "$KOTLINC")"
KOTLIN_LIB="$(cd "$(dirname "$(readlink -f "$KOTLIN_BIN")")/../lib" && pwd)/kotlin-stdlib.jar"
[ -f "$KOTLIN_LIB" ] || { echo "NAPAKA: kotlin-stdlib.jar ni najden ($KOTLIN_LIB)"; exit 1; }

echo "3/4: DEX (d8)"
java -cp "$D8_JAR" com.android.tools.r8.D8 \
    --min-api 28 \
    --output "$BUILD_DIR/dex" \
    --lib "$ANDROID_JAR" \
    "${RAZREDI[@]}" \
    "$KOTLIN_LIB" "$DIR"/libs/*.jar

echo "4/4: Nepodpisan APK"
cp "$BUILD_DIR/resources.apk" "$BUILD_DIR/nepodpisan.apk"
(cd "$BUILD_DIR/dex" && jar -uf "$BUILD_DIR/nepodpisan.apk" ./*.dex)

test -s "$BUILD_DIR/nepodpisan.apk" || { echo "NAPAKA: APK ni nastal"; exit 1; }
ls -lh "$BUILD_DIR/nepodpisan.apk"
echo "Koda se prevede. Paket ni podpisan in ni za uporabnike."
