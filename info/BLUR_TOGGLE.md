# SystemUI Blur Toggle

This ROM exposes a runtime toggle to disable SystemUI blur effects to reduce GPU composition cost and improve smoothness on lower-end hardware, while preserving visual fidelity when disabled.

## What it does
- Gates Notification Shade blur (and other blur paths) via `Settings.Secure.BLUR_EFFECTS_DISABLED`.
- When enabled (set to 1), blur radius is forced to 0 in the shade depth controller and related surfaces.
- Plays well with existing logic that avoids blurs during unlock/app launch.

## How to use
- Enable (disable blurs):
  ```bash
  adb shell settings put secure blur_effects_disabled 1
  ```
- Disable (restore blurs):
  ```bash
  adb shell settings put secure blur_effects_disabled 0
  ```

Changes apply live; no reboot required.

## Affected components
- Notification shade (window blur and wallpaper zoom out).
- Any additional blur surfaces that respect the central blur radius application.

## Notes
- Devices that do not support blurs on windows are unaffected.
- Battery Saver mode can complement this toggle if you choose to couple it in the future.