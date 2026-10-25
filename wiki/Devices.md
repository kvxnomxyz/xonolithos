# Devices

| Device | Codename | Current state |
| --- | --- | --- |
| Samsung Galaxy Tab S5e LTE, SM-T725 | `gts4lv` | Android 17 development build compiled and booted. Branding sideload v3 reported success in recovery. LTE, camera, sensors, and release readiness still require testing. |
| Xiaomi Redmi Note 7 | `lavender` | Pinned device source prepared. ROM compilation and physical-device validation are pending. |

Do not flash a build for another model or codename. Development builds use test keys; back up data before device experiments. A successful ADB transfer does not prove recovery installed an update—read the recovery result.

See [Samsung build and test notes](https://github.com/kvxnomxyz/xonolithos/blob/main/docs/SAMSUNG-BUILD.md), [lavender status](https://github.com/kvxnomxyz/xonolithos/blob/main/docs/LAVENDER.md), and the [porting guide](https://github.com/kvxnomxyz/xonolithos/blob/main/docs/PORTING.md). A port should document firmware baseline, kernel and vendor sources, recovery behavior, and hardware results before it is called supported.
