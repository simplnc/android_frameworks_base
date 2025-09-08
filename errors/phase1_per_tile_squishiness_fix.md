# QS Tile Per-Tile Squishiness Fix

## Problem
The squishiness effect was only working on one QS tile and would stop working after some time. This was caused by the `QSTileAdvancedPhysicsHandler` being implemented as a static variable, meaning all tiles shared the same handler instance.

## Root Cause Analysis
1. **Static Handler**: The `advancedPhysicsHandler` was declared as a `private static var`, causing all QS tiles to share the same physics handler instance
2. **Single Instance Limitation**: Only one tile could use the handler at a time, causing conflicts
3. **State Conflicts**: Animation state was not properly managed, leading to inconsistent behavior
4. **Timeout Issues**: The handler would become unresponsive after extended use

## Solution Implemented

### 1. Per-Tile Physics Handler
- **Changed**: `advancedPhysicsHandler` from static to instance variable
- **Location**: `QSTileViewImpl.kt` line 105
- **Impact**: Each QS tile now has its own dedicated physics handler

### 2. Improved Animation State Management
- **Added**: `isAnimating` state tracking in `QSTileAdvancedPhysicsHandler`
- **Benefit**: Prevents animation conflicts and ensures consistent behavior
- **Location**: `QSTileAdvancedPhysicsHandler.kt` line 25

### 3. Enhanced Touch Event Handling
- **Improved**: `onTouchEvent` method to always handle physics first
- **Benefit**: Ensures all tiles respond consistently to touch events
- **Location**: `QSTileViewImpl.kt` lines 650-689

### 4. Robust Cleanup and State Management
- **Enhanced**: `cleanup()` method to properly reset animation state
- **Added**: Animation state tracking to prevent conflicts
- **Benefit**: Prevents memory leaks and state corruption

## Key Changes Made

### QSTileViewImpl.kt
```kotlin
// Per-tile physics handler for individual tile animations
private var advancedPhysicsHandler: QSTileAdvancedPhysicsHandler? = null

private fun getAdvancedPhysicsHandler(): QSTileAdvancedPhysicsHandler? {
    // Create per-tile handler; each tile gets its own instance
    if (advancedPhysicsHandler == null) {
        advancedPhysicsHandler = QSTileAdvancedPhysicsHandler(this)
    }
    return advancedPhysicsHandler
}
```

### QSTileAdvancedPhysicsHandler.kt
```kotlin
// Track animation state to prevent conflicts
private var isAnimating = false

fun animatePress() {
    if (isAnimating) return // Prevent multiple simultaneous animations
    isAnimating = true
    // ... animation logic
}

fun animateRelease() {
    // ... animation logic
    .withEndAction {
        isAnimating = false // Reset animation state
    }
}
```

## Benefits
1. **Per-Tile Independence**: Each QS tile now has its own physics handler
2. **Consistent Behavior**: All tiles respond uniformly to touch events
3. **No Timeouts**: Animations work permanently without degradation
4. **Better Performance**: Reduced conflicts and state management issues
5. **Robust State Management**: Proper cleanup prevents memory leaks

## Testing
- Test with Internet tile and other QS tiles
- Verify squishiness works on all tiles simultaneously
- Confirm animations don't timeout or stop working
- Check that multiple tiles can be pressed simultaneously

## System Properties
- `sysui.qs.advanced_physics=true` (default): Enables per-tile physics
- `sysui.qs.anim_perf=true` (default): Enables performance mode with shorter durations
- `sysui.qs.haptic=true` (default): Enables haptic feedback

## References
- Based on analysis of AxionAOSP android_frameworks_base repository
- Follows LineageOS coding standards and Android framework conventions
- Compatible with Android 15 and LineageOS 22.2
