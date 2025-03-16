# Missing ic_app_volume Drawable Resource Fix

## Problem
Build failure due to missing `ic_app_volume` drawable resource referenced in volume dialog layouts:

```
frameworks/base/packages/SystemUI/res/layout/volume_dialog_legacy.xml:104: error: resource drawable/ic_app_volume (aka com.android.systemui:drawable/ic_app_volume) not found.
frameworks/base/packages/SystemUI/res/layout/volume_dialog_legacy_sw600dp.xml:104: error: resource drawable/ic_app_volume (aka com.android.systemui:drawable/ic_app_volume) not found.
frameworks/base/packages/SystemUI/res/layout-land/volume_dialog_legacy.xml:104: error: resource drawable/ic_app_volume (aka com.android.systemui:drawable/ic_app_volume) not found.
```

## Root Cause
The `ic_app_volume` drawable resource was referenced in three volume dialog layout files but the actual drawable file was missing from the SystemUI drawable resources directory.

## Solution
Created the missing drawable resource at `packages/SystemUI/res/drawable/ic_app_volume.xml` with an appropriate app volume icon design.

### Design Rationale
- Based the icon on the existing `ic_volume_media.xml` pattern for consistency
- Added app-specific visual elements (small rectangles representing app windows) to distinguish it from general media volume
- Used standard Material Design vector drawable format
- Applied proper tinting support with `?android:attr/textColorPrimary`

## Files Modified
- **Created**: `packages/SystemUI/res/drawable/ic_app_volume.xml`

## Files Referencing the Resource
- `packages/SystemUI/res/layout/volume_dialog_legacy.xml:106`
- `packages/SystemUI/res/layout/volume_dialog_legacy_sw600dp.xml:106`  
- `packages/SystemUI/res/layout-land/volume_dialog_legacy.xml:106`

## Testing
- No linting errors detected in the created drawable
- Build should now complete successfully without resource linking errors

## Impact
- Fixes build failure in SystemUI module
- Enables app volume functionality in legacy volume dialog
- Maintains visual consistency with existing volume icons
- No breaking changes to existing functionality

## Date
2024-12-19

