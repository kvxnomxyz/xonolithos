#!/usr/bin/env bash
# Standalone SDK build without Gradle or external libraries. ROM builds use Android.bp.
set -euo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}
TOOLS="$SDK/build-tools/35.0.0"
ANDROID_JAR="$SDK/platforms/android-35/android.jar"
APP="$ROOT/apps/Xstore"
OUT="$APP/build"
mkdir -p "$OUT" "$ROOT/artifacts" "$ROOT/.cache"
for cmd in javac jar keytool zip; do command -v "$cmd" >/dev/null; done
[[ -f $ANDROID_JAR && -x $TOOLS/aapt2 ]] || { echo 'Need Android SDK platform 35 and build-tools 35.0.0.' >&2; exit 1; }
STAGE=$(mktemp -d "$OUT/staging.XXXXXX")
trap 'rm -rf -- "$STAGE"' EXIT
mkdir -p "$STAGE/gen" "$STAGE/classes" "$STAGE/dex"
"$TOOLS/aapt2" compile --dir "$APP/res" -o "$STAGE/resources.zip"
"$TOOLS/aapt2" link -o "$STAGE/app.apk" -I "$ANDROID_JAR" \
    --manifest "$APP/AndroidManifest.xml" --java "$STAGE/gen" "$STAGE/resources.zip"
find "$APP/src" "$STAGE/gen" -name '*.java' -print > "$STAGE/sources.txt"
javac -source 8 -target 8 -Xlint:-options -bootclasspath "$ANDROID_JAR:$TOOLS/core-lambda-stubs.jar" \
    -d "$STAGE/classes" @"$STAGE/sources.txt"
jar cf "$STAGE/classes.jar" -C "$STAGE/classes" .
"$TOOLS/d8" --min-api 26 --lib "$ANDROID_JAR" --output "$STAGE/dex" "$STAGE/classes.jar"
(cd "$STAGE/dex" && zip -q "$STAGE/app.apk" classes*.dex)
"$TOOLS/zipalign" -P 16 -f 4 "$STAGE/app.apk" "$STAGE/aligned.apk"
KEY="$ROOT/.cache/xstore-development.jks"
if [[ ! -f $KEY ]]; then
    (umask 077; keytool -genkeypair -keystore "$KEY" -alias xstore-development \
        -storepass android -keypass android -keyalg RSA -keysize 3072 -validity 3650 \
        -dname 'CN=Xstore Development,O=XonolithOS' >/dev/null)
fi
"$TOOLS/apksigner" sign --ks "$KEY" --ks-key-alias xstore-development \
    --ks-pass pass:android --key-pass pass:android \
    --out "$ROOT/artifacts/Xstore-0.1.0-dev.apk" "$STAGE/aligned.apk"
"$TOOLS/apksigner" verify --verbose "$ROOT/artifacts/Xstore-0.1.0-dev.apk"
echo "Built $ROOT/artifacts/Xstore-0.1.0-dev.apk (development signature; ROM signs separately)"
