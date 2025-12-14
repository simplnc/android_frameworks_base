# AOD and Ambient Display Conflict Fix

## Problem
Users reported that both Always On Display (AOD) and the system's ambient display notification pulses were active simultaneously, causing conflicts and potentially excessive battery drain.

## Root Cause
The `pulseOnNotificationEnabled()` method in `AmbientDisplayConfiguration.java` only checked if notification pulses were enabled and available, but did not consider whether AOD was already active. This allowed both features to run concurrently:

- `Settings.Secure.DOZE_ALWAYS_ON` (AOD) = 1
- `Settings.Secure.DOZE_ENABLED` (notification pulses) = 1

When AOD is enabled, it continuously displays notifications and time, making additional notification pulses redundant and potentially confusing.

## Solution
Modified `AmbientDisplayConfiguration.pulseOnNotificationEnabled()` to return `false` when AOD is enabled:

```java
public boolean pulseOnNotificationEnabled(int user) {
    return boolSettingDefaultOn(Settings.Secure.DOZE_ENABLED, user)
            && pulseOnNotificationAvailable()
            && !alwaysOnEnabled(user);  // Added this check
}
```

## Impact
- When AOD is enabled, notification pulses are automatically disabled
- Users can still enable notification pulses when AOD is disabled
- Maintains backward compatibility for users who only want notification pulses
- Prevents UI conflicts and potential battery drain from dual ambient display mechanisms

## Testing
- Verified that enabling AOD disables notification pulses
- Confirmed that disabling AOD allows notification pulses to work
- Checked that the Settings UI properly reflects the state changes

## Files Changed
- `frameworks/base/core/java/android/hardware/display/AmbientDisplayConfiguration.java`

## Risk Assessment
Low risk - this change only affects the logic for enabling notification pulses when AOD is already active. It doesn't break existing functionality, just prevents conflicting behavior.
