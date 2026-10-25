# Samsung feature update pack

The quick packaging path compiles Xstore and XonolithSystemCenter with the local
Android SDK, signs the apps with the matching ROM development certificates, and
adds the generated boot animation without rebuilding the complete ROM:

```bash
python3 scripts/package-features-update.py --sdk-build
```

Output: `artifacts/xOS-0.1.0-dev-gts4lv-features-v1-sdk.zip` with an adjacent
SHA-256 file. The APK signatures are checked against the exact ROM test and
platform certificates. This path has passed host-side signature and payload
validation; installation on the physical tablet still needs testing.

The integrated Android build path is also available:

```bash
bash scripts/build-features.sh
python3 scripts/package-features-update.py
```

It produces `artifacts/xOS-0.1.0-dev-gts4lv-features-v1.zip` after the focused
Android build succeeds. Both packagers require the matching original ROM OTA.
They include Xstore, platform-signed SystemCenter, the boot animation, and the
privileged-permission allowlist. Recovery checks the device and exact base
`ro.lineage.version` before writing. Both use the development OTA certificate
trusted by the existing recovery and verify the signed output.

On the matching SM-T725 development ROM, use recovery's **Apply update → Apply
from ADB**, then:

```bash
adb sideload artifacts/xOS-0.1.0-dev-gts4lv-features-v1-sdk.zip
```

Check recovery's final installation result before rebooting. The pack does not
replace the full ROM, wipe data, or alter boot, recovery, vendor, or vbmeta. Do
not use it on the Wi-Fi Tab S5e, lavender, or another build. This is a test-key
development update, not a public release. The standalone SystemCenter APK uses
a different development key and cannot perform privileged actions when sideloaded.
