# Initial validation — 2026-10-23

## Passed

- Four Python tests covering LTE manifest selection, exact revision rendering, metadata-only
  catalog entries, required installation metadata, HTTP rejection and APK checksum mismatch.
- JVM Xstore test covering GitHub catalog parsing at a pinned commit, metadata-only listings,
  verified downloads, tamper rejection/deletion and a missing appstore branch.
- After publishing, the actual Xstore catalog client loaded all five entries from the live
  `kvxnomxyz/xonolithos:appstore` branch and validated their installation metadata.
- Xstore compiled with Android SDK 35 and JDK 21 into a standalone development APK.
  Android apksigner verified its v2 and v3 signatures.
- All five downloaded prebuilt APKs passed Android signature verification and package-ID
  inspection. Their SHA-256 and signing-certificate fingerprints are recorded in the app lock.
- The OTA-removal patch anchors matched the downloaded upstream Samsung/Lineage source.
- Shell syntax and Python compilation checks passed.
- Local Ubuntu builder image built; the Android source-sync stage started successfully.
- Extra Btrfs swap is active. Memory preflight counts physical RAM and disk swap, not zram.

## Not yet verified

The full ROM has not compiled, booted or been flashed. Samsung LTE and other hardware are
untested. Xstore's Android UI and real package-installer handoff have not been exercised
on a device; its JVM test is not an Android instrumentation test. GCam is not selected.
Bitpost integration is not implemented. Production signing and release readiness are pending.

This is a setup-time record. `.state/status.json` is the local job status; a later compile
does not automatically update this document or turn it into a hardware-test report.
