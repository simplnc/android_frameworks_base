# Phase 1 - Step 1: QS anim resources

- Commit Title: SystemUI: add qs_tile_press/release anim resources
- Commit Message:
  SystemUI: Add animator XMLs for QS tile press/release.
  No wiring yet; defaults unchanged.

- Summary:
  Adds `packages/SystemUI/res/animator/qs_tile_press.xml` and
  `packages/SystemUI/res/animator/qs_tile_release.xml`. Pure resources,
  unused until explicitly wired. Conservative durations and values
  aligned with Material motion. No private attrs.

- Build Safety: 99%
  - Resource-only; no Java/Kotlin wiring.
  - No manifest changes.
  - AAPT2-safe attributes only.

- Rollback:
  Remove the two files or revert the commit.

- Test Plan:
  - Build SystemUI to ensure AAPT2 passes.
  - No runtime change expected.
