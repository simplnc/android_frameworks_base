# Hidden Apps Still Visible - Fixed

## Problem Description
Hidden apps were still visible in launchers despite being in the hide list. The main filtering logic was working correctly, but apps were appearing through synthetic activities.

## Root Cause Analysis
The issue was in `LauncherAppsService.java`. The service has logic to create synthetic activities for apps that don't have launcher activities but should still be accessible. The `shouldShowSyntheticActivity()` method was not checking the hide list, so it was creating synthetic activities for hidden apps.

**Key Issue:**
- `ComputerEngine.shouldFilterApplicationCustom()` was working correctly
- `LauncherAppsService.shouldShowSyntheticActivity()` was bypassing the hide list
- Hidden apps appeared as synthetic activities instead of being filtered out

## Solution Applied

### Modified LauncherAppsService.java
**File:** `services/core/java/com/android/server/pm/LauncherAppsService.java`

**Added hide list check to `shouldShowSyntheticActivity()` method:**

```java
private boolean shouldShowSyntheticActivity(UserHandle user, ApplicationInfo appInfo) {
    if (appInfo == null || appInfo.isSystemApp() || appInfo.isUpdatedSystemApp()) {
        return false;
    }
    if (isManagedProfileAdmin(user, appInfo.packageName)) {
        return false;
    }
    // Don't show synthetic activity for apps in the hide list
    if (com.android.internal.util.epic.HideAppListUtils.shouldHideAppList(
            mContext, appInfo.packageName)) {
        return false;
    }
    final AndroidPackage pkg = mPackageManagerInternal.getPackage(appInfo.packageName);
    if (pkg == null) {
        // Should not happen, but we shouldn't be failing if it does
        return false;
    }
    // If app does not have any default enabled launcher activity or any permissions,
    // the app can legitimately have no icon so we do not show the synthetic activity.
    return requestsPermissions(pkg) && hasDefaultEnableLauncherActivity(
            appInfo.packageName);
}
```

**Key Change:**
```java
// Don't show synthetic activity for apps in the hide list
if (com.android.internal.util.epic.HideAppListUtils.shouldHideAppList(
        mContext, appInfo.packageName)) {
    return false;
}
```

## How It Works Now

### Complete Hide App Flow
1. **ComputerEngine Filtering**: `shouldFilterApplicationCustom()` filters apps from package queries
2. **LauncherAppsService Filtering**: `shouldShowSyntheticActivity()` prevents synthetic activities for hidden apps
3. **RecentTasks Filtering**: `isVisibleRecentTask()` hides apps from recent tasks
4. **Settings Integration**: Hide list is loaded from `Settings.Secure.HIDE_APPLIST`

### Synthetic Activity Logic
- **Purpose**: Shows apps that don't have launcher activities but should be accessible
- **Hide List Check**: Now respects the hide list and won't create synthetic activities for hidden apps
- **System Apps**: Still exempted (system apps won't get synthetic activities anyway)

## Testing
After applying this fix:
1. Build the ROM
2. Verify apps in the hide list are completely hidden from launchers
3. Verify hidden apps don't appear as synthetic activities
4. Verify hidden apps don't appear in recent tasks
5. Test that system apps are still accessible
6. Test that apps can still see themselves

## Files Modified
- `services/core/java/com/android/server/pm/LauncherAppsService.java`

## Related Components
- `services/core/java/com/android/server/pm/ComputerEngine.java` - Main filtering logic
- `services/core/java/com/android/server/wm/RecentTasks.java` - Recent tasks filtering
- `core/java/com/android/internal/util/epic/HideAppListUtils.java` - Hide list utility
- `Settings.Secure.HIDE_APPLIST` - Hide list setting

## Previous Fixes Applied
1. **Initial Fix**: Added `shouldFilterApplicationCustom()` call in `ComputerEngine.shouldFilterApplication()`
2. **System App Protection**: Added system app exemption to prevent hiding system apps
3. **Recent Tasks**: Added hide list filtering to recent tasks
4. **Synthetic Activity Fix**: Added hide list check to `LauncherAppsService.shouldShowSyntheticActivity()`

The hide app functionality should now work completely across all contexts.
