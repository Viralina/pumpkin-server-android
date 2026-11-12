#!/usr/bin/env bash
# Build an APK without Android Studio / Gradle / SDK manager.
# Requirements: aapt2, d8, apksigner, keytool, javac, zip + android.jar
#   (Android SDK Build-Tools 34.0.0 and JDK 17)
#   ANDROID_JAR=~/android-build/sdk/android-34/android.jar ./build.sh
#
# Two variants:
#   ./build.sh                                  -> pumpkin-panel-plain.apk (no binary)
#   ./build.sh --with-binary /sdcard/pumpkin    -> pumpkin-panel-full.apk  (with binary)
set -euo pipefail

ROOT=$(cd "$(dirname "$0")" && pwd)
ANDROID_JAR=${ANDROID_JAR:-$HOME/android-build/sdk/android-34/android.jar}
MIN_SDK=24
TARGET_SDK=28
VERSION_CODE=5
VERSION_NAME=1.0.0

BUNDLE_BIN=${BUNDLE_BIN:-}
APK_NAME=${APK_NAME:-}
while [ $# -gt 0 ]; do
  case "$1" in
    --with-binary)
      [ $# -ge 2 ] || { echo "--with-binary requires a binary path" >&2; exit 2; }
      BUNDLE_BIN=$2
      shift 2
      ;;
    --with-binary=*)
      BUNDLE_BIN=${1#*=}
      shift
      ;;
    --name)
      [ $# -ge 2 ] || { echo "--name requires an APK filename" >&2; exit 2; }
      APK_NAME=$2
      shift 2
      ;;
    --name=*)
      APK_NAME=${1#*=}
      shift
      ;;
    -h|--help)
      sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

if [ -z "$APK_NAME" ]; then
  if [ -n "$BUNDLE_BIN" ]; then APK_NAME=pumpkin-panel-full.apk
  else APK_NAME=pumpkin-panel-plain.apk
  fi
fi

if [ ! -f "$ANDROID_JAR" ]; then
  echo "android.jar not found at $ANDROID_JAR" >&2
  echo "download https://dl.google.com/android/repository/platform-34-ext7_r03.zip" >&2
  echo "then extract android-34/android.jar" >&2
  exit 1
fi
for tool in aapt2 d8 apksigner keytool javac zip; do
  command -v "$tool" >/dev/null || { echo "missing tool: $tool" >&2; exit 1; }
done

if [ -n "$BUNDLE_BIN" ]; then
  if [ ! -f "$BUNDLE_BIN" ]; then
    echo "binary not found: $BUNDLE_BIN" >&2
    exit 1
  fi
  if command -v readelf >/dev/null 2>&1; then
    ELF_HEADER=$(readelf -h "$BUNDLE_BIN" 2>/dev/null || true)
    ELF_PROG=$(readelf -l "$BUNDLE_BIN" 2>/dev/null || true)
    case "$ELF_HEADER" in
      *AArch64*) ;;
      *) echo "binary is not arm64/aarch64: $BUNDLE_BIN" >&2; exit 1 ;;
    esac
    case "$ELF_PROG" in
      *"/system/bin/linker64"*) ;;
      *)
        echo "binary is not an Android build (interpreter is not /system/bin/linker64)" >&2
        echo "use pumpkin-aarch64-android, not pumpkin-ARM64-Linux" >&2
        exit 1
        ;;
    esac
  fi
  BIN_MD5=$(md5sum "$BUNDLE_BIN" | cut -d' ' -f1)
  echo "binary: $BUNDLE_BIN ($(wc -c < "$BUNDLE_BIN" | tr -d ' ') bytes, md5 $BIN_MD5)"
fi

SRC=$ROOT/app/src/main
BUILD=$ROOT/build
OUT=$BUILD/apk
STAGE=$BUILD/assets
ASSET=$STAGE/index.html

rm -rf "$BUILD/res" "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$BUILD/assets"
rm -f "$BUILD/res.zip" "$BUILD/base.apk" "$BUILD/unsigned.apk" \
  "$BUILD/AndroidManifest.xml" "$BUILD/sources.txt" "$BUILD/classes.txt" "$BUILD/javac.log"
mkdir -p "$BUILD/res" "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$STAGE" "$OUT"

# The modular panel (panel/index.html + css/ + js/) is bundled into ONE HTML file.
# The APK stays self-contained; runtime never reads /sdcard.
# SKIP_PANEL_BUNDLE=1 reuses the existing app/src/main/assets/index.html.
if [ "${SKIP_PANEL_BUNDLE:-0}" = "1" ] && [ -f "$SRC/assets/index.html" ]; then
  cp "$SRC/assets/index.html" "$ASSET"
else
  bash "$ROOT/tools/bundle-panel.sh" "$ASSET"
fi
if [ ! -f "$ASSET" ]; then
  echo "assets/index.html not found — run tools/bundle-panel.sh" >&2
  exit 1
fi
echo "panel: $(wc -c < "$ASSET" | tr -d ' ') bytes, md5 $(md5sum "$ASSET" | cut -d' ' -f1)"

# The binary only goes into the build staging dir, never into source/git.
if [ -n "$BUNDLE_BIN" ]; then
  cp "$BUNDLE_BIN" "$STAGE/pumpkin-aarch64-android"
  chmod 0644 "$STAGE/pumpkin-aarch64-android"
  # The app reads this ID; extraction happens once per binary.
  printf '%s\n' "$BIN_MD5" > "$STAGE/bundled-bin-id"
fi

echo "[1/6] compile resources"
aapt2 compile --dir "$SRC/res" -o "$BUILD/res.zip"

echo "[2/6] link resources -> base.apk + R.java"
# AGP fills the package attribute from the namespace; plain aapt2 needs it in the manifest.
sed 's|<manifest |<manifest package="com.pumpkin.panel" |' \
  "$SRC/AndroidManifest.xml" > "$BUILD/AndroidManifest.xml"
aapt2 link \
  -o "$BUILD/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$BUILD/AndroidManifest.xml" \
  -A "$STAGE" \
  --java "$BUILD/gen" \
  --min-sdk-version "$MIN_SDK" \
  --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --auto-add-overlay \
  "$BUILD/res.zip"

echo "[3/6] javac"
find "$SRC/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
if ! javac -nowarn -Xlint:-options \
  -source 17 -target 17 \
  -classpath "$ANDROID_JAR" \
  -d "$BUILD/classes" \
  @"$BUILD/sources.txt" > "$BUILD/javac.log" 2>&1; then
  grep -v '^Note:' "$BUILD/javac.log" >&2 || true
  echo "javac failed" >&2
  exit 1
fi
grep -v '^Note:' "$BUILD/javac.log" || true

echo "[4/6] d8 -> classes.dex"
find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
d8 --release --min-api "$MIN_SDK" --lib "$ANDROID_JAR" \
  --output "$BUILD/dex" @"$BUILD/classes.txt"

echo "[5/6] package APK"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q -j "$BUILD/unsigned.apk" classes.dex)

echo "[6/6] sign"
if [ ! -f "$ROOT/debug.keystore" ]; then
  keytool -genkeypair -keystore "$ROOT/debug.keystore" -storepass android -keypass android \
    -alias debug -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Pumpkin Panel Debug,O=Android,C=US" >/dev/null 2>&1
fi
apksigner sign --ks "$ROOT/debug.keystore" --ks-pass pass:android --key-pass pass:android \
  --out "$OUT/$APK_NAME" "$BUILD/unsigned.apk"
apksigner verify --verbose "$OUT/$APK_NAME" 2>&1 | sed -n '1,6p'

if [ -n "$BUNDLE_BIN" ]; then
  echo "binary in APK:"
  unzip -l "$OUT/$APK_NAME" assets/pumpkin-aarch64-android | sed -n '4p'
fi

ls -lh "$OUT/$APK_NAME"
echo "OK -> $OUT/$APK_NAME"

# Copy to Download when running on a device (CI has no /sdcard).
if [ -d /sdcard/Download ]; then
  cp -f "$OUT/$APK_NAME" "/sdcard/Download/$APK_NAME" && echo "copied -> /sdcard/Download/$APK_NAME"
fi
