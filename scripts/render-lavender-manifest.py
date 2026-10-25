#!/usr/bin/env python3
"""Write the pinned lavender projects into the existing Android checkout."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / 'manifests/lavender/sources.lock.json'

def render(destination):
    projects = json.loads(LOCK.read_text())['projects']
    manifest = ET.Element('manifest')
    ET.SubElement(manifest, 'remote', name='pix106', fetch='https://github.com/pix106')
    paths = set()
    for project in projects:
        path = project['path']
        if path in paths:
            raise ValueError(f'Duplicate path: {path}')
        paths.add(path)
        attrs = {key: project[key] for key in ('path', 'name', 'remote')}
        attrs['revision'] = project['commit']
        attrs['upstream'] = 'refs/heads/' + project['branch']
        ET.SubElement(manifest, 'project', **attrs)
    destination.mkdir(parents=True, exist_ok=True)
    ET.indent(manifest)
    ET.ElementTree(manifest).write(destination/'xos-lavender.xml', encoding='utf-8', xml_declaration=True)

if __name__ == '__main__':
    render(Path(sys.argv[1]))
