# Missing Drawable Resources Fix

## Issue Description
The build was failing with the following errors due to missing drawable resources:

```
frameworks/base/packages/SystemUI/res/layout/quick_settings_brightness_dialog.xml:41: error: resource drawable/ic_qs_brightness_auto_off_new (aka com.android.systemui:drawable/ic_qs_brightness_auto_off_new) not found.
frameworks/base/packages/SystemUI/res/layout/volume_dialog_legacy.xml:104: error: resource drawable/ic_app_volume (aka com.android.systemui:drawable/ic_app_volume) not found.
frameworks/base/packages/SystemUI/res/layout/volume_dialog_legacy_sw600dp.xml:104: error: resource drawable/ic_app_volume (aka com.android.systemui:drawable/ic_app_volume) not found.
frameworks/base/packages/SystemUI/res/layout-land/volume_dialog_legacy.xml:104: error: resource drawable/ic_app_volume (aka com.android.systemui:drawable/ic_app_volume) not found.
error: failed linking file resources.
```

## Root Cause
The layout files were referencing drawable resources that didn't exist in the SystemUI drawable directory:

1. **`ic_qs_brightness_auto_off_new`**: Referenced in `quick_settings_brightness_dialog.xml` but only `ic_qs_brightness_auto_off.xml` existed
2. **`ic_app_volume`**: Referenced in multiple volume dialog layout files but didn't exist at all

## Solution Applied
Created the missing drawable resources:

### 1. `ic_qs_brightness_auto_off_new.xml`
- **Location**: `packages/SystemUI/res/drawable/ic_qs_brightness_auto_off_new.xml`
- **Design**: A diamond-shaped icon with inner circle, representing brightness auto-off functionality
- **Size**: 24dp x 24dp vector drawable
- **Tint**: Uses `?attr/colorOnSurface` for proper theming

### 2. `ic_app_volume.xml`
- **Location**: `packages/SystemUI/res/drawable/ic_app_volume.xml`
- **Design**: Speaker icon with sound waves, representing application volume control
- **Size**: 24dp x 24dp vector drawable
- **Tint**: Uses `?attr/colorOnSurface` for proper theming

## Files Modified
- `packages/SystemUI/res/drawable/ic_qs_brightness_auto_off_new.xml` (created)
- `packages/SystemUI/res/drawable/ic_app_volume.xml` (created)

## Build Impact
This fix resolves the aapt2 resource linking errors that were preventing the SystemUI module from building. The missing drawables are now available for the layout files to reference.

## Current Status
✅ **Drawable Resources Fixed**: The missing drawable resources have been created and the SystemUI resource linking stage now completes successfully.

⚠️ **New Issue Encountered**: After fixing the drawable resources, the build progressed to 82% completion but encountered a "ninja fifo didn't finish after 5s" timeout error.

## Next Steps for Ninja Timeout
The ninja fifo timeout is a different issue that can occur due to:

1. **System Resource Contention**: High CPU/memory usage during build
2. **Build Process Hanging**: A specific build step getting stuck
3. **Ninja State Corruption**: Corrupted ninja build state files

### Recommended Actions:
1. **Clean Ninja State**: Remove ninja state files to force a fresh build
   ```bash
   rm -f out/.ninja_fifo out/.ninja_log out/.ninja_deps
   ```

2. **Reduce Build Parallelism**: Use fewer parallel jobs to reduce resource contention
   ```bash
   m SystemUI -j4  # Instead of default -j10
   ```

3. **Monitor System Resources**: Check CPU, memory, and disk I/O during build
   ```bash
   htop  # Monitor system resources
   iostat 1  # Monitor disk I/O
   ```

4. **Try Incremental Build**: Build specific modules instead of full system
   ```bash
   m SystemUI  # Build just SystemUI first
   ```

## Verification
After applying this fix, the build should proceed past the SystemUI resource linking stage. The icons will appear in:
- Quick Settings brightness dialog (brightness auto-off icon)
- Volume dialog legacy layouts (app volume icon)

## Notes
- The icons use standard Material Design patterns
- Both icons are vector drawables for scalability across different screen densities
- The tint attribute ensures proper theming support
- The ninja timeout issue is separate from the drawable resources and requires different troubleshooting approaches
