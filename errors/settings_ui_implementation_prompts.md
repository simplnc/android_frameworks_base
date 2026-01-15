# Settings UI Implementation Prompts for LineageOS Sandbox

## Overview
This document contains detailed prompts for implementing the Settings UI for the LineageOS Sandbox service. Each prompt is designed for a separate implementation phase to ensure build safety and incremental development.

## Prompt 1: Core Settings Fragment

```
Implement the main Sandbox Settings fragment for the LineageOS Settings app.

**Location**: packages/apps/Settings/src/com/android/settings/privacy/sandbox/

**Files to create**:
- SandboxSettingsFragment.java (main fragment)
- sandbox_settings.xml (layout)
- SandboxSettingsController.java (business logic)

**Requirements**:
1. **Fragment Structure**:
   - Extend SettingsPreferenceFragment
   - Handle lifecycle properly
   - Implement search provider interface

2. **Layout (sandbox_settings.xml)**:
   - Use PreferenceScreen as root
   - Master toggle SwitchPreference
   - PreferenceCategory for locked apps
   - ButtonPreference for "Add app to lock"
   - Emergency options category

3. **Functionality**:
   - Check MANAGE_DEVICE_POLICY_SANDBOX permission
   - Show permission request dialog if needed
   - Load and display current locked apps
   - Handle master toggle state
   - Navigate to app picker on "Add app" click

4. **Integration**:
   - Add to Settings search index
   - Register in privacy dashboard
   - Handle configuration changes
   - Support dark/light themes

5. **Build Safety**:
   - Use only existing Settings dependencies
   - Follow SettingsPreferenceFragment patterns
   - No new external libraries
   - Test compilation before committing

**Example structure**:
```java
public class SandboxSettingsFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_MASTER_TOGGLE = "sandbox_master_toggle";
    private static final String KEY_LOCKED_APPS = "locked_apps_category";
    private static final String KEY_ADD_APP = "add_app_to_lock";

    private LineageSandboxManager mSandboxManager;
    private SwitchPreference mMasterToggle;
    private PreferenceCategory mLockedAppsCategory;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.sandbox_settings, rootKey);
        mSandboxManager = getSandboxManager();

        setupPreferences();
        refreshLockedApps();
    }

    private void setupPreferences() {
        mMasterToggle = findPreference(KEY_MASTER_TOGGLE);
        mLockedAppsCategory = findPreference(KEY_LOCKED_APPS);

        // Setup master toggle
        mMasterToggle.setOnPreferenceChangeListener(this);

        // Setup add app button
        Preference addAppPref = findPreference(KEY_ADD_APP);
        addAppPref.setOnPreferenceClickListener(pref -> {
            showAppPicker();
            return true;
        });
    }
}
```

**Testing checklist**:
- Fragment loads without crashes
- Permission dialog appears when needed
- Master toggle works
- Navigation to app picker works
- Settings search finds the new screen
```

## Prompt 2: App Picker Implementation

```
Implement the app picker for selecting apps to lock in the sandbox.

**Location**: packages/apps/Settings/src/com/android/settings/privacy/sandbox/

**Files to create**:
- AppPickerFragment.java
- AppPickerAdapter.java
- AppListItem.java
- app_picker.xml

**Requirements**:
1. **Fragment Structure**:
   - Extend SettingsPreferenceFragment or use RecyclerView
   - Async loading of app list
   - Search/filter functionality

2. **App Loading**:
   - Use PackageManager to get installed apps
   - Filter criteria:
     - User-installed apps only (not system apps)
     - Launchable activities exist
     - Not currently locked
     - Not in exclusion list (system UI, settings, etc.)
   - Load app icons, names, package names

3. **UI Components**:
   - SearchView at top
   - RecyclerView with app list
   - App item: icon + name + package + checkbox
   - Action buttons: Cancel, Add Selected

4. **Performance**:
   - Load apps asynchronously
   - Use ViewHolder pattern
   - Efficient icon loading (don't load all at once)
   - Handle large app lists (100+ apps)

5. **Selection Logic**:
   - Multi-select with checkboxes
   - Select all/none options
   - Validate selections before returning
   - Return selected package names to parent fragment

**Example AppListItem**:
```java
public class AppListItem implements Comparable<AppListItem> {
    public final String packageName;
    public final CharSequence appName;
    public final Drawable icon;
    public boolean isSelected;

    public AppListItem(String packageName, CharSequence appName, Drawable icon) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
    }

    @Override
    public int compareTo(AppListItem other) {
        return appName.toString().compareToIgnoreCase(other.appName.toString());
    }
}
```

**Build safety**:
- Use existing Settings RecyclerView patterns
- Handle PackageManager exceptions
- Test with devices having many apps
- Ensure no memory leaks
```

## Prompt 3: Locked Apps Management

```
Implement the locked apps display and management UI.

**Location**: packages/apps/Settings/src/com/android/settings/privacy/sandbox/

**Files to create**:
- LockedAppsFragment.java (or integrate into main fragment)
- LockedAppAdapter.java
- locked_app_item.xml

**Requirements**:
1. **Display Logic**:
   - Show all currently locked apps
   - Display: icon, name, unlock button
   - Empty state when no apps locked

2. **App Information**:
   - Load app info from PackageManager
   - Handle apps that may have been uninstalled
   - Show package name for debugging

3. **Unlock Functionality**:
   - Individual unlock buttons
   - Confirmation dialog before unlock
   - Bulk unlock option
   - Handle unlock failures gracefully

4. **Real-time Updates**:
   - Refresh when apps are locked/unlocked
   - Handle app install/remove events
   - Update UI immediately after operations

**Example locked app item**:
```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="16dp"
    android:orientation="horizontal">

    <ImageView
        android:id="@+id/app_icon"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_marginEnd="16dp" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/app_name"
            android:textSize="16sp"
            android:textColor="?android:textColorPrimary" />

        <TextView
            android:id="@+id/package_name"
            android:textSize="12sp"
            android:textColor="?android:textColorSecondary" />

    </LinearLayout>

    <Button
        android:id="@+id/unlock_button"
        android:text="@string/sandbox_unlock"
        style="?android:buttonStyleSmall" />

</LinearLayout>
```

**Error handling**:
- Handle PackageManager.NameNotFoundException
- Show appropriate messages for failed operations
- Graceful degradation for missing app info
```

## Prompt 4: Security Integration

```
Add security features and authentication to the sandbox Settings UI.

**Requirements**:
1. **Authentication Integration**:
   - Use BiometricPrompt for sensitive operations
   - Require authentication for:
     - Changing master toggle
     - Adding app locks
     - Unlocking apps
     - Clearing all locks

2. **Authentication Flow**:
   - Check if device has biometric/PIN
   - Show appropriate auth prompt
   - Handle auth success/failure
   - Allow fallback to device credentials

3. **Audit Logging**:
   - Log all security operations
   - Include timestamp, operation type, app details
   - Store securely (consider using system log)

4. **Permission Handling**:
   - Check MANAGE_DEVICE_POLICY_SANDBOX permission
   - Request permission if missing
   - Show clear error messages
   - Handle permission revocation

**Example authentication code**:
```java
private void authenticateAndLockApp(String packageName) {
    BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.sandbox_auth_title))
            .setSubtitle(getString(R.string.sandbox_auth_subtitle, getAppName(packageName)))
            .setNegativeButtonText(getString(R.string.cancel))
            .build();

    BiometricPrompt biometricPrompt = new BiometricPrompt(this,
            ContextCompat.getMainExecutor(requireContext()),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    performLockApp(packageName);
                }

                @Override
                public void onAuthenticationFailed() {
                    showAuthFailedMessage();
                }
            });

    biometricPrompt.authenticate(promptInfo);
}
```

**Security considerations**:
- Never store auth results
- Handle auth timeouts
- Respect user security settings
- Clear sensitive data on fragment destroy
```

## Prompt 5: Settings Integration and Polish

```
Complete the Settings integration and add finishing touches.

**Requirements**:
1. **Settings Search Integration**:
   - Add to global search index
   - Provide search keywords
   - Handle search result clicks

2. **Settings Dashboard Integration**:
   - Add to Privacy dashboard
   - Show summary of locked apps
   - Handle dashboard clicks

3. **Resource Management**:
   - Add all required strings to Settings strings.xml
   - Add icons if needed
   - Handle RTL layouts
   - Support multiple screen densities

4. **Accessibility**:
   - Add content descriptions
   - Handle screen readers
   - Support keyboard navigation
   - Meet accessibility guidelines

5. **Error Handling & UX**:
   - Show loading states
   - Handle service unavailable
   - Provide clear error messages
   - Add help text and tooltips

6. **Multi-user Support**:
   - Handle per-user settings
   - Respect user restrictions
   - Handle user switches

**Strings to add**:
```xml
<!-- Sandbox Settings -->
<string name="sandbox_settings_title">Sandbox</string>
<string name="sandbox_settings_summary">Lock apps and control access</string>
<string name="sandbox_master_toggle_title">Enable app locking</string>
<string name="sandbox_locked_apps_title">Locked apps</string>
<string name="sandbox_add_app_title">Add app to lock</string>
<string name="sandbox_clear_all_title">Clear all locks</string>
<string name="sandbox_unlock">Unlock</string>
<string name="sandbox_lock">Lock</string>
<string name="sandbox_no_locked_apps">No apps are currently locked</string>
<string name="sandbox_auth_title">Authentication required</string>
<string name="sandbox_auth_subtitle">Authenticate to lock %1$s</string>
```

**Build verification**:
- Test Settings app compilation
- Verify search integration
- Test on different screen sizes
- Confirm no resource conflicts
- Validate accessibility
```

## Implementation Order

1. **Phase 1**: Core fragment and basic UI
2. **Phase 2**: App picker functionality
3. **Phase 3**: Locked apps management
4. **Phase 4**: Security and authentication
5. **Phase 5**: Integration and polish

## Build Safety Measures

### Pre-implementation Checks
1. **Dependency Analysis**: Ensure all required classes exist in Settings app
2. **Resource Conflicts**: Check for duplicate string/icon names
3. **API Compatibility**: Verify APIs exist in target Android version
4. **Import Verification**: Confirm all imports resolve correctly

### During Implementation
1. **Incremental Builds**: Build after each major change
2. **Clean Builds**: Test with `make clean` to catch hidden issues
3. **Multi-device Testing**: Test on different device configurations
4. **Resource Validation**: Run resource validation tools

### Post-implementation
1. **Full Build Test**: Complete build from clean state
2. **Runtime Testing**: Launch Settings app and test functionality
3. **Compatibility Test**: Test on target LineageOS build
4. **Performance Check**: Monitor for UI lag or memory issues

## Error Prevention

### Common Build Breakers to Avoid
1. **Missing Imports**: Always check imports exist
2. **Resource Not Found**: Use existing resources or add properly
3. **Class Not Found**: Verify class exists in Settings app
4. **Method Not Found**: Check API levels and availability
5. **Permission Issues**: Handle permissions gracefully

### Debugging Tips
1. **Log Build Output**: Capture full build logs for analysis
2. **Check Dependencies**: Use `find` to verify file locations
3. **Test Fragments Independently**: Test each fragment separately
4. **Use Existing Patterns**: Copy working code from similar Settings screens

### Rollback Plan
1. **Feature Flags**: Consider using feature flags to disable if needed
2. **Clean Removal**: Know exactly which files to remove
3. **Backup Originals**: Keep backups of modified files
4. **Incremental Commits**: Commit changes in small, testable units

This implementation plan ensures the Settings UI integrates safely with the LineageOS build system while providing a complete, secure user experience for the sandbox features.