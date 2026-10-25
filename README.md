<p align="center">
  <img src="branding/xonolithos-logo.svg" alt="XonolithOS" width="540">
</p>

# XonolithOS

**XonolithOS (xOS)** is a community Android fork built from LineageOS 24.0 and Android 17. The short name is case-sensitive: **xOS**. We are building a polished, repairable system with clear device support, open source code, and room for contributors.

This project is in **developer preview**. The current Samsung Galaxy Tab S5e LTE build has booted, but hardware validation and release signing are incomplete. The Redmi Note 7 build is still in progress. Please do not present these builds as stable releases.

## What we are building

- A recognizable xOS look: the phone-to-wordmark boot animation, crystalline wallpapers, and a reusable [logo](branding/xonolithos-logo.svg).
- [Xstore](apps/Xstore), an app browser backed by the public `appstore` branch. It keeps the requested folder-based `title.txt`, `author.txt`, and `description.txt` catalog format.
- [XonolithSystemCenter](apps/XonolithSystemCenter), a local tool for verified update packs, boot options, USB debugging control, and included wallpapers.
- A Google-services-free base. Bitpost Account Network remains a future integration until its protocol and account flow are specified.
- Device bring-up that records what has actually built and been tested.

The new look and SystemCenter are **source changes**. They will appear on devices only after a new ROM or update package containing them is built and installed. For the existing Samsung development ROM, see the [feature update procedure](docs/FEATURES-UPDATE.md).

## Device status

| Device | Codename | State |
| --- | --- | --- |
| Samsung Galaxy Tab S5e LTE (SM-T725) | `gts4lv` | Android 17 development ROM built and booted; hardware tests and release signing pending |
| Xiaomi Redmi Note 7 | `lavender` | Pinned source prepared; ROM compile and device tests pending |

There is no public stable download or automatic OTA channel yet. See the [device pages](wiki/Devices.md) before attempting a port or flash.

## Get involved

Contributions are welcome in device bring-up, Android app development, accessibility, translations, visual design, testing, and documentation. Start with [CONTRIBUTING.md](CONTRIBUTING.md), then open an issue or pull request with the device, branch, and reproducible details. Please distinguish a build result from an on-device test.

The [wiki source](wiki/Home.md) is the entry point for devices, building, Xstore, SystemCenter, and branding. More detailed source documents live in [docs](docs/).

## Repository layout

- `main`: OS sources, apps integrated into the ROM, build scripts, documentation, and artwork.
- `xstore`: standalone Xstore Android app source.
- `appstore`: public Xstore catalog folders; no OS source or app implementation.
- `apps/`: native xOS apps.
- `vendor/extra/`: ROM integration and generated boot animation.
- `manifests/`: pinned upstream source revisions.

To build locally, start with [Samsung build instructions](docs/SAMSUNG-BUILD.md) or the [Redmi Note 7 status](docs/LAVENDER.md). The [porting guide](docs/PORTING.md) describes the expected device bring-up work. Generated APKs and ROM ZIPs are development artifacts; they are not stored in Git.

## Trust and licensing

Xstore checks APK SHA-256 and package identity before handing an APK to Android's installer. SystemCenter uses Android's recovery certificate check before offering to reboot for an update. Both still depend on the integrity of the published catalog or update source; community submissions need review.

Original xOS code is [Apache-2.0](LICENSE). Android, LineageOS, kernels, device trees, and third-party apps retain their own licenses; see [NOTICE.md](NOTICE.md). XonolithOS is an independent project and is not an official LineageOS release.
