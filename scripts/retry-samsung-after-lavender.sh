#!/usr/bin/env bash
set -u
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
STATE="$ROOT/.state"
mkdir -p "$STATE"
exec >> "$STATE/retry-samsung.log" 2>&1
printf '[%s] waiting for lavender/unattended service\n' "$(date -Is)"
while systemctl --user is-active --quiet xos-unattended-builds.service; do
    sleep 60
done
printf '[%s] starting Samsung retry\n' "$(date -Is)"
export BUILD_JOBS=1 SYNC_JOBS=2
bash "$ROOT/scripts/build-rom.sh"
rc=$?
printf '%s\n' "$rc" > "$STATE/samsung-retry.exit"
cd "$ROOT"
python3 "$ROOT/scripts/publish-build-status.py"
git add docs/BUILD-STATUS.md
git add -u
if ! git diff --cached --quiet; then
    git commit -m 'Record unattended Samsung retry result' || true
    git push origin main || true
fi
exit "$rc"
