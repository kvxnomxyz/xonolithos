# Samsung feature update pack

The focused Samsung build compiles Xstore, XonolithSystemCenter, and the boot
animation without rebuilding the complete ROM:

```bash
bash scripts/build-features.sh
python3 scripts/package-features-update.py
```

The packager requires a successful feature build and the matching original ROM
OTA. It embeds the compiled Xstore APK, platform-signed SystemCenter APK, boot
animation, and privileged-permission allowlist. It checks the device and exact
base `ro.lineage.version` in recovery before writing. It uses the same development
OTA certificate that the existing recovery trusts and verifies the signed output.

Output: `artifacts/xOS-0.1.0-dev-gts4lv-features-v1.zip` and its SHA-256 file.
On the matching SM-T725 development ROM, use recovery's **Apply update → Apply
from ADB**, then:

```bash
adb sideload artifacts/xOS-0.1.0-dev-gts4lv-features-v1.zip
```

Check recovery's final installation result before rebooting. The pack does not
replace the full ROM, wipe data, or alter boot, recovery, vendor, or vbmeta. Do
not use it on the Wi-Fi Tab S5e, lavender, or another build. This is a test-key
development update, not a public release. The standalone SystemCenter APK uses
a different development key and cannot perform privileged actions when sideloaded.
