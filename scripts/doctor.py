#!/usr/bin/env python3
"""Report real host capacity; compressed zram is not additional physical capacity."""
import argparse
import json
import os
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]

def inspect():
    mem = {k: int(v.strip().split()[0]) * 1024 for k, v in
           (line.split(":", 1) for line in Path('/proc/meminfo').read_text().splitlines())}
    disk_swap = sum(int(line.split()[2]) * 1024 for line in
                    Path('/proc/swaps').read_text().splitlines()[1:]
                    if not line.split()[0].startswith('/dev/zram'))
    gib = 1024 ** 3
    return dict(ram_gib=round(mem['MemTotal']/gib, 1),
                available_ram_gib=round(mem['MemAvailable']/gib, 1),
                disk_swap_gib=round(disk_swap/gib, 1),
                ram_plus_disk_swap_gib=round((mem['MemTotal']+disk_swap)/gib, 1),
                free_disk_gib=round(shutil.disk_usage(ROOT).free/gib, 1),
                cpus=os.cpu_count(),
                sufficient_memory=mem['MemTotal']+disk_swap >= 60*gib)

if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--require-build', action='store_true')
    a = p.parse_args()
    info = inspect()
    print(json.dumps(info, indent=2))
    if a.require_build and not info['sufficient_memory']:
        raise SystemExit('Need at least 60 GiB RAM + disk-backed swap. Zram is excluded.')
