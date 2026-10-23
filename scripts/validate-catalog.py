#!/usr/bin/env python3
"""Check the exact catalog contract consumed by Xstore; never download or publish."""
import argparse
import hashlib
from pathlib import Path
import re
from urllib.parse import urlparse

def validate(root):
    errors=[];count=0
    for directory in sorted(root.iterdir()):
        if not directory.is_dir() or directory.name.startswith('.'):
            continue
        if not all((directory/name).is_file() for name in ('title.txt','author.txt','description.txt')):
            continue
        count+=1
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,79}',directory.name):
            errors.append(f'{directory.name}: unsupported folder name')
        values={}
        for name,limit in [('title.txt',512),('author.txt',512),('description.txt',32768),
                           ('apk.txt',2048),('sha256.txt',128),('package.txt',256)]:
            path=directory/name
            if not path.exists():continue
            try:
                if path.stat().st_size>limit:raise ValueError(f'over {limit} bytes')
                value=path.read_text(encoding='utf-8').strip()
                if not value:raise ValueError('empty')
                values[name]=value
            except (UnicodeError,OSError,ValueError) as e:errors.append(f'{directory.name}/{name}: {e}')
        install=any((directory/n).exists() for n in ('apk.txt','app.apk','sha256.txt','package.txt'))
        if install:
            if not (directory/'app.apk').is_file() and 'apk.txt' not in values:
                errors.append(f'{directory.name}: missing APK source')
            if 'apk.txt' in values:
                url=urlparse(values['apk.txt'])
                if url.scheme!='https' or not url.netloc or url.username or url.password:
                    errors.append(f'{directory.name}: apk.txt must be HTTPS without credentials')
            if not re.fullmatch('[a-fA-F0-9]{64}',values.get('sha256.txt','')):
                errors.append(f'{directory.name}: sha256.txt must contain a SHA-256 digest')
            if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+',values.get('package.txt','')):
                errors.append(f'{directory.name}: missing or invalid package.txt')
            apk=directory/'app.apk'
            if apk.exists() and 'apk.txt' not in values:
                if apk.stat().st_size>512*1024*1024:
                    errors.append(f'{directory.name}: APK exceeds 512 MiB')
                else:
                    h=hashlib.sha256()
                    with apk.open('rb') as stream:
                        for block in iter(lambda:stream.read(1024*1024),b''):h.update(block)
                    if h.hexdigest()!=values.get('sha256.txt','').lower():
                        errors.append(f'{directory.name}: APK checksum does not match')
    if count>100:errors.append('Catalog exceeds the initial 100-app limit')
    return count,errors

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('root',type=Path)
    root=parser.parse_args().root
    if not root.is_dir():parser.exit(1,'Catalog directory not found\n')
    count,errors=validate(root)
    for error in errors:print(error)
    print(f'{count} app folders; {len(errors)} errors')
    raise SystemExit(bool(errors))
