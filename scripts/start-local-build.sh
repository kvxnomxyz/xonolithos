#!/usr/bin/env bash
# Run using pkexec or sudo. Changes: extra temporary swap, Docker image/container.
# No fstab edits, group changes, uploads, device flashing, or source deletion.
set -euo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
if (( EUID != 0 )); then
    echo "Run: pkexec bash $ROOT/scripts/start-local-build.sh" >&2
    exit 1
fi
OWNER_UID=$(stat -c %u "$ROOT")
OWNER_GID=$(stat -c %g "$ROOT")
(( OWNER_UID != 0 )) || { echo 'The project must belong to your regular user.' >&2; exit 1; }
command -v docker >/dev/null
command -v swapon >/dev/null
bash "$ROOT/scripts/prepare-swap.sh"
python3 "$ROOT/scripts/doctor.py" --require-build
systemctl start docker
if docker container inspect xos-gts4lv >/dev/null 2>&1; then
    if [[ $(docker inspect --format '{{.State.Running}}' xos-gts4lv) == true ]]; then
        echo 'xos-gts4lv is already running. Read .state/build.log for progress.'
        exit 0
    fi
    # Preserve the stopped container and its logs instead of deleting it.
    docker rename xos-gts4lv "xos-gts4lv-previous-$(date +%s)"
fi
docker build --tag xos-builder:24.0 --build-arg "BUILDER_UID=$OWNER_UID" \
    --build-arg "BUILDER_GID=$OWNER_GID" "$ROOT/build"
install -d -o "$OWNER_UID" -g "$OWNER_GID" "$ROOT/.state" "$ROOT/.cache" "$ROOT/android" "$ROOT/artifacts"
docker image inspect xos-builder:24.0 --format '{{.Id}}' > "$ROOT/.state/builder-image.txt"
chown "$OWNER_UID:$OWNER_GID" "$ROOT/.state/builder-image.txt"
docker run --detach --name xos-gts4lv --init \
    --user "$OWNER_UID:$OWNER_GID" \
    --mount "type=bind,source=$ROOT,target=/workspace" \
    --env HOME=/workspace/.cache/build-home \
    --env USER=xos --env BUILD_JOBS="${BUILD_JOBS:-2}" --env SYNC_JOBS=4 \
    --security-opt no-new-privileges \
    xos-builder:24.0
echo 'Build launched. Progress: .state/build.log and .state/status.json'
echo 'Stop: pkexec docker stop --time 60 xos-gts4lv'
echo 'Extra swap is temporary (not added to fstab); re-run this script after reboot.'
