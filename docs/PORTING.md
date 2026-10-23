# Porting xOS — draft, not released

Publish a stable revision of this guide **after the Samsung SM-T725 build boots and passes
hardware testing**. xOS is currently an Android 17 / LineageOS 24.0 bring-up project.

## Establish the device baseline

1. Identify the exact codename/model and ownership of a test device. Distinguish variants.
2. Obtain working Android 17 device, common-device, kernel and vendor trees. A successful
   old Android build does not establish Android 17 compatibility.
3. Build and test an unmodified baseline with the corresponding firmware and recovery.
4. Record the repository URLs, branches and commit SHAs, firmware requirements, kernel
   license/source and proprietary blob provenance. Respect redistribution permissions.

Samsung's SDM670 kernel work, old-keyring fixes, OMR metadata layout and radio assumptions
are **specific to this port**. Do not copy its manifests or partition changes into another
device. Redmi Note 7 (`lavender`) will need its own evaluated trees and test plan.

## Shared xOS product

`vendor/extra/product.mk` is inherited by LineageOS's common product configuration. It
includes `vendor/extra/config/xos.mk` for identity and app selection. The integration tool
copies Xstore source and generates `android_app_import` definitions for approved prebuilt
APKs. These are normal product apps with their original signatures, not platform-privileged
applications. The shared product does not alter hardware identifiers or vendor fingerprints.

The current `prepare-tree.py`, container launcher, artifact collector and source locks are
intentionally restricted to `gts4lv`. Add an explicit second device configuration and parameterize
those checks before supporting another target; changing only the `breakfast` argument is insufficient.

For each port:

- Pin its local manifests; record the resolved full manifest after sync.
- Integrate `vendor/extra` without overwriting existing customizations.
- Remove inherited upstream/private-fork OTA services before building an xOS image.
- Audit that no GApps/microG or other unintended account-service packages were included.
- Select a matching ABI for prebuilts; inspect required SDK/features and app signatures.
- Check available system/product space; these APKs must fit the actual partition layout.
- Retain first-boot input/camera fallbacks until the replacements are validated.
- Preserve enforcing SELinux, encryption and secure ADB; diagnose compatibility failures
  rather than globally disabling those protections.

## Deliverables for a supported port

Produce the ROM, matching recovery/vbmeta where needed, SHA-256 sums, exact source revision
manifest and build environment record, app lock, install/upgrade/rollback guide and truthful
known-issues list. Publish applicable kernel/source modifications and required third-party
notices alongside binaries. Separate compiler success from first boot and hardware QA.

Do not ship Android test keys as a production release. Establish and back up private signing
keys outside the public repository, then verify upgrade behavior and package/APEX signing.
Public OTA feeds must use the xOS release keys and correct per-device metadata; Xstore's app
catalog is unrelated to operating-system OTAs.

Bitpost authentication and integration are outside this draft until the owner defines them.
