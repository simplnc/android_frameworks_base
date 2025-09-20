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

/**
 * Secure App Spawning - Based on GrapheneOS implementation
 * Replaces traditional Zygote spawning with exec-based spawning
 * Each app process gets a unique memory layout for enhanced ASLR
 */
public class SecureAppSpawning {
    private static final String TAG = "SecureAppSpawning";
    private static final boolean DEBUG = false;

    // System properties (matching GrapheneOS)
    private static final String PROP_SECURE_APP_SPAWNING = "persist.security.secure_app_spawning";
    private static final String PROP_ZYGOTE_FORK_MODE = "persist.security.zygote_fork_mode";
    private static final String PROP_EXEC_BASED_SPAWNING = "persist.security.exec_based_spawning";

    // Settings keys (matching GrapheneOS Settings > Security)
    private static final String SETTING_SECURE_APP_SPAWNING = "secure_app_spawning_enabled";
    private static final String SETTING_ZYGOTE_FORK_MODE = "zygote_fork_mode_enabled";
    private static final String SETTING_EXEC_BASED_SPAWNING = "exec_based_spawning_enabled";

    // Default settings (secure by default, like GrapheneOS)
    private static final boolean DEFAULT_SECURE_APP_SPAWNING = true;
    private static final boolean DEFAULT_ZYGOTE_FORK_MODE = false; // Disable traditional Zygote
    private static final boolean DEFAULT_EXEC_BASED_SPAWNING = true;

    private final Context mContext;

    public SecureAppSpawning(Context context) {
        mContext = context;
        initializeSystemProperties();
    }

    /**
     * Initialize system properties for secure app spawning
     */
    private void initializeSystemProperties() {
        SystemProperties.set(PROP_SECURE_APP_SPAWNING, 
                isSecureAppSpawningEnabled() ? "1" : "0");
        SystemProperties.set(PROP_ZYGOTE_FORK_MODE, 
                isZygoteForkModeEnabled() ? "1" : "0");
        SystemProperties.set(PROP_EXEC_BASED_SPAWNING, 
                isExecBasedSpawningEnabled() ? "1" : "0");
    }

    /**
     * Check if secure app spawning is enabled
     * This is the main GrapheneOS feature: Settings > Security > Enable secure app spawning
     */
    public boolean isSecureAppSpawningEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_SECURE_APP_SPAWNING, DEFAULT_SECURE_APP_SPAWNING ? 1 : 0) == 1;
    }

    /**
     * Check if traditional Zygote fork mode is enabled
     * This should be disabled when secure app spawning is enabled
     */
    public boolean isZygoteForkModeEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_ZYGOTE_FORK_MODE, DEFAULT_ZYGOTE_FORK_MODE ? 1 : 0) == 1;
    }

    /**
     * Check if exec-based spawning is enabled
     * This is the core mechanism that replaces Zygote spawning
     */
    public boolean isExecBasedSpawningEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_EXEC_BASED_SPAWNING, DEFAULT_EXEC_BASED_SPAWNING ? 1 : 0) == 1;
    }

    /**
     * Enable secure app spawning (GrapheneOS main feature)
     */
    public void setSecureAppSpawningEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_SECURE_APP_SPAWNING, enabled ? 1 : 0);
        SystemProperties.set(PROP_SECURE_APP_SPAWNING, enabled ? "1" : "0");
        
        // When secure app spawning is enabled, disable traditional Zygote fork mode
        if (enabled) {
            setZygoteForkModeEnabled(false);
            setExecBasedSpawningEnabled(true);
        } else {
            // When disabled, revert to traditional Zygote mode
            setZygoteForkModeEnabled(true);
            setExecBasedSpawningEnabled(false);
        }
        
        if (DEBUG) Log.d(TAG, "Secure app spawning " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Set Zygote fork mode (traditional spawning)
     */
    public void setZygoteForkModeEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_ZYGOTE_FORK_MODE, enabled ? 1 : 0);
        SystemProperties.set(PROP_ZYGOTE_FORK_MODE, enabled ? "1" : "0");
    }

    /**
     * Set exec-based spawning mode
     */
    public void setExecBasedSpawningEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_EXEC_BASED_SPAWNING, enabled ? 1 : 0);
        SystemProperties.set(PROP_EXEC_BASED_SPAWNING, enabled ? "1" : "0");
    }

    /**
     * Reset to secure defaults (GrapheneOS defaults)
     */
    public void resetToSecureDefaults() {
        setSecureAppSpawningEnabled(DEFAULT_SECURE_APP_SPAWNING);
        setZygoteForkModeEnabled(DEFAULT_ZYGOTE_FORK_MODE);
        setExecBasedSpawningEnabled(DEFAULT_EXEC_BASED_SPAWNING);
        
        if (DEBUG) Log.d(TAG, "Reset to GrapheneOS secure defaults");
    }

    /**
     * Get spawning mode status
     */
    public String getSpawningModeStatus() {
        if (isSecureAppSpawningEnabled()) {
            return "Secure App Spawning (Exec-based) - Enhanced ASLR";
        } else if (isZygoteForkModeEnabled()) {
            return "Traditional Zygote Fork Mode - Shared Memory";
        } else {
            return "Custom Spawning Mode";
        }
    }

    /**
     * Check if current spawning mode is secure
     */
    public boolean isSecureSpawningMode() {
        return isSecureAppSpawningEnabled() && isExecBasedSpawningEnabled() && !isZygoteForkModeEnabled();
    }

    /**
     * Get security benefits of current spawning mode
     */
    public String getSecurityBenefits() {
        if (isSecureSpawningMode()) {
            return "Enhanced ASLR, Unique Memory Layouts, No Shared Memory Secrets";
        } else {
            return "Traditional spawning - Consider enabling secure app spawning for better security";
        }
    }

    /**
     * Log spawning event for security monitoring
     */
    public void logSpawningEvent(String packageName, String spawningMode, int uid) {
        if (DEBUG) {
            Log.d(TAG, String.format("App spawning: %s, mode: %s, uid: %d, secure: %b",
                    packageName, spawningMode, uid, isSecureSpawningMode()));
        }
        
        // Log to security audit trail
        SecurityLogger.logSecurityViolation("App spawning: " + spawningMode, packageName, uid);
    }
}
