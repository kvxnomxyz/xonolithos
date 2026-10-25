# Xstore

The XonolithOS (**xOS**) app store. This branch contains the standalone Android app source.
The `main` branch contains the OS build project; `appstore` contains catalog folders.

## Build

Requires JDK 17/21, Android SDK platform 35 and build-tools 35.0.0.

```bash
bash build.sh
```

The development APK is written to `artifacts/Xstore-0.1.0-dev.apk`. No Gradle or Google
Services dependencies are used. For ROM builds, use `Android.bp` from the main project.
Development and ROM signing keys differ; do not use this preview as a production OTA.

The catalog endpoint is `kvxnomxyz/xonolithos`, branch `appstore`. See [CATALOG.md](CATALOG.md).
The app has a clean, F-Droid-inspired catalog layout with search and availability filters.
It supports cached listings, HTTPS APK downloads, checksum and package-ID checks, and
confirmation through Android's package installer. It is independent of F-Droid.

Bitpost Account Network integration is pending. No account/token is embedded.
The APK compiles and catalog tests pass; Android UI and on-device installation are untested.

The integrated app source is maintained in sync with `main/apps/Xstore`.
