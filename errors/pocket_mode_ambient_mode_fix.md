# Pocket Mode and Ambient Mode Not Working - Fixed

## Problem Description
Both pocket mode and ambient mode were not working despite being pre-enabled in the settings. The features were configured but not functioning.

## Root Cause Analysis

### Pocket Mode Issue
1. **PocketModeService not initialized**: The `PocketModeService` class existed but was never initialized in the system startup process
2. **Missing system ready call**: The service required `setSystemReady()` to be called before it could function

### Ambient Mode Issue
1. **Missing doze component**: The `config_dozeComponent` was empty, preventing ambient display from working
2. **DozeService not configured**: SystemUI's DozeService needed to be specified as the doze component

## Solution Applied

### 1. Initialize PocketModeService
**File:** `services/java/com/android/server/SystemServer.java`

Added PocketModeService initialization in two places:

**Initialization during core services startup:**
```java
// Initialize PocketModeService
if (!isWatch && !isTv && !isAutomotive) {
    t.traceBegin("StartPocketModeService");
    try {
        org.custom.server.PocketModeService.getInstance(context);
    } catch (Throwable e) {
        Slog.e("System", "Failure starting PocketModeService", e);
    }
    t.traceEnd();
}
```

**System ready call after ActivityManagerService is ready:**
```java
// Make PocketModeService ready
t.traceBegin("MakePocketModeServiceReady");
try {
    org.custom.server.PocketModeService.getInstance(context).setSystemReady();
} catch (Throwable e) {
    reportWtf("making PocketModeService ready", e);
}
t.traceEnd();
```

### 2. Configure Ambient Mode Component
**File:** `core/res/res/values/config.xml`

Set the doze component to SystemUI's DozeService:
```xml
<string name="config_dozeComponent" translatable="false">com.android.systemui/.doze.DozeService</string>
```

## How It Works Now

### Pocket Mode
- **Initialization**: PocketModeService is initialized during system startup
- **System Ready**: Service is marked as ready after ActivityManagerService is ready
- **Settings Integration**: Service listens to `pocket_mode_enabled` and `always_on_pocket_mode_enabled` settings
- **Sensor Detection**: Uses proximity, light, accelerometer sensors to detect pocket state
- **Overlay Display**: Shows overlay when device is detected in pocket

### Ambient Mode (Doze)
- **Component Configuration**: DozeService is properly configured as the doze component
- **Settings Integration**: Respects `doze_enabled` setting (pre-enabled by default)
- **SystemUI Integration**: Works with SystemUI's ambient display features
- **Power Management**: Integrates with PowerManagerService for doze states

## Testing
After applying these fixes:
1. Build the ROM
2. Verify pocket mode activates when device is placed in pocket
3. Verify ambient display shows when device is charging or in pocket
4. Test pocket mode settings in system settings
5. Test ambient display settings in system settings
6. Verify both features work together without conflicts

## Files Modified
- `services/java/com/android/server/SystemServer.java` - Added PocketModeService initialization
- `core/res/res/values/config.xml` - Configured doze component

## Related Components
- `services/core/java/org/custom/PocketModeService.java` - Pocket mode implementation
- `packages/SystemUI/src/com/android/systemui/doze/DozeService.java` - Ambient display implementation
- `Settings.Secure.POCKET_MODE_ENABLED` - Pocket mode setting
- `Settings.Secure.DOZE_ENABLED` - Ambient display setting
- `packages/SystemUI/src/com/android/systemui/pocket/PocketStateReceiver.java` - Pocket state receiver
