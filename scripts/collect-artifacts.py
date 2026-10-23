#!/usr/bin/env python3
"""Collect only the current xOS development build, never publish it."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def collect(tree):
    output = tree/'out/target/product/gts4lv'
    props = {}
    for f in output.rglob('build.prop'):
        for line in f.read_text(errors='replace').splitlines():
            if '=' in line and not line.startswith('#'):
                k, v = line.split('=',1)
                props[k] = v
    if props.get('ro.xos.short_name') != 'xOS' or props.get('ro.xos.name') != 'XonolithOS':
        raise ValueError('Built product is not branded as xOS')
    if props.get('ro.build.version.release') != '17':
        raise ValueError('Built product is not Android 17')
    version = props.get('ro.lineage.version','')
    if 'xOS-0.1.0-dev' not in version or not version.endswith('-gts4lv'):
        raise ValueError('Unexpected build version '+version)
    source_zip = output/('lineage-'+version+'.zip')
    if not source_zip.is_file():
        raise ValueError('Current build ZIP is missing')
    with zipfile.ZipFile(source_zip) as z:
        metadata = z.read('META-INF/com/android/metadata').decode()
        fields = dict(line.split('=',1) for line in metadata.splitlines() if '=' in line)
        if 'gts4lv' not in fields.get('pre-device','').split('|'):
            raise ValueError('OTA ZIP has the wrong device assertion')
    destination = ROOT/'artifacts'/('xOS-'+version)
    destination.mkdir(parents=True,exist_ok=True)
    files = [source_zip, output/'recovery.img', output/'vbmeta.img']
    for file in files:
        if not file.is_file():
            raise ValueError('Missing build artifact '+str(file))
        shutil.copy2(file,destination/file.name)
    sums=[]
    for file in files:
        h=hashlib.sha256()
        with (destination/file.name).open('rb') as stream:
            for block in iter(lambda:stream.read(1024*1024),b''):h.update(block)
        sums.append(h.hexdigest()+'  '+file.name)
    (destination/'SHA256SUMS').write_text('\n'.join(sums)+'\n')
    shutil.copy2(ROOT/'artifacts/manifest-before-xos.xml',destination/'source-manifest.xml')
    shutil.copy2(ROOT/'prebuilts/apps.lock.json',destination/'apps.lock.json')
    (destination/'BUILD-STATUS.json').write_text(json.dumps({
        'product':'XonolithOS','short_name':'xOS','device':'gts4lv','model':'SM-T725',
        'android':'17','lineage':'24.0','channel':'development-test-keys',
        'compiled':True,'boot_tested':False,'hardware_tested':False,'release_ready':False,
        'pending':['Bitpost Account Network specification','compatible GCam selection',
                   'LTE and hardware tests','release signing and redistribution review']},indent=2)+'\n')
    print(destination)

if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('tree',type=Path)
    try:collect(parser.parse_args().tree)
    except (ValueError,OSError,KeyError,zipfile.BadZipFile) as e:parser.exit(1,str(e)+'\n')
