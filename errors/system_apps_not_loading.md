# System Default Apps Not Loading - Fixed

## Problem Description
System default apps were not loading properly, likely due to the hide app list functionality incorrectly hiding system apps.

## Root Cause Analysis
The issue was in the hide app list implementation:

1. **System apps were included in hide list**: The default hide list contained system apps like:
   - `org.chromium.webview_shell` (system webview shell)
   - `com.android.messaging` (system messaging app)
   - `com.android.athena` (system app)

2. **Incorrect filtering logic**: The `shouldFilterApplicationCustom()` method only checked if the caller was a system app, but didn't check if the target app itself was a system app.

3. **Recent tasks also affected**: The recent tasks filtering logic didn't exempt system apps from hiding.

## Solution Applied

### 1. Added System App Protection
**File:** `services/core/java/com/android/server/pm/ComputerEngine.java`

Added a check to prevent system apps from being hidden:
```java
// Don't hide system apps - they should always be accessible
if (ps.getPkg() != null && ps.getPkg().isSystem()) {
    return false;
}
```

### 2. Fixed Recent Tasks Filtering
**File:** `services/core/java/com/android/server/wm/RecentTasks.java`

Added system app protection to recent tasks filtering:
```java
// Don't hide system apps from recent tasks
try {
    ApplicationInfo appInfo = mService.mContext.getPackageManager()
            .getApplicationInfo(packageName, 0);
    if (appInfo != null && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
        // System app - don't hide
    } else if (com.android.internal.util.epic.HideAppListUtils.shouldHideAppList(
            mService.mContext, packageName)) {
        return false;
    }
} catch (Exception e) {
    // If we can't get app info, check hide list anyway
    if (com.android.internal.util.epic.HideAppListUtils.shouldHideAppList(
            mService.mContext, packageName)) {
        return false;
    }
}
```

### 3. Removed System Apps from Hide List
**File:** `packages/SettingsProvider/res/values/defaults.xml`

Removed system apps from the default hide list:
- Removed: `org.chromium.webview_shell`
- Removed: `com.android.messaging` 
- Removed: `com.android.athena`

## How It Works Now
- **System apps**: Never hidden, always accessible to all callers
- **Third-party apps**: Can be hidden via hide list
- **Launchers**: System apps always visible, third-party apps respect hide list
- **Recent tasks**: System apps always visible, third-party apps respect hide list

## Testing
After applying this fix:
1. Build the ROM
2. Verify system apps (Messaging, WebView Shell, etc.) are visible in launcher
3. Verify system apps appear in recent tasks
4. Verify third-party apps can still be hidden via hide list
5. Test that Session/Ambient Music apps are properly hidden

## Files Modified
- `services/core/java/com/android/server/pm/ComputerEngine.java`
- `services/core/java/com/android/server/wm/RecentTasks.java`
- `packages/SettingsProvider/res/values/defaults.xml`

## Related Components
- `core/java/com/android/internal/util/epic/HideAppListUtils.java` - Hide list utility
- `Settings.Secure.HIDE_APPLIST` - Hide list setting
- System app detection via `ApplicationInfo.FLAG_SYSTEM`
