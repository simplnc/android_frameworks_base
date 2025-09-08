# Phase 1 - Step 2: QS physics handler (gated)

- Commit Title: SystemUI: introduce QSTileAdvancedPhysicsHandler (flagged)
- Commit Message:
  SystemUI: Add `QSTileAdvancedPhysicsHandler` that animates simple
  press/release scaling and alpha. The handler is disabled by default
  and only runs when `sysui.qs.advanced_physics=1`.

- Summary:
  Adds a standalone Kotlin class under `qs/tileimpl` with conservative
  values (scale 0.90, alpha 0.95). Early returns unless the sysprop is
  set. No wiring yet.

- Build Safety: 97%
  - New class only; no manifest edits.
  - No references unless wired.

- Rollback:
  Remove the file or revert the commit.

- Test Plan:
  - Build SystemUI; ensure compilation succeeds.
  - Leave default off; no runtime change.
  - (Later) enable via `setprop sysui.qs.advanced_physics 1` for testing
    after Step 3 wiring.
