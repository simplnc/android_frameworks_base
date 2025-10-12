# FooterPreference Icon Removal - Keep Icon Space

## Problem Description
The user wanted to remove the icon from `FooterPreference` in SettingsLib while keeping the icon space to maintain proper layout spacing.

## Root Cause Analysis
The original implementation controlled the visibility of the entire `icon_frame` container, which would collapse the space when the icon was hidden. This caused layout issues when the icon was not visible.

## Solution Applied

### Modified FooterPreference.java
**File:** `packages/SettingsLib/FooterPreference/src/com/android/settingslib/widget/FooterPreference.java`

**Changes Made:**

1. **Changed default icon visibility:**
```java
@VisibleForTesting int mIconVisibility = View.GONE;
```
Changed from `View.VISIBLE` to `View.GONE` to hide the icon by default.

2. **Modified onBindViewHolder method:**
```java
View iconFrame = holder.itemView.findViewById(R.id.icon_frame);
View icon = holder.itemView.findViewById(android.R.id.icon);
if (iconFrame != null && icon != null) {
    // Keep the icon frame visible to maintain spacing, but hide the actual icon
    iconFrame.setVisibility(View.VISIBLE);
    icon.setVisibility(mIconVisibility);
}
```

**Key Changes:**
- `iconFrame` is always kept visible (`View.VISIBLE`) to maintain spacing
- Only the actual `icon` (ImageView) visibility is controlled by `mIconVisibility`
- This preserves the layout structure while hiding the icon content

## How It Works Now

### Layout Structure
- **Icon Frame**: Always visible, maintains 56dp minimum width and padding
- **Icon ImageView**: Visibility controlled by `mIconVisibility` (default: `View.GONE`)
- **Text Content**: Positioned correctly with proper spacing regardless of icon visibility

### API Behavior
- `setIconVisibility(View.GONE)` - Hides icon but keeps space
- `setIconVisibility(View.VISIBLE)` - Shows icon in the reserved space
- `setIconVisibility(View.INVISIBLE)` - Hides icon but keeps space (same as GONE for this use case)

## Benefits
1. **Consistent Layout**: Footer preferences maintain consistent spacing
2. **Flexible Control**: Developers can still show/hide icons as needed
3. **Backward Compatibility**: Existing code using `setIconVisibility()` continues to work
4. **Clean Appearance**: No icon clutter while maintaining proper text alignment

## Testing
After applying this change:
1. Build the ROM
2. Verify FooterPreference instances show no icon by default
3. Verify text content is properly aligned with consistent spacing
4. Test `setIconVisibility(View.VISIBLE)` to ensure icons can still be shown when needed
5. Check various settings screens that use FooterPreference for consistent appearance

## Files Modified
- `packages/SettingsLib/FooterPreference/src/com/android/settingslib/widget/FooterPreference.java`

## Related Components
- `packages/SettingsLib/FooterPreference/res/layout/preference_footer.xml` - Layout structure
- `R.id.icon_frame` - Icon container that maintains spacing
- `android.R.id.icon` - ImageView that displays the actual icon
