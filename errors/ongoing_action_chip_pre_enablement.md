# Ongoing Action Chip Pre-Enablement Fix

## Problem
The ongoing action chip in the status bar was not visible on the phone after downloading the ROM. The chip was implemented but had visibility issues.

## Root Cause Analysis
1. **Missing Import**: `NotificationListener` class was not imported in `OnGoingActionProgressController.java`
2. **Missing Drawable**: `action_chip_container_background.xml` drawable resource was missing
3. **Settings Dependency**: The chip was dependent on a settings toggle that wasn't properly configured
4. **Layout Issues**: The layout used inconsistent dimensions (sp instead of dp, hardcoded sizes)

## Solution Implemented

### 1. Fixed Missing Import
**File**: `packages/SystemUI/src/com/android/systemui/statusbar/OnGoingActionProgressController.java`
- Added missing import: `import com.android.systemui.statusbar.NotificationListener;`

### 2. Created Missing Drawable Resource
**File**: `packages/SystemUI/res/drawable/action_chip_container_background.xml`
- Created background drawable using `colorAccent` and proper corner radius
- Uses existing `ongoing_activity_chip_corner_radius` dimension

### 3. Pre-Enabled the Chip
**File**: `packages/SystemUI/src/com/android/systemui/statusbar/OnGoingActionProgressController.java`
- Modified `updateSettings()` method to always set `mActionChipEnabled = true`
- Added initialization in constructor: `mActionChipEnabled = true; mActionChipAllowed = true;`
- Commented out original settings-based code for future toggle implementation

### 4. Fixed Layout Dimensions
**File**: `packages/SystemUI/res/layout/status_bar_ongoing_action_chip.xml`
- Changed height from `16sp` to `@dimen/ongoing_activity_chip_height`
- Changed padding from `4sp` to `@dimen/ongoing_activity_chip_side_padding`
- Updated icon size to use `@dimen/ongoing_activity_chip_icon_size`
- Updated spacing to use `@dimen/ongoing_activity_chip_icon_text_padding`
- Changed margin from `layout_marginLeft` to `layout_marginStart`

## How It Works
The ongoing action chip now:
1. **Always Enabled**: The chip is pre-enabled and will show when notifications with progress are detected
2. **Proper Sizing**: Uses consistent dimensions from the existing ongoing activity chip system
3. **Visual Consistency**: Matches the design of other status bar chips with proper background and spacing
4. **Progress Tracking**: Displays app icon and progress bar for notifications with `EXTRA_PROGRESS` and `EXTRA_PROGRESS_MAX`

## Testing
To test the ongoing action chip:
1. Install an app that shows progress notifications (e.g., file download apps)
2. Trigger a progress notification
3. The chip should appear in the status bar showing the app icon and progress bar
4. The chip should disappear when the notification is removed or completed

## Future Toggle Implementation
To add a toggle later, uncomment the settings-based code in `updateSettings()` method and add the toggle to SystemUI Tuner preferences.

## Files Modified
- `packages/SystemUI/src/com/android/systemui/statusbar/OnGoingActionProgressController.java`
- `packages/SystemUI/res/layout/status_bar_ongoing_action_chip.xml`
- `packages/SystemUI/res/drawable/action_chip_container_background.xml` (created)

## Compatibility
- Compatible with LineageOS 22.2 (Android 15)
- Uses existing LineageOS resources and dimensions
- Follows LineageOS coding standards and conventions
