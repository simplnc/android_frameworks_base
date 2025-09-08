# Phase 1 - Step 3: Wire handler in QSTileViewImpl (flagged)

- Commit Title: SystemUI: wire advanced QS physics handler behind flag
- Commit Message:
  SystemUI: Add optional forwarding of touch events from QSTileViewImpl
  to QSTileAdvancedPhysicsHandler. The handler itself checks
  `sysui.qs.advanced_physics` and returns early when disabled. Default
  behavior remains unchanged.

- Summary:
  - Adds a nullable field `advancedPhysicsHandler` in `QSTileViewImpl`.
  - Lazy-creates the handler and forwards MotionEvent.
  - Calls `cleanup()` on detach.
  - No effect unless `setprop sysui.qs.advanced_physics 1`.

- Build Safety: 95%
  - Minimal guarded call sites.
  - No manifest/components added.

- Rollback:
  Remove the field + calls, or revert the commit.

- Test Plan:
  - Build SystemUI; boot should be unaffected.
  - Runtime test (adb shell):
    - `setprop sysui.qs.advanced_physics 1`
    - Toggle QS tiles and verify press/release animation plays.
