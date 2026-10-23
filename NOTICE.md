# Sources and attribution

- [LineageOS](https://github.com/LineageOS): Android manifest, device trees, platform and build system.
- [Android Open Source Project](https://source.android.com/): underlying platform and tools.
- [anton-scholten's Tab S5e port](https://github.com/anton-scholten/lineageos-galaxy-tab-s5e):
  Samsung Android 17 bring-up, public local manifests, device/kernel/platform compatibility work.
  The local manifests in this project are copied from the commit in `manifests/sources.lock.json`.
  Their comments describe the original author's work, not work independently performed by xOS.
- [ExyHyperBrick](https://github.com/ExyHyperBrick): kernel backport work used by the Samsung port.
- [TheMuppets](https://github.com/TheMuppets): vendor blob repositories used by the port.
- WM Keyboard, F-Droid, Aurora Store, Mozilla Firefox and Obsidian retain their original
  authorship, signatures, licenses and trademarks. See `prebuilts/apps.lock.json` and `docs/APPS.md`.

No xOS testing claim is implied by an upstream author's reported result. Original authors'
Git histories remain in their repositories. The local manifests refer to them instead of
flattening or claiming their kernel/device work as original xOS work.
