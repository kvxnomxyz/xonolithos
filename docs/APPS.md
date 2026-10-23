# Initial xOS applications

| App | Package/source | Initial build treatment |
|---|---|---|
| WM Keyboard | `com.wasimaster.wmkeyboard`, F-Droid | Original F-Droid APK, upstream signature retained |
| Obsidian | `md.obsidian`, official GitHub release | Original APK for the private local build; public bundling permission unresolved |
| F-Droid | `org.fdroid.fdroid`, F-Droid | Suggested stable version; no privileged extension |
| Aurora Store | `com.aurora.store`, F-Droid | Normal app; no privileged installer service |
| Firefox | `org.mozilla.firefox`, Mozilla archive | ARM64 APK with upstream signature |
| GCam | Device-specific port not selected | Pending; upstream camera retained for bring-up |
| Xstore | `org.xonolithos.xstore`, this source tree | Compiled by the ROM build |

The spelling in the supplied keyboard URL was `com.wasimeter.wmkeyboard`; F-Droid's
verified WM Keyboard package is `com.wasimaster.wmkeyboard`.

`prebuilts/apps.lock.json` records the selected versions, exact download URLs, measured
SHA-256 values and verified APK signing-certificate fingerprints. Hashes lock the reviewed
downloads; they are not a claim of independent reproducible-build verification. The APKs
stay in the ignored local `prebuilts/` directory. No APK is re-signed with a platform key.

The build keeps Lineage's input method and camera as first-boot fallbacks. Selecting WM
Keyboard as default is a user action until tablet setup testing establishes a safe default.
Firefox does not change system WebView. Aurora's compatibility with Google Play distribution
does not make xOS compatible with Google Play Services APIs used by some downloaded apps.

No Google Services package, microG, signature-spoofing patch or substitute GMS provider is
bundled. Bitpost Account Network is reserved for the owner's later specification; there
is no fabricated API, login screen, endpoint or compatibility promise.

GCam support varies with the camera HAL, port and GMS expectations. Selecting an arbitrary
APK and calling it compatible would be misleading. Test a specific port on the SM-T725
before adding it to the lock; ordinary Lineage camera functionality remains the first milestone.

Before distributing a ROM, resolve Obsidian/GCam redistribution rights and fulfill the
source, notice and trademark requirements for the chosen versions of all bundled apps.
An official public APK download is not itself a redistribution license. Preserve upstream
copyright notices and record the corresponding source/build metadata for copyleft packages.
