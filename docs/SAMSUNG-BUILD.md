# Samsung SM-T725 bring-up

## Device boundary

This target is `gts4lv`, the Galaxy Tab S5e **LTE**. Do not install Wi-Fi `gts4lvwifi`
artifacts. Preserve Samsung's model/manufacturer identifiers; xOS identity is exposed
separately as `ro.xos.name`, `ro.xos.short_name`, `ro.xos.version`.

We start from [anton-scholten's public port](https://github.com/anton-scholten/lineageos-galaxy-tab-s5e)
and [LineageOS 24.0](https://github.com/LineageOS/android/tree/lineage-24.0).
The port reports a tested Wi-Fi build but an **untested LTE build**. On LTE, mobile data
is unconfirmed and its old radio HAL remains a risk. ANT+ is unavailable upstream;
VoLTE/VoWiFi are not supported by that port. These are upstream reports, not xOS test results.

The port reuses the Samsung OMR partition for `/metadata` and carries old-kernel encryption,
Bluetooth, graphics/media and SELinux compatibility work. Those changes are preserved,
not replaced with bypass flags. Its kernel version override represents compatibility
backports; it does not turn the Linux 4.9 kernel into a maintained Linux 5.15 kernel.

## Host and disk

Initial local inspection: 16 logical CPUs, about 15.3 GiB physical RAM, 15.3 GiB disk swap,
another 15.3 GiB compressed zram, and about 491 GiB free disk space. Zram is not additional
RAM and is deliberately excluded from the build capacity check.

The port reports Soong memory peaks around 53 GiB. Our launcher adds 44 GiB of disk swap
and checks for at least 60 GiB physical RAM plus disk-backed swap. It leaves existing
swap settings intact. The new swap consists of 22 two-GiB files in `/swap/xos-build/`,
mode 0600, and is not made persistent in fstab. This host has a multi-device Btrfs filesystem;
a single 48 GiB allocation failed the kernel's single-device requirement. Smaller allocations
are validated with `btrfs inspect-internal map-swapfile` before activation. Incompatible
allocations made by this script are removed; existing system swap is never changed.
The host accepted 22 additional swap areas; its limit prevented a 23rd. That still supplies
about 74.6 GiB of RAM plus disk-backed swap. No filesystem balancing or partition changes are performed.

Do not disable the extra swap during compilation. After the container has stopped and
memory pressure is low, it can be deactivated with:

```bash
sudo bash -c 'for file in /swap/xos-build/swap-*; do swapoff "$file"; done'
```

Leave the files in place to resume later. Swap allocation and Docker access require root;
the source sync and compilation inside the container run as the project owner's UID/GID.
No desktop configuration is changed.

## Build stages

1. `preflight`: validate available memory and the SHA-256 of every locked APK.
2. `initializing`: initialize `android/` with the recorded LineageOS manifest commit.
3. `syncing`: install the Samsung manifests rendered at locked project SHAs, then run a
   four-job current-branch source sync. Re-running resumes successful downloads.
4. `integrating`: verify Samsung project revisions, copy `vendor/extra` and Xstore,
   generate presigned prebuilt APK modules, remove the port's OTA URI and Updater.
   Unexpected edits to the patched files abort instead of being overwritten.
5. `compiling`: `breakfast gts4lv`, then `mka bacon -j2`, with an xOS development version suffix.
6. `collecting`: check xOS/Android version properties and the OTA device assertion, then
   collect this build's ZIP, recovery and vbmeta. Old ZIPs are not mistaken for current output.

The source lock pins the base manifest and Samsung-specific projects. The base manifest
still contains moving upstream branches: `artifacts/manifest-before-xos.xml` records the
resolved commits from each successful sync. Archive that file to reproduce a build.
The builder image ID is recorded in `.state/builder-image.txt`; Ubuntu package versions
are not yet a reproducible toolchain lock.

This script's success is a compile result, not a release result. Default Android test keys
are deliberately used for initial development. Private production keys, verified boot
configuration and an OTA migration strategy must be established before public release.

## First hardware session (not performed yet)

Before writing installation instructions, confirm the tablet's exact model, installed
firmware, bootloader state and current recovery. Back up user data. Installation and the
metadata migration can require a factory reset; never automate that as part of building.

Use the [official model guide](https://wiki.lineageos.org/devices/gts4lv/install/) together
with the selected port's notes, after reviewing the actual built recovery and partitions.
There is no blanket flashing command in this project because the current device state
has not been inspected. No connected tablet has been modified.

Record these results for an xOS candidate:

- Recovery boots; correct model and partition map; recovery logs have no mount errors.
- System first boot, setup without Google, reboot, and safe factory-reset behavior.
- Set PIN/password, reboot and unlock encrypted data; check SELinux enforcing.
- Wi-Fi, Bluetooth pairing/audio, speakers, microphone, cameras, rotation and sensors.
- SIM detection, mobile data, SMS/calling where applicable, airplane-mode recovery.
- Charging, USB/MTP, SD card, suspend/resume, overnight idle battery and thermal behavior.
- WM Keyboard selection and offline typing; each bundled app starts without GMS.
- Xstore catalog load, network failure/cached state, checksum rejection and installer confirmation.
- GCam compatibility, once a specific APK/config has been selected.

Keep full logs private: they can include device identifiers and user data. A simple local
capture command is `adb logcat -b all -d > tablet-logcat.txt`; redact before publishing.
