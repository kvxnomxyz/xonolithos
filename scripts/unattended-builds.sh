#!/usr/bin/env bash
# Sequential unattended builds for the 16 GiB host. Full Android builds are
# deliberately serialized so they share the prepared swap instead of competing
# for memory. Build outputs remain local; tracked source changes are pushed.
set -u
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
STATE="$ROOT/.state"
mkdir -p "$STATE"
exec >> "$STATE/unattended-builds.log" 2>&1
printf '\n[%s] unattended sequence started\n' "$(date -Is)"

push_tracked() {
    cd "$ROOT"
    git add -u
    if ! git diff --cached --quiet; then
        git commit -m "Record unattended build updates" || true
        git push origin main || true
    fi
}

run_stage() {
    local name=$1
    shift
    printf '[%s] starting %s\n' "$(date -Is)" "$name"
    "$@"
    local rc=$?
    printf '%s\n' "$rc" > "$STATE/unattended-${name}.exit"
    printf '[%s] %s finished with exit %s\n' "$(date -Is)" "$name" "$rc"
    push_tracked
    return "$rc"
}

export BUILD_JOBS=1 SYNC_JOBS=2
run_stage samsung bash "$ROOT/scripts/build-rom.sh" || true
run_stage lavender bash "$ROOT/scripts/build-lavender.sh" || true
printf '[%s] unattended sequence finished\n' "$(date -Is)"
push_tracked
