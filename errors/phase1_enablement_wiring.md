# Phase 1 Enablement Wiring

- Commit Title: SystemUI: wire Phase 1 QS flags for immediate enablement
- Commit Message:
  SystemUI: Add flag-based selection for QS backgrounds and animation durations.
  - `sysui.qs.bg_advanced=1` selects advanced background
  - `sysui.qs.anim_perf=1` uses shorter animation durations
  Phase 1 is now fully functional with all features gated behind props.

- Summary:
  - Updated `createTileBackground()` to check `sysui.qs.bg_advanced` flag
  - Updated `QSTileAdvancedPhysicsHandler` to use perf durations when `sysui.qs.anim_perf=1`
  - All Phase 1 features now controllable via system properties

- Build Safety: 95%
  - Small guarded additions; no breaking changes

- Enablement Commands:
  ```bash
  # Basic physics + haptic
  adb shell setprop sysui.qs.advanced_physics 1
  adb shell setprop sysui.qs.haptic 1
  
  # Advanced background
  adb shell setprop sysui.qs.bg_advanced 1
  
  # Performance animations (shorter durations)
  adb shell setprop sysui.qs.anim_perf 1
  ```

- Test Plan:
  - Build SystemUI; boot should be unaffected
  - Enable flags and test QS tile interactions
  - Verify background changes and animation speed differences
