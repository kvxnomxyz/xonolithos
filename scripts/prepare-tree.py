#!/usr/bin/env python3
"""Install xOS additions into an already-synced tree, refusing unrelated edits."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'scripts'))
import apps

def digest_bytes(data):
    return hashlib.sha256(data).hexdigest()

def install(tree):
    if not (tree/'build/envsetup.sh').is_file():
        raise ValueError('Not a synced Android source tree')
    apps.verify()
    lock = json.loads((ROOT/'manifests/sources.lock.json').read_text())
    for project in lock['projects']:
        path = tree/project['path']
        actual = subprocess.check_output(['git','-C',str(path),'rev-parse','HEAD'], text=True).strip()
        if actual != project['commit']:
            raise ValueError('Unreviewed source revision at '+project['path'])
    destination = tree/'vendor/extra'
    marker = destination/'.xos-managed'
    if destination.exists() and not marker.exists():
        raise ValueError('vendor/extra already exists and is not managed by xOS')
    shutil.copytree(ROOT/'vendor/extra', destination, dirs_exist_ok=True)
    marker.write_text('Managed by XonolithOS/scripts/prepare-tree.py\n')
    shutil.copytree(ROOT/'apps/Xstore', destination/'apps/Xstore',
                    ignore=shutil.ignore_patterns('build'), dirs_exist_ok=True)
    prebuilts = destination/'prebuilts'
    prebuilts.mkdir(exist_ok=True)
    modules = []
    definitions = ['// Generated from prebuilts/apps.lock.json. Preserve upstream signatures.\n']
    for app in apps.entries():
        module = 'xOS'+app['module']
        modules.append(module)
        shutil.copy2(ROOT/'prebuilts'/app['filename'], prebuilts/app['filename'])
        definitions.append('android_app_import {\n'
            f'    name: "{module}",\n    apk: "{app["filename"]}",\n'
            '    presigned: true,\n    preprocessed: true,\n'
            '    product_specific: true,\n    dex_preopt: { enabled: false },\n'
            '    enforce_uses_libs: false,\n}\n')
    (prebuilts/'Android.bp').write_text('\n'.join(definitions))
    (prebuilts/'prebuilts.mk').write_text('PRODUCT_PACKAGES += '+' '.join(modules)+'\n')
    # Record original/modified hashes. Re-runs accept only the upstream or our own version.
    state_file = tree/'.repo/xos-patches.json'
    state = json.loads(state_file.read_text()) if state_file.exists() else {}
    changes = {
        'device/samsung/gts4lv-common/system.prop': lambda s: re.sub(r'^lineage\.updater\.uri=.*\n?', '', s, flags=re.M),
        'vendor/lineage/config/common.mk': lambda s: re.sub(r'^    Updater\s*$', '    Xstore', s, flags=re.M),
    }
    # No third-party OTA service is inherited. Xstore is a normal app, not an OS updater.
    for relative, transform in changes.items():
        path = tree/relative
        before = path.read_bytes()
        repo_path = tree/('device/samsung/gts4lv-common' if relative.startswith('device/') else 'vendor/lineage')
        rel = str(path.relative_to(repo_path))
        original = subprocess.check_output(['git','-C',str(repo_path),'show','HEAD:'+rel])
        expected = transform(original.decode()).encode()
        if original == expected:
            raise ValueError('Expected upstream patch anchor is missing in '+relative)
        if before not in (original, expected):
            raise ValueError('Refusing to overwrite unrelated local changes: '+relative)
        path.write_bytes(expected)
        state[relative] = {'upstream_sha256':digest_bytes(original), 'xos_sha256':digest_bytes(expected)}
    state_file.write_text(json.dumps(state, indent=2)+'\n')
    print('xOS integration applied; no device partitions were touched.')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('tree', type=Path)
    try:
        install(parser.parse_args().tree.resolve())
    except (ValueError, OSError, subprocess.CalledProcessError) as e:
        parser.exit(1, str(e)+'\n')
