#!/usr/bin/env bash
# Build Redmi Note 7 from pinned sources with an independent output directory.
set -eo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
TREE="$ROOT/android"
mkdir -p "$ROOT/.state" "$ROOT/artifacts"
rm -f "$ROOT/.state/lavender-build.exit"
exec >> "$ROOT/.state/lavender-build.log" 2>&1
status() {
    python3 - "$ROOT/.state/lavender-status.json" "$1" "${2:-}" <<'PY'
import datetime, json, pathlib, sys
p = pathlib.Path(sys.argv[1]); tmp = p.with_suffix('.tmp')
tmp.write_text(json.dumps({'stage': sys.argv[2], 'detail': sys.argv[3], 'updated_utc': datetime.datetime.now(datetime.timezone.utc).isoformat()}, indent=2) + '\n')
tmp.replace(p)
PY
}
finish() {
    rc=$?
    printf '%s\n' "$rc" > "$ROOT/.state/lavender-build.exit"
    if (( rc != 0 )); then status failed "exit $rc; inspect .state/lavender-build.log"; fi
}
trap finish EXIT
trap 'exit 143' TERM INT
export TARGET_UNOFFICIAL_BUILD_ID=xOS-0.1.0-dev
export BUILD_USERNAME=xos BUILD_HOSTNAME=xos-builder USE_CCACHE=0
unset WITH_GMS WITH_GAPPS WITH_MICROG
status preflight 'Starting lavender alongside the Samsung branding build'
if [[ $(df --output=avail -BG "$ROOT" | tail -1 | tr -dc '0-9') -lt 90 ]]; then
    echo 'Need at least 90 GiB free for the first lavender build.' >&2
    exit 1
fi
cd "$TREE"
status syncing 'Fetching pinned lavender device, vendor, kernel and hardware trees'
python3 "$ROOT/scripts/render-lavender-manifest.py" "$TREE/.repo/local_manifests"
mapfile -t paths < <(python3 - "$ROOT/manifests/lavender/sources.lock.json" <<'PY'
import json, sys
for project in json.load(open(sys.argv[1]))['projects']:
    print(project['path'])
PY
)
"$TREE/.repo/repo/repo" sync -c -j2 --no-tags --no-clone-bundle "${paths[@]}"
status integrating 'Applying xOS apps and branding to lavender'
python3 "$ROOT/scripts/prepare-tree.py" "$TREE"
"$TREE/.repo/repo/repo" manifest -r -o "$ROOT/artifacts/manifest-lavender.xml"
status compiling 'LineageOS 24 / Android 17; Redmi Note 7 development build'
export OUT_DIR="$TREE/out-lavender"
source build/envsetup.sh
breakfast lavender
mka bacon -j2
ROM=$(find "$OUT_DIR/target/product/lavender" -maxdepth 1 -type f -name '*lavender.zip' -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)
[[ -n "$ROM" ]] || { echo 'Build returned success but no lavender ROM ZIP was found.' >&2; exit 1; }
sha256sum "$ROM" > "$ROOT/artifacts/lavender-rom.sha256"
status built 'Compilation succeeded; image has not been flashed or tested on a Redmi Note 7'
