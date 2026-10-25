#!/usr/bin/env bash
# Incrementally rebuild the xOS boot animation and visible system labels.
set -eo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
mkdir -p "$ROOT/.state"
rm -f "$ROOT/.state/branding-build.exit"
exec >> "$ROOT/.state/branding-build.log" 2>&1
trap 'rc=$?; printf "%s\n" "$rc" > "$ROOT/.state/branding-build.exit"; exit "$rc"' EXIT
export TARGET_UNOFFICIAL_BUILD_ID=xOS-0.1.0-dev
export BUILD_USERNAME=xos BUILD_HOSTNAME=xos-builder USE_CCACHE=0
cd "$ROOT/android"
source build/envsetup.sh
breakfast gts4lv
m -j2 bootanimation.zip org.lineageos.platform-res LineageParts Settings
