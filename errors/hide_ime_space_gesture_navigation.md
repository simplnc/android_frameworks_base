# Hide IME Space Feature Implementation

## Problem
When using gesture navigation, there's unnecessary space below the IME (Input Method Editor) that creates a gap between the keyboard and the bottom edge of the screen. This wastes screen real estate and creates a poor user experience.

## Solution
Implemented the Hide IME Space feature that automatically activates when using gesture navigation, removing the gap below the IME and hiding the rotating IME back button.

## Implementation Details

### Files Modified

1. **core/java/android/provider/Settings.java**
   - Added `HIDE_IME_SPACE_ENABLE` constant
   - Added to `PRIVATE_SETTINGS` for system-level access

2. **core/res/res/values/dimens.xml**
   - Added `navigation_bar_frame_height_hide_ime` dimension (0dp)
   - Added `navigation_bar_height_hide_ime` dimension (12dp)
   - Used when Hide IME Space is enabled

3. **core/res/res/values/symbols.xml**
   - Added `navigation_bar_frame_height_hide_ime` symbol for SystemUI access
   - Added `navigation_bar_height_hide_ime` symbol for SystemUI access

4. **packages/SystemUI/src/com/android/systemui/navigationbar/views/NavigationBarView.java**
   - Added missing `UserHandle` import statement
   - Modified `updateNavButtonIcons()` to hide IME switcher when Hide IME Space is enabled
   - Modified back button visibility to hide when Hide IME Space is enabled in gesture mode
   - Updated `onMeasure()` to properly use `navigation_bar_frame_height_hide_ime` dimension
   - Updated `onMeasure()` to use specific `navigation_bar_height_hide_ime` dimension instead of calculated height
   - Updated `getNavBarHeight()` to properly handle IME space hiding
   - Added `isHideIMESpaceEnabled()` method that automatically enables for gesture navigation

5. **packages/SystemUI/src/com/android/systemui/navigationbar/views/buttons/KeyButtonDrawable.java**
   - Added try-catch block to handle missing resources gracefully
   - Returns null if drawable resource not found

6. **packages/SystemUI/src/com/android/systemui/navigationbar/views/buttons/ContextualButton.java**
   - Added null check before setting drawable to prevent crashes

### Key Features

- **Automatic Activation**: Feature automatically enables when using gesture navigation
- **No Toggle Required**: No user setting needed - works automatically
- **Gesture-Only**: Only affects gesture navigation mode, not button navigation
- **Proper Resource Usage**: Uses specific dimensions for consistent behavior
- **Crash Prevention**: Handles missing resources gracefully
- **Clean Implementation**: Follows Android development best practices

### Behavior

- **Gesture Navigation**: IME space is hidden, back button is hidden, IME switcher is hidden
- **Button Navigation**: Normal behavior maintained
- **Resource Safety**: Missing drawables handled gracefully without crashes

## References

Based on commits from:
- [Project-AnfangX/frameworks_base commit eaf0915](https://github.com/Project-AnfangX/frameworks_base/commit/eaf0915bfaa56c2a9936786f294687132cc7746c)
- [Albinoman887/android_packages_apps_Settings commit 4775b73](https://github.com/Albinoman887/android_packages_apps_Settings-new/commit/4775b73a439be4a19955fef3896c93e19955c86e)
- [ObsidianOS-AOSP/android_vendor_addons commit b0f50b0](https://github.com/ObsidianOS-AOSP/android_vendor_addons/commit/b0f50b0163e13d8a8c08a072dc9dc78a05083459)

## Testing

- Test with gesture navigation enabled
- Verify IME space is hidden when keyboard appears
- Confirm back button is hidden in gesture mode
- Test with different keyboard apps
- Verify no crashes with missing resources

## Status
✅ **COMPLETED** - All changes implemented and tested for build compatibility
