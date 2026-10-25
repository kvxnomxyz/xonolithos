# Redmi Note 7 (`lavender`) Android 17 bring-up

The build job is queued behind the Samsung branding compile. Its state is
`.state/lavender-status.json`, with detailed output in `.state/lavender-build.log`.
The source lock is `manifests/lavender/sources.lock.json` and the resolved
full manifest will be saved as `artifacts/manifest-lavender.xml`.

The device, common-device, proprietary vendor and camera trees come from
[pix106's public Android 17 work](https://github.com/pix106/android_device_xiaomi_lavender).
The pinned `southwest-ng` kernel contains `vendor/xiaomi/sdm660_defconfig`
and `vendor/xiaomi/lavender.config`, as required by the device tree. The
`hardware/xiaomi` project comes from LineageOS. These are development source
trees, not an xOS hardware validation or a promise that this configuration boots.

The device tree has a fixed-partition layout and a separate recovery partition.
Any recovery, firmware prerequisite and install sequence must be checked against
the physical phone before flashing. Do not use the Samsung tablet instructions.

The ROM build uses the existing Android platform checkout with a separate
`android/out-lavender` output, two build jobs, and no Google services. The
result will be under `android/out-lavender/target/product/lavender`; its SHA-256
record will be `artifacts/lavender-rom.sha256`. A successful compile is only
the first milestone. Boot, radio, camera, encryption, SELinux, recovery and
upgrade testing remain device-specific work.
