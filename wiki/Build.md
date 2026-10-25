# Build from source

The OS lives on the `main` branch. It pins external Android and device-tree revisions so contributors can reproduce the same checkout. Follow the [Samsung build procedure](https://github.com/kvxnomxyz/xonolithos/blob/main/docs/SAMSUNG-BUILD.md) or the [Redmi Note 7 build state](https://github.com/kvxnomxyz/xonolithos/blob/main/docs/LAVENDER.md) for current commands and requirements.

`python3 scripts/doctor.py` checks the host. `python3 scripts/apps.py fetch` verifies preinstalled app sources. `scripts/prepare-tree.py` installs xOS additions into the pinned Android tree. The Samsung and lavender compiles use separate output directories.

For focused work, `bash scripts/build-xstore.sh` and `bash scripts/build-systemcenter.sh` make standalone development APKs. The latter does not receive ROM-only privileged permissions when sideloaded. `python3 scripts/make-bootanimation.py` regenerates the boot ZIP from source.

Keep build output and signing keys out of Git. Record whether each check was a compile, emulator test, or physical-device test. Public ROM releases need hardware validation, independent signing, and redistribution review.
