#!/usr/bin/env python3
"""Fetch/verify the reviewed APK lock; fetching never updates the lock implicitly."""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]

def entries():
    return json.loads((ROOT/'prebuilts/apps.lock.json').read_text())['apps']

def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024*1024), b''):
            h.update(chunk)
    return h.hexdigest()

def verify():
    for app in entries():
        path = ROOT/'prebuilts'/app['filename']
        if not path.is_file() or digest(path) != app['sha256']:
            raise ValueError(f"Missing or changed APK: {path}; run scripts/apps.py fetch")
        print(f"Verified {app['module']} {app['version']}")

def fetch():
    for app in entries():
        target = ROOT/'prebuilts'/app['filename']
        if target.exists() and digest(target) == app['sha256']:
            continue
        if not app['url'].startswith('https://'):
            raise ValueError('Only HTTPS downloads are accepted')
        temporary = target.with_suffix('.part')
        try:
            request = urllib.request.Request(app['url'], headers={'User-Agent':'XonolithOS-build/0.1'})
            with urllib.request.urlopen(request, timeout=60) as response, temporary.open('wb') as f:
                if not response.url.startswith('https://'):
                    raise ValueError('HTTPS downgrade refused')
                size = 0
                for chunk in iter(lambda: response.read(1024*1024), b''):
                    size += len(chunk)
                    if size > 512*1024*1024:
                        raise ValueError('APK exceeds 512 MiB limit')
                    f.write(chunk)
            if digest(temporary) != app['sha256']:
                raise ValueError('APK checksum mismatch: '+app['module'])
            temporary.replace(target)
        finally:
            temporary.unlink(missing_ok=True)
    verify()

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['fetch','verify'])
    args = parser.parse_args()
    try:
        fetch() if args.action == 'fetch' else verify()
    except (OSError, ValueError) as e:
        parser.exit(1, str(e)+'\n')
