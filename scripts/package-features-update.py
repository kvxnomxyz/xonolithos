#!/usr/bin/env python3
"""Make a signed gts4lv recovery ZIP from built xOS features."""
import argparse
import hashlib
import os
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


def sdk_files():
    for script in ('build-xstore.sh', 'build-systemcenter.sh'):
        subprocess.run(['bash', str(ROOT / 'scripts' / script)], check=True)
    sdk = Path(os.environ.get('ANDROID_SDK_ROOT',
                  os.environ.get('ANDROID_HOME', Path.home() / 'Android/Sdk')))
    apksigner = sdk / 'build-tools/35.0.0/apksigner'
    if not apksigner.is_file():
        raise SystemExit(f'Missing Android SDK signer: {apksigner}')
    prebuilt = ROOT / '.cache/feature-sdk'
    prebuilt.mkdir(parents=True, exist_ok=True)
    pairs = (
        ('Xstore', 'testkey'),
        ('XonolithSystemCenter', 'platform'),
    )
    result = dict(FILES)
    result['product/media/bootanimation.zip'] = ROOT / 'vendor/extra/bootanimation/bootanimation.zip'
    for app, keyname in pairs:
        source = ROOT / f'artifacts/{app}-0.1.0-dev.apk'
        signed = prebuilt / f'{app}.apk'
        certificate = TREE / f'build/target/product/security/{keyname}.x509.pem'
        key = TREE / f'build/target/product/security/{keyname}.pk8'
        subprocess.run([str(apksigner), 'sign', '--key', str(key), '--cert',
                        str(certificate), '--out', str(signed), str(source)],
                       check=True, stdout=subprocess.DEVNULL)
        verify = subprocess.run([str(apksigner), 'verify', '--print-certs', str(signed)],
                                check=True, capture_output=True, text=True)
        actual = next((line.rsplit(': ', 1)[1].strip().lower()
                       for line in verify.stdout.splitlines()
                       if 'Signer #1 certificate SHA-256 digest:' in line), None)
        der = subprocess.check_output(['openssl', 'x509', '-in', str(certificate),
                                       '-outform', 'DER'])
        expected = hashlib.sha256(der).hexdigest()
        if actual != expected:
            raise SystemExit(f'{app} signer differs from the ROM {keyname} certificate')
        relative = next(name for name in FILES if name.endswith('/' + app + '.apk'))
        result[relative] = signed
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk-build', action='store_true',
                        help='compile apps with local SDK and re-sign with ROM certificates')
    args = parser.parse_args()
    if args.sdk_build:
        files = sdk_files()
        output = OUTPUT.with_name(OUTPUT.stem + '-sdk.zip')
    else:
        completed = ROOT / '.state/features-build.exit'
        if not completed.is_file() or completed.read_text().strip() != '0':
            raise SystemExit('The targeted feature build has not succeeded')
        files = FILES
        output = OUTPUT
    for path in (BASE_OTA, CERT, KEY, SIGNAPK, *files.values()):
        if not path.is_file():
            raise SystemExit(f'Missing prerequisite: {path}')
    if digest(files['product/media/bootanimation.zip']) != digest(
            ROOT / 'vendor/extra/bootanimation/bootanimation.zip'):
        raise SystemExit('Compiled boot animation is stale; rerun the focused build')
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
        for relative in files:
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
        content = 'features-v1-sdk' if args.sdk_build else 'features-v1'
        metadata = ('ota-type=FILE\npre-device=gts4lv\npost-sdk-level=37\n'
                    f'xos-version=0.1.0-dev\nxos-content={content}\n')
        with zipfile.ZipFile(unsigned, 'w', allowZip64=True) as z:
            z.writestr('META-INF/com/android/metadata', metadata)
            z.writestr('META-INF/com/google/android/update-binary', binary,
                       compress_type=zipfile.ZIP_STORED)
            z.writestr('META-INF/com/google/android/updater-script',
                       '\n'.join(script) + '\n')
            for relative, src in files.items():
                z.write(src, 'payload/' + relative, compress_type=zipfile.ZIP_DEFLATED)
        output.parent.mkdir(exist_ok=True)
        subprocess.run(['java', '-Djava.library.path=' +
                        str(TREE / 'out/host/linux-x86/lib64'),
                        '-jar', str(SIGNAPK), '-w', str(CERT), str(KEY),
                        str(unsigned), str(output)], check=True)
    subprocess.run([str(TREE / 'out/host/linux-x86/bin/check_ota_package_signature'),
                    str(CERT), str(output)], check=True)
    with zipfile.ZipFile(output) as z:
        if z.testzip():
            raise SystemExit('ZIP integrity check failed')
        for relative, src in files.items():
            if hashlib.sha256(z.read('payload/' + relative)).hexdigest() != digest(src):
                raise SystemExit(f'Payload mismatch: {relative}')
    output.with_suffix(output.suffix + '.sha256').write_text(
        f'{digest(output)}  {output.name}\n')
    print(f'Validated sideload update: {output} ({output.stat().st_size} bytes)')


if __name__ == '__main__':
    main()
