#!/usr/bin/env bash
set -eo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
TREE="$ROOT/android"
mkdir -p "$ROOT/.state" "$ROOT/artifacts" "$HOME"
exec > >(tee -a "$ROOT/.state/build.log") 2>&1
status() {
    python3 - "$ROOT/.state/status.json" "$1" "${2:-}" <<'PY'
import json,sys,datetime,pathlib
p=pathlib.Path(sys.argv[1]); tmp=p.with_suffix('.tmp')
tmp.write_text(json.dumps({'stage':sys.argv[2], 'detail':sys.argv[3], 'updated_utc':datetime.datetime.now(datetime.timezone.utc).isoformat()},indent=2)+'\n');tmp.replace(p)
PY
}
trap 'rc=$?; status failed "exit $rc at line $LINENO; inspect .state/build.log"; exit "$rc"' ERR
trap 'status stopped "container received stop signal"; exit 143' TERM INT
export TZ=UTC
export TARGET_UNOFFICIAL_BUILD_ID=xOS-0.1.0-dev
unset WITH_GMS WITH_GAPPS WITH_MICROG
export BUILD_USERNAME=xos BUILD_HOSTNAME=xos-builder
export USE_CCACHE=0
status preflight
python3 "$ROOT/scripts/doctor.py" --require-build
python3 "$ROOT/scripts/apps.py" verify
if [[ $(df --output=avail -BG "$ROOT" | tail -1 | tr -dc '0-9') -lt 40 ]]; then
    echo 'Need at least 40 GiB free to resume.' >&2; exit 1
fi
git config --global user.name 'XonolithOS Local Builder'
git config --global user.email 'build@xonolithos.invalid'
git config --global trailer.changeid.key Change-Id
git lfs install --skip-repo
mkdir -p "$TREE"
cd "$TREE"
status initializing 'LineageOS 24.0 / Android 17, gts4lv'
MANIFEST_COMMIT=$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["lineage"]["commit"])' "$ROOT/manifests/sources.lock.json")
repo init -u https://github.com/LineageOS/android.git -b "$MANIFEST_COMMIT" \
    --git-lfs --no-clone-bundle --depth=1
python3 "$ROOT/scripts/render-manifests.py" "$TREE/.repo/local_manifests"
status syncing 'Downloading source; resumable, limited to 4 jobs'
repo sync -c -j"${SYNC_JOBS:-4}" --no-tags --no-clone-bundle --fail-fast
repo manifest -r -o "$ROOT/artifacts/manifest-before-xos.xml"
status integrating
python3 "$ROOT/scripts/prepare-tree.py" "$TREE"
status compiling 'Development build, 2 jobs by default; test keys, not a release'
# Android's envsetup is not compatible with shell nounset.
source build/envsetup.sh
breakfast gts4lv
mka bacon -j"${BUILD_JOBS:-2}"
status collecting
python3 "$ROOT/scripts/collect-artifacts.py" "$TREE"
status built 'Compilation succeeded. Not flashed or hardware-tested; not a public release.'
