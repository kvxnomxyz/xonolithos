import importlib.util
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]

def module(name):
    spec=importlib.util.spec_from_file_location(name,ROOT/'scripts'/(name+'.py'))
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result

class CatalogValidation(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.app=self.root/'notes';self.app.mkdir()
        for key,value in {'title':'Notes','author':'Author','description':'Plain text'}.items():
            (self.app/(key+'.txt')).write_text(value)
        self.validate=module('validate-catalog').validate

    def test_requested_metadata_only_format(self):
        self.assertEqual(self.validate(self.root),(1,[]))

    def test_incomplete_install_entry_is_rejected(self):
        (self.app/'apk.txt').write_text('https://example.org/app.apk')
        _,errors=self.validate(self.root)
        self.assertTrue(any('sha256' in e for e in errors))
        self.assertTrue(any('package.txt' in e for e in errors))

    def test_tampering_and_plain_http_rejected(self):
        (self.app/'app.apk').write_bytes(b'original')
        (self.app/'sha256.txt').write_text(hashlib.sha256(b'original').hexdigest())
        (self.app/'package.txt').write_text('org.example.notes')
        self.assertEqual(self.validate(self.root),(1,[]))
        (self.app/'app.apk').write_bytes(b'changed')
        self.assertTrue(any('checksum' in e for e in self.validate(self.root)[1]))
        (self.app/'apk.txt').write_text('http://example.org/app.apk')
        self.assertTrue(any('HTTPS' in e for e in self.validate(self.root)[1]))

class ManifestValidation(unittest.TestCase):
    def test_samsung_manifest_lock_and_lte_variant(self):
        with tempfile.TemporaryDirectory() as name:
            destination=Path(name);module('render-manifests').render(destination)
            projects=[p for f in destination.glob('*.xml') for p in ET.parse(f).findall('project')]
            paths={p.get('path') for p in projects}
            self.assertIn('device/samsung/gts4lv',paths)
            self.assertNotIn('device/samsung/gts4lvwifi',paths)
            for p in projects:
                self.assertRegex(p.get('revision'),r'^[a-f0-9]{40}$')
                self.assertTrue(p.get('upstream').startswith('refs/heads/'))

if __name__=='__main__':unittest.main()
