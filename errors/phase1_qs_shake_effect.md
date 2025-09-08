# Phase 1 - QS Tile Shake Effect

- Commit Title: SystemUI: add subtle shake effect to QS tile press
- Commit Message:
  SystemUI: Add subtle shake animation to QS tile press for enhanced tactile feedback:
  - 2px horizontal shake during press animation
  - 60ms total shake duration (4 phases of 15ms each)
  - Shake: right -> left -> right/2 -> center
  - Combined with existing squish and vibration for premium feel

- Summary:
  - Added animateShake() method with 4-phase horizontal movement
  - Very subtle 2px shake distance to avoid being distracting
  - Quick 60ms duration that complements the squish animation
  - Shake runs alongside squish, not replacing it

- Build Safety: 95%
  - Pure animation addition, no breaking changes
  - Uses existing animation framework

- What you'll see:
  - QS tiles now shake slightly when pressed
  - Combined effect: squish + shake + vibration
  - More tactile and responsive feeling
  - Subtle enough to not be annoying

- Test Plan:
  - Build and test - tiles should shake slightly when pressed
  - Should feel more "alive" and responsive
  - Shake should be subtle, not distracting
