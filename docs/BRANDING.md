# XonolithOS visual identity

The canonical [SVG logo](../branding/xonolithos-logo.svg) combines a faceted X
with the XonolithOS wordmark. The [mark](../branding/xonolithos-mark.svg) can be
used as a compact icon. PNG exports are included for surfaces that do not render
SVG. Keep the spelling `XonolithOS` and `xOS` exact.

`scripts/make-bootanimation.py` generates a stored Android boot ZIP in
`vendor/extra/bootanimation/`. A small phone breaks into crystalline shards,
the XonolithOS wordmark appears, then pulses until Android starts. Run the
script after editing it and inspect
[`bootanimation-preview.png`](../branding/bootanimation-preview.png).

XonolithSystemCenter includes three selectable wallpapers: Obsidian for tablet
and phone, and Glacier for tablet. Their original PNG files are under
`apps/XonolithSystemCenter/res/drawable-nodpi/`; they are shipped inside the app.
The app lets the user choose and apply them. A new ROM or update pack is required
before any source change appears on an existing device.
