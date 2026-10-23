# XonolithOS

**xOS** is case-sensitive: lowercase `x`, uppercase `OS`.

Android **17**, LineageOS **24.0**. First target: **Samsung Galaxy Tab S5e LTE,
SM-T725 (`gts4lv`)**. This is a development project, not an official LineageOS release.
The Redmi Note 7 (`lavender`) is a later target; no Redmi build is supplied here.

## Current work

- Samsung source manifests and a verified revision lock for the device dependencies.
- A resumable Ubuntu 22.04 container build on the local Omarchy machine.
- xOS product properties and a separately buildable native **Xstore** app.
- Presigned APK integration for WM Keyboard, Obsidian, F-Droid, Aurora Store, Firefox.
- No GApps or microG. **Bitpost Account Network is pending the owner's specification.**
- GCam is pending device-specific selection and testing. The upstream camera remains available.
- The inherited third-party OTA feed and Updater app are removed for the bring-up build.

Build state is recorded in `.state/status.json`; the log is `.state/build.log`.
Only `stage: built` means a ROM compiled successfully. It does **not** mean that it boots,
that LTE works, or that the ROM is ready to distribute.

## Start or resume the local tablet build

```bash
python3 scripts/doctor.py
python3 scripts/apps.py fetch
pkexec bash scripts/start-local-build.sh
```

The privileged script creates **44 GiB of disk-backed swap**, starts Docker, builds
the Ubuntu builder image and launches `xos-gts4lv` in the background. An authentication
dialog is expected. Read the script before running it. It does not flash a device,
publish a repository, change group membership or write `/etc/fstab`.

```bash
tail -f .state/build.log
cat .state/status.json
# Stop the container, keeping all source and incremental build outputs:
pkexec docker stop --time 60 xos-gts4lv
```

The initial source sync is large. Compilation is limited to two jobs for this 16 GiB
machine; swapping may make it very slow. Leave the computer powered and awake.
After a reboot, run the same start command to re-enable the temporary swap and resume.

Build outputs, if successful, are copied to `artifacts/xOS-*/`, including the ROM ZIP,
recovery, vbmeta, SHA-256 checksums, a resolved source manifest, app lock, and an explicit
hardware-testing status. They use **development test keys**. No public OTA service is enabled.

## Xstore

Xstore uses `kvxnomxyz/xonolithos`, branch **`appstore`**. The source is in `apps/Xstore`.
It is a regular Android app: no privileged installation, embedded tokens or Google dependencies.
The appstore branch must be public; a private or missing catalog is displayed
as unavailable, without fake app listings. The `main` branch holds the OS project,
`xstore` holds the standalone store source, and `appstore` holds catalog folders.

```bash
# Requires JDK 17/21, Android SDK platform 35 and build-tools 35.0.0.
bash scripts/build-xstore.sh
```

Standalone output: `artifacts/Xstore-0.1.0-dev.apk`. This has a separate development
signature; the ROM compiles and signs Xstore from source with its own default build key.

## Documentation

- [Samsung build and test procedure](docs/SAMSUNG-BUILD.md)
- [App bundle and outstanding decisions](docs/APPS.md)
- [Xstore folder format](docs/XSTORE-CATALOG.md)
- [Porting guide draft](docs/PORTING.md) — publish after Samsung validation
- [Source attribution](NOTICE.md)

Original xOS code is Apache-2.0. Upstream Android, kernel and third-party applications
retain their own licenses. The kernel is GPL-2.0; the xOS license does not relicense it.
