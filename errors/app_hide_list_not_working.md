# App Hide List Not Working

## Problem Description
The app hide list functionality was not working - apps that were supposed to be hidden from the launcher were still visible.

## Root Cause Analysis
The issue was in `services/core/java/com/android/server/pm/ComputerEngine.java`. The hide app list logic was implemented in a method called `shouldFilterApplicationCustom()` but this method was never being called.

The main `shouldFilterApplication()` method was delegating directly to `mAppsFilter.shouldFilterApplication()` without checking the custom hide list first.

## Solution Applied
Added a call to `shouldFilterApplicationCustom()` in the main `shouldFilterApplication()` method before the AppsFilter check:

```java
// Check if app should be hidden from app list
if (shouldFilterApplicationCustom(ps, callingUid, userId)) {
    return true;
}
```

## Files Modified
- `services/core/java/com/android/server/pm/ComputerEngine.java`

## How It Works
1. When an app queries for installed applications, `shouldFilterApplication()` is called
2. The method now first checks if the app should be hidden using `shouldFilterApplicationCustom()`
3. If the app is in the hide list (stored in `Settings.Secure.HIDE_APPLIST`), it returns `true` to filter it out
4. Otherwise, it continues with the normal AppsFilter logic

## Hide List Behavior
- **All launchers** (including default home launcher) will hide apps in the hide list
- **System apps** (system/root/shell) can still see all apps
- **Apps themselves** can still see themselves

## Testing
After applying this fix:
1. Build the ROM
2. Add apps to the hide list via Settings
3. Verify that hidden apps no longer appear in ANY launcher (including default home launcher)
4. Verify that system apps can still see hidden apps
5. Verify that apps can still see themselves

## Related Components
- `core/java/com/android/internal/util/epic/HideAppListUtils.java` - Utility class for managing hide list
- `Settings.Secure.HIDE_APPLIST` - Secure setting storing comma-separated list of hidden packages
- `packages/SettingsProvider/res/values/defaults.xml` - Default hidden apps configuration

## Commit Reference
Original hide app list implementation: `682deff3c907`
Fix applied: Current working directory changes
