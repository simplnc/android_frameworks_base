# Phase 1 - Step 4: QS backgrounds + perf anim set (flagged)

- Commit Title: SystemUI: add advanced QS background and perf anim set (flagged)
- Commit Message:
  SystemUI: Add `qs_tile_background_advanced.xml` and performance
  variants `qs_tile_press_perf.xml` / `qs_tile_release_perf.xml`.
  Selection will be wired via flags in a later step:
  - `sysui.qs.bg_advanced=1` to pick advanced bg
  - `sysui.qs.anim_perf=1` to pick perf anims

- Summary:
  Resource-only add of an alternative QS background (mirrors structure of
  the default) and a shorter-duration animator pair for lower overhead.
  No behavior change until wiring lands.

- Build Safety: 96%
  - Pure resources; no code paths yet.

- Rollback:
  Remove the three files or revert the commit.

- Test Plan:
  - Build SystemUI; AAPT2 should pass.
  - Visual verification occurs after wiring in a later step.
