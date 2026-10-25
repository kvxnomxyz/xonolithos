#!/usr/bin/env python3
"""Make a signed gts4lv recovery ZIP from the incremental xOS feature build."""
import hashlib
from pathlib import Path
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TREE = ROOT / 'android'
PRODUCT = TREE / 'out/target/product/gts4lv'
BASE_OTA = PRODUCT / 'lineage_gts4lv-ota.zip'
OUTPUT = ROOT / 'artifacts/xOS-0.1.0-dev-gts4lv-features-v1.zip'
CERT = TREE / 'build/target/product/security/testkey.x509.pem'
KEY = TREE / 'build/target/product/security/testkey.pk8'
SIGNAPK = TREE / 'out/host/linux-x86/framework/signapk.jar'
FILES = {
    'product/media/bootanimation.zip': PRODUCT / 'system/product/media/bootanimation.zip',
    'product/app/Xstore/Xstore.apk': PRODUCT / 'system/product/app/Xstore/Xstore.apk',
    'product/priv-app/XonolithSystemCenter/XonolithSystemCenter.apk':
        PRODUCT / 'system/product/priv-app/XonolithSystemCenter/XonolithSystemCenter.apk',
    'product/etc/permissions/privapp-permissions-xonolithos.xml':
        ROOT / 'vendor/extra/permissions/privapp-permissions-xonolithos.xml',
}


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as src:
        for block in iter(lambda: src.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def main():
    completed = ROOT / '.state/features-build.exit'
    if not completed.is_file() or completed.read_text().strip() != '0':
        raise SystemExit('The targeted feature build has not succeeded')
    for path in (BASE_OTA, CERT, KEY, SIGNAPK, *FILES.values()):
        if not path.is_file():
            raise SystemExit(f'Missing prerequisite: {path}')
    buildprop = PRODUCT / 'system/product/etc/build.prop'
    props = dict(line.split('=', 1) for line in buildprop.read_text().splitlines()
                 if '=' in line and not line.startswith('#'))
    if props.get('ro.xos.version') != '0.1.0-dev':
        raise SystemExit('Unexpected xOS build version')
    lineage_version = props['ro.lineage.version']
    if not lineage_version.endswith('-gts4lv'):
        raise SystemExit('This is not a gts4lv system image')
    with zipfile.ZipFile(BASE_OTA) as base:
        binary = base.read('META-INF/com/google/android/update-binary')
        ota_cert = base.read('META-INF/com/android/otacert')
    with tempfile.TemporaryDirectory(prefix='xos-features-', dir=ROOT / '.cache') as tmp:
        tmp = Path(tmp)
        extracted_cert = tmp / 'ota-cert.pem'
        extracted_cert.write_bytes(ota_cert)
        def fingerprint(path):
            return subprocess.check_output(['openssl', 'x509', '-in', str(path),
                                            '-noout', '-fingerprint', '-sha256'])
        if fingerprint(extracted_cert) != fingerprint(CERT):
            raise SystemExit('Base recovery OTA is not signed by the expected test key')
        script = [
            'ui_print("XonolithOS feature update for Samsung gts4lv");',
            'assert(getprop("ro.product.device") == "gts4lv" || '
            'getprop("ro.build.product") == "gts4lv" || '
            'abort("This package is only for Samsung gts4lv (SM-T725)."));',
            'assert(mount("ext4", "EMMC", "/dev/block/by-name/system", "/system", "rw") == "/system" || '
            'abort("Could not mount system read-write."));',
            'assert(file_getprop("/system/system/product/etc/build.prop", "ro.lineage.version") == '
            f'"{lineage_version}" || abort("Install the matching xOS ROM first."));',
            'assert(file_getprop("/system/system/product/etc/build.prop", "ro.xos.version") == '
            '"0.1.0-dev" || abort("Not an xOS 0.1.0-dev system."));',
            'show_progress(0.9, 0);',
        ]
        for relative in FILES:
            dest = '/system/system/' + relative
            temp = '/system/system/product/.xos-' + Path(relative).name + '-new'
            script += [
                f'assert(package_extract_file("payload/{relative}", "{temp}") || '
                f'abort("Could not write {relative}."));',
                f'set_metadata("{temp}", "uid", 0, "gid", 0, "mode", 0644, '
                '"selabel", "u:object_r:system_file:s0");',
                f'assert(rename("{temp}", "{dest}") == "{dest}");',
            ]
            if relative.startswith('product/priv-app/'):
                script.append('set_metadata("/system/system/product/priv-app/XonolithSystemCenter", '
                              '"uid", 0, "gid", 0, "mode", 0755, '
                              '"selabel", "u:object_r:system_file:s0");')
        script += [
            'delete_recursive("/system/system/product/app/Xstore/oat");',
            'set_progress(1.0);',
            'ui_print("xOS feature update installed. Reboot to XonolithOS.");',
        ]
        unsigned = tmp / 'unsigned.zip'
        metadata = ('ota-type=FILE\npre-device=gts4lv\npost-sdk-level=37\n'
                    'xos-version=0.1.0-dev\nxos-content=features-v1\n')
        with zipfile.ZipFile(unsigned, 'w', allowZip64=True) as z:
            z.writestr('META-INF/com/android/metadata', metadata)
            z.writestr('META-INF/com/google/android/update-binary', binary,
                       compress_type=zipfile.ZIP_STORED)
            z.writestr('META-INF/com/google/android/updater-script',
                       '\n'.join(script) + '\n')
            for relative, src in FILES.items():
                z.write(src, 'payload/' + relative, compress_type=zipfile.ZIP_DEFLATED)
        OUTPUT.parent.mkdir(exist_ok=True)
        subprocess.run(['java', '-Djava.library.path=' +
                        str(TREE / 'out/host/linux-x86/lib64'),
                        '-jar', str(SIGNAPK), '-w', str(CERT), str(KEY),
                        str(unsigned), str(OUTPUT)], check=True)
    subprocess.run([str(TREE / 'out/host/linux-x86/bin/check_ota_package_signature'),
                    str(CERT), str(OUTPUT)], check=True)
    with zipfile.ZipFile(OUTPUT) as z:
        if z.testzip():
            raise SystemExit('ZIP integrity check failed')
        for relative, src in FILES.items():
            if hashlib.sha256(z.read('payload/' + relative)).hexdigest() != digest(src):
                raise SystemExit(f'Payload mismatch: {relative}')
    OUTPUT.with_suffix(OUTPUT.suffix + '.sha256').write_text(
        f'{digest(OUTPUT)}  {OUTPUT.name}\n')
    print(f'Validated sideload update: {OUTPUT} ({OUTPUT.stat().st_size} bytes)')


if __name__ == '__main__':
    main()
