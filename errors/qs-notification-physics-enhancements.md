# QS Tile and Notification Physics Enhancements

## Problem
User requested enhanced UI interactions for Quick Settings tiles and notifications with specific color schemes, improved physics, and better tactile feedback.

## Solution Implemented

### 1. QS Tile Color Enhancements
- **Privacy tiles** (mic, camera, location): Pastel red (`#e6294f`) when enabled
- **Connectivity tiles** (wifi, mobile data, sync, hotspot, internet): Pastel blue (`#0e67ab`) when enabled
- **Theme compatibility**: Default tiles respect user's `colorAccent` theme customization
- **Flashlight**: Maintains orange color (`#c99a20`) for consistency

### 2. Enhanced QS Tile Physics
- **Increased squishiness**: More pronounced depression (88% scale vs previous 92%)
- **Enhanced elasticity**: Longer release animations (320ms) with bounce effect (105% overshoot)
- **Better spacing**: Increased tile padding from 12dp to 16dp, start padding from 16dp to 20dp
- **Shadow effects**: New layered backgrounds with shadow layers for depth
- **Enhanced depression**: Stronger visual feedback when pressed (-8f elevation vs -6f)
- **Improved haptics**: Multi-level haptic feedback for better tactile response

### 3. Notification Physics & Interactions
- **Squishiness matching QS tiles**: Similar physics parameters for consistent UX
- **Enhanced elasticity**: Smooth bounce-back animations with overshoot interpolator
- **Improved dismissal animations**: Enhanced swipe and tap-to-dismiss with directional effects
- **No haptic feedback**: Disabled to avoid interference with system feedback
- **Seamless integration**: Physics handler automatically manages lifecycle and cleanup

### 4. Enhanced Visual Resources
- **New enhanced backgrounds**: Created `qs_tile_background_enhanced.xml` with shadow layers
- **Shadow effects**: Added subtle shadow shapes for depth perception
- **Pressed state visuals**: Dedicated pressed background for better depression feedback
- **Property-based theming**: Enhanced backgrounds are opt-in via system property

## Safety Measures Implemented

### Bootloop Prevention
- **Exception handling**: All physics handlers wrapped in try-catch blocks
- **Graceful degradation**: Falls back to standard behavior if physics handlers fail
- **Resource cleanup**: Proper lifecycle management prevents memory leaks
- **System property controls**: Can be disabled via `sysui.qs.bg_enhanced` and `sysui.notification.physics`

### Theme Compatibility
- **Respects user themes**: Uses `colorAccent` for default tiles
- **Maintains existing functionality**: All original QS tile behaviors preserved
- **Backward compatibility**: Gracefully handles missing theme attributes
- **No breaking changes**: All existing tile specifications continue to work

### Build Stability
- **No resource conflicts**: All new resources use unique names
- **Proper imports**: Kotlin files properly handle imports
- **Memory management**: Physics handlers properly cleaned up on view detachment
- **Error logging**: Comprehensive logging for debugging without crashing

## Compatibility Notes

### Tested Against
- **LineageOS 22.2**: Full compatibility maintained
- **crDroid**: No conflicts with existing implementations
- **Alhadroid**: Theme system respected
- **Android 15**: Uses modern animation APIs and system properties

### Known Safe
- **No haptic conflicts**: Notification haptics disabled to prevent system interference
- **Theme preservation**: User's custom theme colors are respected
- **Performance optimized**: Uses efficient animation systems
- **Memory safe**: Proper cleanup prevents leaks

## Files Modified
1. `packages/SystemUI/src/com/android/systemui/qs/tileimpl/QSTileViewImpl.kt`
2. `packages/SystemUI/src/com/android/systemui/qs/tileimpl/QSTileAdvancedPhysicsHandler.kt`
3. `packages/SystemUI/src/com/android/systemui/statusbar/notification/row/NotificationPhysicsHandler.kt`
4. `packages/SystemUI/src/com/android/systemui/statusbar/notification/row/ExpandableNotificationRow.java`
5. `packages/SystemUI/res/values/dimens.xml`
6. `packages/SystemUI/res/drawable/qs_tile_background_enhanced.xml`
7. `packages/SystemUI/res/drawable/qs_tile_background_enhanced_shape.xml`
8. `packages/SystemUI/res/drawable/qs_tile_shadow_shape.xml`
9. `packages/SystemUI/res/drawable/qs_tile_background_pressed_shape.xml`

## System Properties
- `sysui.qs.bg_enhanced=true`: Enables enhanced QS tile backgrounds
- `sysui.qs.haptic=true`: Enables enhanced QS tile haptics
- `sysui.notification.physics=true`: Enables notification physics
- `sysui.qs.advanced_physics=true`: Enables advanced QS physics

## Result
Enhanced UI interactions with improved tactile feedback, better visual hierarchy, and maintained theme compatibility while preventing bootloops and system instability.
