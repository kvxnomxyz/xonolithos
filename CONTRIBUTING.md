# Contributing to XonolithOS

Welcome. xOS is a community project, and useful contributions include code, device
testing, translation, accessibility feedback, artwork, and clear bug reports.

## Before a change

Check the [wiki source](wiki/Home.md), open issues, and the
device-specific documents in `docs/`. For a new device, state its exact model and
codename, firmware baseline, bootloader state, and source revisions. A passing
compiler run is not a boot or hardware test.

Use `main` for OS and integrated-app changes, `xstore` for the standalone Xstore
source, and `appstore` only for catalog entries. Keep integrated Xstore sources in
sync with the standalone branch when the same behavior changes.

## Pull requests

Describe the problem, the resulting behavior, and how you checked it. Include
logs or screenshots when they clarify a device or UI issue, but remove tokens,
personal data, serial numbers, and private account information. Keep changes
reviewable; do not include build output, APKs, ROM ZIPs, private signing keys,
or copied third-party source without its license.

Run the relevant local checks. For Xstore, `bash scripts/build-xstore.sh` and
`bash scripts/test-xstore.sh` compile the standalone APK and exercise catalog
parsing and download checks. For SystemCenter, run
`bash scripts/build-systemcenter.sh`; its local development APK cannot exercise
privileged controls unless built into xOS with platform signing. For boot artwork,
run `python3 scripts/make-bootanimation.py` and inspect the resulting frames.

## Reports and support

Open a GitHub issue with the exact device, xOS build, recovery version, steps to
reproduce, expected and observed behavior, and the smallest relevant log excerpt.
Do not publish a device as supported merely because its source compiles.

Treat other contributors with respect. Critique changes and evidence rather than
people. Maintainers may remove harassment, spam, or unsafe flashing instructions.
