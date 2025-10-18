# App Hide List Enhancements

## Changes Made

### 1. Added Session and Ambient Music to Hide List
**Files Modified:**
- `packages/SettingsProvider/res/values/defaults.xml`

**Details:**
Added common package name patterns for Session and Ambient Music apps to the default hide list:
- `com.session.music`
- `com.sessionmusic` 
- `com.session`
- `com.ambient.music`
- `com.ambientmusic`
- `com.ambient`

### 2. Pre-enabled Pocket Mode
**Files Modified:**
- `core/java/android/provider/Settings.java`
- `packages/SettingsProvider/src/android/provider/settings/validators/SecureSettingsValidators.java`
- `packages/SettingsProvider/res/values/defaults.xml`
- `packages/SettingsProvider/src/com/android/providers/settings/DatabaseHelper.java`

**Details:**
- Added `POCKET_MODE_ENABLED` and `ALWAYS_ON_POCKET_MODE_ENABLED` settings to Settings.java
- Added boolean validators for both settings
- Set default values: pocket mode enabled (1), always-on pocket mode disabled (0)
- Added settings loading in DatabaseHelper.java

### 3. Pre-enabled Ambient Mode (Doze)
**Files Modified:**
- `packages/SettingsProvider/res/values/defaults.xml`
- `packages/SettingsProvider/src/com/android/providers/settings/DatabaseHelper.java`

**Details:**
- Set `def_doze_enabled` to 1 (enabled by default)
- Added doze_enabled setting loading in DatabaseHelper.java

### 4. Hide Apps from Recent Tasks/Sidebar
**Files Modified:**
- `services/core/java/com/android/server/wm/RecentTasks.java`

**Details:**
- Modified `isVisibleRecentTask()` method to check hide list
- Apps in the hide list are now filtered out from recent tasks/sidebar
- Uses `HideAppListUtils.shouldHideAppList()` for consistency

## How It Works

### Hide List Behavior
- **Launchers**: Apps in hide list are hidden from all launchers (including default home launcher)
- **Recent Tasks/Sidebar**: Apps in hide list are hidden from recent tasks view
- **System Apps**: Can still see all apps (system/root/shell)
- **Apps Themselves**: Can still see themselves

### Pocket Mode
- **Default State**: Enabled by default
- **Always-On**: Disabled by default (can be enabled manually)
- **Functionality**: Prevents accidental touches when device is in pocket

### Ambient Mode (Doze)
- **Default State**: Enabled by default
- **Functionality**: Shows ambient display when device is charging or in pocket

## Testing
After applying these changes:
1. Build the ROM
2. Verify Session/Ambient Music apps are hidden from launcher and recent tasks
3. Verify pocket mode is enabled by default
4. Verify ambient mode (doze) is enabled by default
5. Test that hidden apps don't appear in recent tasks/sidebar

## Related Components
- `core/java/com/android/internal/util/epic/HideAppListUtils.java` - Utility class for managing hide list
- `services/core/java/org/custom/PocketModeService.java` - Pocket mode implementation
- `Settings.Secure.HIDE_APPLIST` - Secure setting storing comma-separated list of hidden packages
- `Settings.Secure.POCKET_MODE_ENABLED` - Pocket mode setting
- `Settings.Secure.DOZE_ENABLED` - Ambient display setting
