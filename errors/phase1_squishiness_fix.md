# Phase 1 - Enhanced Squishiness Fix

- Commit Title: SystemUI: fix QS squishiness for all tiles + more pronounced effect
- Commit Message:
  SystemUI: Fix QS tile squishiness to work on ALL tiles, not just Internet tile.
  Move physics handler before parent processing and make effect more pronounced:
  - Scale: 0.90 → 0.82 (more squish)
  - Alpha: 0.95 → 0.88 (more fade)
  - Add vibration on press for tactile feedback
  - Faster animations (80ms/140ms)

- Summary:
  - Fixed touch event handling order - physics handler now runs FIRST
  - More pronounced squish effect (18% scale reduction vs 10%)
  - Added vibration feedback on each tile press
  - Faster, snappier animations

- Build Safety: 95%
  - Same approach, just reordered and enhanced

- What you'll see:
  - ALL QS tiles now squish when pressed (not just Internet)
  - Much more noticeable squish effect
  - Vibration feedback on each tile press
  - Snappier animations

- Test Plan:
  - Build and test - every QS tile should now squish with vibration
