# SM-T725 branding sideload

`scripts/package-branding-update.py` packages the already-compiled boot animation,
platform resource APK, LineageParts APK and Settings APK into a small recovery ZIP.
It reuses the updater binary from the matching full ROM and signs the result with
the development OTA key that the current recovery trusts. It does not rebuild the ROM.

The package checks for `gts4lv` and the matching `xOS-0.1.0-dev` base build before
writing. It touches only those four system files and removes the old preoptimized
copies of the two modified apps; it does not wipe data or change boot, recovery,
vendor or vbmeta. Rebuild the package after any of the four modules changes.

Output: `artifacts/xOS-0.1.0-dev-gts4lv-branding-sideload-v2.zip` with an adjacent
SHA-256 file. In the matching Lineage recovery, choose Apply update > Apply from
ADB, then run:

```bash
adb sideload artifacts/xOS-0.1.0-dev-gts4lv-branding-sideload-v2.zip
```

The host's `Total xfer: 1.00x` means transfer finished, not that recovery
installed the update. Read the recovery result before rebooting. Do not use
this ZIP on lavender or on a different Samsung build. This is a test-key
development package, not a public release.
