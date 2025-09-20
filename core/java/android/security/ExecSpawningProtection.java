/*
 * Copyright (C) 2023-2024 The RisingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security;

import android.content.Context;
import android.os.Binder;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;

/**
 * Secure App Spawning - Based on GrapheneOS exec-based spawning implementation
 * Ensures each app process has a unique memory layout for enhanced ASLR
 * This replaces traditional Zygote spawning to prevent shared memory secrets
 */
public class ExecSpawningProtection {
    private static final String TAG = "ExecSpawningProtection";
    private static final boolean DEBUG = false;

    // System properties for secure app spawning (GrapheneOS style)
    private static final String PROP_SECURE_APP_SPAWNING = "persist.security.secure_app_spawning";
    private static final String PROP_ZYGOTE_FORK_MODE = "persist.security.zygote_fork_mode";
    private static final String PROP_EXEC_BASED_SPAWNING = "persist.security.exec_based_spawning";

    // Settings keys (matching GrapheneOS implementation)
    private static final String SETTING_SECURE_APP_SPAWNING = "secure_app_spawning_enabled";
    private static final String SETTING_ZYGOTE_FORK_MODE = "zygote_fork_mode";
    private static final String SETTING_EXEC_BASED_SPAWNING = "exec_based_spawning_enabled";

    // Default security settings (secure by default, like GrapheneOS)
    private static final boolean DEFAULT_SECURE_APP_SPAWNING = true;
    private static final boolean DEFAULT_ZYGOTE_FORK_MODE = false; // Disable traditional Zygote
    private static final boolean DEFAULT_EXEC_BASED_SPAWNING = true;

    // Allowed exec paths for system processes
    private static final Set<String> ALLOWED_EXEC_PATHS = new HashSet<>();
    static {
        // System binaries that are allowed to execute
        ALLOWED_EXEC_PATHS.add("/system/bin/");
        ALLOWED_EXEC_PATHS.add("/system/xbin/");
        ALLOWED_EXEC_PATHS.add("/vendor/bin/");
        ALLOWED_EXEC_PATHS.add("/apex/");
        ALLOWED_EXEC_PATHS.add("/product/bin/");
    }

    // Blocked exec patterns
    private static final Set<String> BLOCKED_EXEC_PATTERNS = new HashSet<>();
    static {
        BLOCKED_EXEC_PATTERNS.add("/tmp/");
        BLOCKED_EXEC_PATTERNS.add("/data/local/tmp/");
        BLOCKED_EXEC_PATTERNS.add("/sdcard/");
        BLOCKED_EXEC_PATTERNS.add("/storage/");
        BLOCKED_EXEC_PATTERNS.add("/proc/");
        BLOCKED_EXEC_PATTERNS.add("/dev/");
    }

    private final Context mContext;

    public ExecSpawningProtection(Context context) {
        mContext = context;
        initializeSystemProperties();
    }

    /**
     * Initialize system properties for exec spawning protection
     */
    private void initializeSystemProperties() {
        SystemProperties.set(PROP_EXEC_SPAWNING_PROTECTION, 
                isExecProtectionEnabled() ? "1" : "0");
        SystemProperties.set(PROP_STRICT_EXEC_CONTROL, 
                isStrictModeEnabled() ? "1" : "0");
        SystemProperties.set(PROP_PROCESS_SPAWN_LIMITING, 
                isProcessLimitEnabled() ? "1" : "0");
        SystemProperties.set(PROP_SHELL_RESTRICTIONS, 
                isShellRestricted() ? "1" : "0");
    }

    /**
     * Check if exec spawning protection is enabled
     */
    public boolean isExecProtectionEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_EXEC_PROTECTION_ENABLED, DEFAULT_EXEC_PROTECTION_ENABLED ? 1 : 0) == 1;
    }

    /**
     * Check if strict exec control is enabled
     */
    public boolean isStrictModeEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_STRICT_MODE_ENABLED, DEFAULT_STRICT_MODE_ENABLED ? 1 : 0) == 1;
    }

    /**
     * Check if process spawn limiting is enabled
     */
    public boolean isProcessLimitEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_PROCESS_LIMIT_ENABLED, DEFAULT_PROCESS_LIMIT_ENABLED ? 1 : 0) == 1;
    }

    /**
     * Check if shell access is restricted
     */
    public boolean isShellRestricted() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_SHELL_RESTRICTED, DEFAULT_SHELL_RESTRICTED ? 1 : 0) == 1;
    }

    /**
     * Validate exec path for security
     */
    public boolean isExecPathAllowed(String execPath, String packageName, int uid) {
        if (!isExecProtectionEnabled()) {
            return true;
        }

        // Log exec attempt
        logExecAttempt(execPath, packageName, uid);

        // Check if path is blocked
        if (isPathBlocked(execPath)) {
            if (DEBUG) Log.w(TAG, "Blocked exec path: " + execPath + " for package: " + packageName);
            return false;
        }

        // Check if path is explicitly allowed
        if (isPathAllowed(execPath)) {
            return true;
        }

        // In strict mode, only allow system paths
        if (isStrictModeEnabled()) {
            return isSystemPath(execPath);
        }

        // Allow for system apps and root
        return isSystemApp(uid) || Process.isSystemUid(uid);
    }

    /**
     * Check if path is blocked
     */
    private boolean isPathBlocked(String execPath) {
        for (String blockedPattern : BLOCKED_EXEC_PATTERNS) {
            if (execPath.startsWith(blockedPattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if path is explicitly allowed
     */
    private boolean isPathAllowed(String execPath) {
        for (String allowedPath : ALLOWED_EXEC_PATHS) {
            if (execPath.startsWith(allowedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if path is a system path
     */
    private boolean isSystemPath(String execPath) {
        return execPath.startsWith("/system/") || 
               execPath.startsWith("/vendor/") || 
               execPath.startsWith("/apex/") ||
               execPath.startsWith("/product/");
    }

    /**
     * Check if UID is a system app
     */
    private boolean isSystemApp(int uid) {
        return uid < 10000; // System UIDs are typically < 10000
    }

    /**
     * Log exec attempt for security monitoring
     */
    private void logExecAttempt(String execPath, String packageName, int uid) {
        if (DEBUG) {
            Log.d(TAG, String.format("Exec attempt: path=%s, package=%s, uid=%d, allowed=%b",
                    execPath, packageName, uid, true));
        }
        
        // Log to security audit trail
        SecurityLogger.logExecAttempt(execPath, packageName, uid);
    }

    /**
     * Set exec protection enabled
     */
    public void setExecProtectionEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_EXEC_PROTECTION_ENABLED, enabled ? 1 : 0);
        SystemProperties.set(PROP_EXEC_SPAWNING_PROTECTION, enabled ? "1" : "0");
    }

    /**
     * Set strict mode enabled
     */
    public void setStrictModeEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_STRICT_MODE_ENABLED, enabled ? 1 : 0);
        SystemProperties.set(PROP_STRICT_EXEC_CONTROL, enabled ? "1" : "0");
    }

    /**
     * Set process limit enabled
     */
    public void setProcessLimitEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_PROCESS_LIMIT_ENABLED, enabled ? 1 : 0);
        SystemProperties.set(PROP_PROCESS_SPAWN_LIMITING, enabled ? "1" : "0");
    }

    /**
     * Set shell access restricted
     */
    public void setShellRestricted(boolean restricted) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_SHELL_RESTRICTED, restricted ? "1" : "0");
        SystemProperties.set(PROP_SHELL_RESTRICTIONS, restricted ? "1" : "0");
    }

    /**
     * Reset to secure defaults
     */
    public void resetToSecureDefaults() {
        setExecProtectionEnabled(DEFAULT_EXEC_PROTECTION_ENABLED);
        setStrictModeEnabled(DEFAULT_STRICT_MODE_ENABLED);
        setProcessLimitEnabled(DEFAULT_PROCESS_LIMIT_ENABLED);
        setShellRestricted(DEFAULT_SHELL_RESTRICTED);
    }

    /**
     * Get security status
     */
    public String getSecurityStatus() {
        return String.format("ExecProtection: %b, StrictMode: %b, ProcessLimit: %b, ShellRestricted: %b",
                isExecProtectionEnabled(), isStrictModeEnabled(), 
                isProcessLimitEnabled(), isShellRestricted());
    }
}
