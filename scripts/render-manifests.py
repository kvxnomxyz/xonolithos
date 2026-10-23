#!/usr/bin/env python3
"""Render upstream local manifests at the exact audited project commits."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

def render(destination):
    destination.mkdir(parents=True, exist_ok=True)
    lock = json.loads((ROOT/'manifests/sources.lock.json').read_text())
    pins = {p['path']: p for p in lock['projects']}
    seen = set()
    for source in sorted((ROOT/'manifests/gts4lv').glob('*.xml')):
        tree = ET.parse(source)
        for project in tree.findall('project'):
            path = project.get('path')
            if path in seen:
                raise ValueError('Duplicate project path: '+path)
            seen.add(path)
            pin = pins[path]
            if pin['name'] != project.get('name') or pin['branch'] != project.get('revision'):
                raise ValueError('Source lock disagrees with manifest: '+path)
            project.set('revision', pin['commit'])
            project.set('upstream', 'refs/heads/'+pin['branch'])
        ET.indent(tree)
        tree.write(destination/('xos-'+source.name), encoding='utf-8', xml_declaration=True)
    if seen != set(pins):
        raise ValueError('Source lock contains unused projects')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('destination', type=Path)
    render(parser.parse_args().destination)
