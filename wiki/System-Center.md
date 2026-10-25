# XonolithSystemCenter

XonolithSystemCenter is the on-device xOS maintenance app in `main/apps/XonolithSystemCenter`.

**Updates:** select a local OTA ZIP. The app stages it, checks the target device in the package metadata, verifies the recovery signature, shows the checksum, and asks for confirmation. Android then reboots to recovery to perform the installation. The recovery screen is the final result. There is no automatic online OTA feed yet.

**Developer controls:** inspect or change USB debugging, reboot to recovery, or reboot to bootloader. These actions require explicit taps and confirmation. They work only in the ROM's platform-signed, privileged build; the separately compiled development APK is for UI testing.

**Wallpapers:** preview and apply the included Obsidian and Glacier artwork. See [[Branding]].
