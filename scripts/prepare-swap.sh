#!/usr/bin/env bash
# Allocate 22 x 2 GiB instead of a single file on multi-device Btrfs.
# Existing system swap is never disabled, resized, moved or deleted.
set -euo pipefail
(( EUID == 0 )) || { echo 'Swap setup requires root.' >&2; exit 1; }
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
DIRECTORY=/swap/xos-build
[[ ! -L $DIRECTORY ]] || { echo 'Refusing symlink swap directory.' >&2; exit 1; }
[[ -d /swap ]] || install -d -m 700 /swap
# Remove only the failed, inactive 48 GiB file made by this setup's first attempt.
OLD=/swap/xos-build.swap
if [[ -f $OLD && ! -L $OLD ]] && ! swapon --noheadings --raw --show=NAME | grep -Fxq "$OLD"; then
    if [[ $(stat -c %u:%a:%s "$OLD") == "0:600:51539607552" ]]; then
        rm -- "$OLD"
    fi
fi
if [[ ! -d $DIRECTORY ]]; then
    FREE=$(df --output=avail -B1 "$ROOT" | tail -1 | tr -d ' ')
    (( FREE > 400 * 1024 * 1024 * 1024 )) || { echo 'Need >400 GiB free for initial setup.' >&2; exit 1; }
    if [[ $(stat -f -c %T /swap) == btrfs ]]; then
        btrfs subvolume create "$DIRECTORY"
    else
        mkdir "$DIRECTORY"
    fi
    chmod 700 "$DIRECTORY"
fi
[[ $(stat -c %u:%a "$DIRECTORY") == 0:700 ]] || { echo 'Swap directory must be root-owned and mode 700.' >&2; exit 1; }
active=$(swapon --noheadings --raw --show=NAME | awk '/^\/swap\/xos-build\/swap-[0-9]+$/ {n++} END {print n+0}')
if (( active >= 22 )); then
    for index in $(seq -w 1 48); do
        file="$DIRECTORY/swap-$index"
        if [[ -f $file && ! -L $file ]] &&
            [[ $(stat -c %u:%a:%s "$file") == 0:600:2147483648 ]] &&
            ! swapon --noheadings --raw --show=NAME | grep -Fxq "$file"; then
            rm -- "$file"
        fi
    done
    echo 'The extra xOS disk-backed swap is already active.'
    exit 0
fi
rejected=()
# Keep rejected allocations until the end, so Btrfs can try different extents.
# Never rebalance or repartition the filesystem to make swap fit.
for index in $(seq -w 1 48); do
    file="$DIRECTORY/swap-$index"
    [[ ! -L $file ]] || { echo 'Refusing symlink swap file.' >&2; exit 1; }
    if [[ ! -f $file ]]; then
        if [[ $(stat -f -c %T "$DIRECTORY") == btrfs ]]; then
            btrfs filesystem mkswapfile --size 2G "$file"
        else
            (umask 077; fallocate -l 2G "$file")
            mkswap "$file"
        fi
        chmod 600 "$file"
    fi
    [[ $(stat -c %u:%a:%s "$file") == 0:600:2147483648 ]] || { echo "Unexpected file: $file" >&2; exit 1; }
    if ! swapon --noheadings --raw --show=NAME | grep -Fxq "$file"; then
        if [[ $(stat -f -c %T "$DIRECTORY") == btrfs ]] &&
            ! btrfs inspect-internal map-swapfile "$file" >/dev/null 2>&1; then
            rejected+=("$file")
            continue
        fi
        swapon --priority 10 "$file"
        active=$((active+1))
    fi
    (( active >= 22 )) && break
done
for file in "${rejected[@]}"; do
    # Only our own inactive allocation failures are removed.
    if ! swapon --noheadings --raw --show=NAME | grep -Fxq "$file"; then rm -- "$file"; fi
done
(( active >= 22 )) || { echo "Only $active of 22 swap files could be placed on one device; build not launched." >&2; exit 1; }
echo '44 GiB extra disk-backed swap active in /swap/xos-build (temporary until reboot).'
