# XonolithSystemCenter

`apps/XonolithSystemCenter` is a native xOS app. The ROM build signs it with the
platform certificate, places it in `product/priv-app`, and allowlists its recovery,
reboot, and secure-settings permissions. The standalone build command is:

```bash
bash scripts/build-systemcenter.sh
```

That APK uses a development key and is suitable for layout testing. Installing it
on another ROM does not grant privileged controls.

The Updates screen imports a local recovery ZIP, stages it under app-private
`/data`, checks the package's `pre-device` against the running device, and invokes
`RecoverySystem.verifyPackage()` with the device's trusted recovery certificates.
It shows the SHA-256 and asks for confirmation before
`RecoverySystem.installPackage()` requests a recovery reboot. Recovery must still
report the final install result. No remote OTA endpoint, Bitpost account flow, or
background update scheduler is included yet.

The Developer screen explicitly confirms ADB setting changes and reboots to
recovery or bootloader. The Wallpapers screen previews three bundled images and
sets the selected one as the home wallpaper. The app never silently enables ADB,
reboots, flashes, or wipes data.
