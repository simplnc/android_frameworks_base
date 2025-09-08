# Phase 1 - Default Enabled Configuration

- Commit Title: SystemUI: enable Phase 1 QS features by default
- Commit Message:
  SystemUI: Change all Phase 1 QS feature flags to default enabled (true).
  Users will see squishiness, haptics, advanced background, and perf
  animations immediately after build without needing ADB commands.

- Summary:
  - `sysui.qs.advanced_physics` now defaults to `true`
  - `sysui.qs.haptic` now defaults to `true` 
  - `sysui.qs.bg_advanced` now defaults to `true`
  - `sysui.qs.anim_perf` now defaults to `true`

- Build Safety: 95%
  - Same code, just different defaults

- What you'll see after build:
  - QS tiles squish on press/release (90% scale, 95% alpha)
  - Strong haptic feedback on tile taps
  - Advanced background (currently same as default)
  - Performance animations (90ms/140ms durations)

- To disable any feature (if needed):
  ```bash
  adb shell setprop sysui.qs.advanced_physics 0
  adb shell setprop sysui.qs.haptic 0
  adb shell setprop sysui.qs.bg_advanced 0
  adb shell setprop sysui.qs.anim_perf 0
  ```
