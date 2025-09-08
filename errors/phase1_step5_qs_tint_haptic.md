# Phase 1 - Step 5: QS stronger haptic on press (flagged)

- Commit Title: SystemUI: strengthen QS press haptic (flagged)
- Commit Message:
  SystemUI: On ACTION_DOWN in QSTileViewImpl, perform
  `HapticFeedbackConstants.CONTEXT_CLICK` with IGNORE flags; fallback to
  LONG_PRESS if not performed. Enabled only when `sysui.qs.haptic=1`.

- Summary:
  Upgrades the perceived click from light `VIRTUAL_KEY` to a stronger,
  crisp `CONTEXT_CLICK`, while respecting global/view settings via
  override flags. Default remains off.

- Build Safety: 95%

- Rollback:
  Change back to VIRTUAL_KEY or remove the block; or revert.

- Test Plan:
  - `setprop sysui.qs.haptic 1` then tap QS tiles; stronger haptic should be felt.
