#!/usr/bin/env bash
# ==============================================================================
# 🛠️ SAFEER BROWSER — ANDROID BUILD TOOLCHAIN PROVISIONER
# ------------------------------------------------------------------------------
# Idempotently installs everything build_mobile_apk.sh needs and lays it out in
# the directory expected by that script (SAFEER_TOOLS_DIR):
#
#   $SAFEER_TOOLS_DIR/aapt2               (Android SDK build-tools 34.0.0)
#   $SAFEER_TOOLS_DIR/android.jar         (Android platform 34)
#   $SAFEER_TOOLS_DIR/r8.jar              (D8/R8 from build-tools, provides com.android.tools.r8.D8)
#   $SAFEER_TOOLS_DIR/kotlinc/            (JetBrains Kotlin command-line compiler)
#   $SAFEER_TOOLS_DIR/uber-apk-signer.jar (APK signer)
#
# Safe to run repeatedly: existing, valid components are reused and downloads are
# only performed when a component is missing.
# ==============================================================================
set -euo pipefail

TOOLS_DIR="${SAFEER_TOOLS_DIR:-$HOME/.safeer-tools}"
SDK="$TOOLS_DIR/android-sdk"
DL="$TOOLS_DIR/dl"

BUILD_TOOLS_VERSION="34.0.0"
PLATFORM="android-34"
KOTLIN_VERSION="1.9.24"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"
UBER_SIGNER_VERSION="1.3.0"

mkdir -p "$DL" "$SDK/cmdline-tools"

echo "=========================================================="
echo "🛠️  Provisioning Safeer Android toolchain in: $TOOLS_DIR"
echo "=========================================================="

# --- 1. Android SDK command-line tools ---------------------------------------
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
    echo "📥 1/5 Downloading Android command-line tools..."
    curl -fsSL -o "$DL/cmdline-tools.zip" \
        "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
    rm -rf "$DL/tmp-cmdline" "$SDK/cmdline-tools/latest"
    unzip -q "$DL/cmdline-tools.zip" -d "$DL/tmp-cmdline"
    mv "$DL/tmp-cmdline/cmdline-tools" "$SDK/cmdline-tools/latest"
    rm -rf "$DL/tmp-cmdline"
else
    echo "✅ 1/5 Android command-line tools already present."
fi

# --- 2. build-tools + platform ------------------------------------------------
if [ ! -x "$SDK/build-tools/$BUILD_TOOLS_VERSION/aapt2" ] || \
   [ ! -f "$SDK/platforms/$PLATFORM/android.jar" ]; then
    echo "📦 2/5 Installing build-tools;$BUILD_TOOLS_VERSION and platforms;$PLATFORM..."
    yes | "$SDKMANAGER" --sdk_root="$SDK" \
        "platform-tools" \
        "build-tools;$BUILD_TOOLS_VERSION" \
        "platforms;$PLATFORM" > "$DL/sdkmanager.log" 2>&1 || {
            echo "❌ sdkmanager failed; see $DL/sdkmanager.log"; tail -20 "$DL/sdkmanager.log"; exit 1;
        }
else
    echo "✅ 2/5 build-tools;$BUILD_TOOLS_VERSION and platforms;$PLATFORM already installed."
fi

# --- 3. Kotlin command-line compiler -----------------------------------------
if [ ! -x "$TOOLS_DIR/kotlinc/bin/kotlinc" ]; then
    echo "📥 3/5 Downloading Kotlin compiler $KOTLIN_VERSION..."
    curl -fsSL -o "$DL/kotlin-compiler.zip" \
        "https://github.com/JetBrains/kotlin/releases/download/v$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.zip"
    rm -rf "$TOOLS_DIR/kotlinc"
    unzip -q "$DL/kotlin-compiler.zip" -d "$TOOLS_DIR"
else
    echo "✅ 3/5 Kotlin compiler already present."
fi

# --- 4. uber-apk-signer -------------------------------------------------------
if [ ! -f "$TOOLS_DIR/uber-apk-signer.jar" ]; then
    echo "📥 4/5 Downloading uber-apk-signer $UBER_SIGNER_VERSION..."
    curl -fsSL -o "$TOOLS_DIR/uber-apk-signer.jar" \
        "https://github.com/patrickfav/uber-apk-signer/releases/download/v$UBER_SIGNER_VERSION/uber-apk-signer-$UBER_SIGNER_VERSION.jar"
else
    echo "✅ 4/5 uber-apk-signer already present."
fi

# --- 5. Symlink the flat layout build_mobile_apk.sh expects -------------------
echo "🔗 5/5 Linking flat toolchain layout..."
ln -sf "$SDK/build-tools/$BUILD_TOOLS_VERSION/aapt2"        "$TOOLS_DIR/aapt2"
ln -sf "$SDK/platforms/$PLATFORM/android.jar"              "$TOOLS_DIR/android.jar"
ln -sf "$SDK/build-tools/$BUILD_TOOLS_VERSION/lib/d8.jar"  "$TOOLS_DIR/r8.jar"

echo ""
echo "=========================================================="
echo "🎉 Toolchain ready. Build with:"
echo "     SAFEER_TOOLS_DIR=\"$TOOLS_DIR\" ./build_mobile_apk.sh"
echo "=========================================================="
