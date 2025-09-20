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
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Auto-Reboot Manager - Based on GrapheneOS implementation
 * Periodically reboots device to mitigate firmware exploits
 * Reduces window of opportunity for attackers
 */
public class AutoRebootManager {
    private static final String TAG = "AutoRebootManager";
    private static final boolean DEBUG = false;

    // Settings keys
    private static final String SETTING_AUTO_REBOOT_ENABLED = "auto_reboot_enabled";
    private static final String SETTING_AUTO_REBOOT_INTERVAL = "auto_reboot_interval_hours";
    private static final String SETTING_LAST_REBOOT_TIME = "last_reboot_time";
    private static final String SETTING_AUTO_REBOOT_NOTIFICATION = "auto_reboot_notification_enabled";

    // Default settings (GrapheneOS style)
    private static final boolean DEFAULT_AUTO_REBOOT_ENABLED = false; // User choice
    private static final int DEFAULT_AUTO_REBOOT_INTERVAL = 72; // 72 hours default
    private static final boolean DEFAULT_AUTO_REBOOT_NOTIFICATION = true;

    // Reboot intervals (in hours)
    public static final int REBOOT_INTERVAL_24_HOURS = 24;
    public static final int REBOOT_INTERVAL_48_HOURS = 48;
    public static final int REBOOT_INTERVAL_72_HOURS = 72;
    public static final int REBOOT_INTERVAL_168_HOURS = 168; // 1 week

    private final Context mContext;
    private final PowerManager mPowerManager;
    private final Handler mHandler;
    private final SimpleDateFormat mDateFormat;

    public AutoRebootManager(Context context) {
        mContext = context;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        
        if (DEBUG) Log.d(TAG, "AutoRebootManager initialized");
    }

    /**
     * Check if auto-reboot is enabled
     */
    public boolean isAutoRebootEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_AUTO_REBOOT_ENABLED, DEFAULT_AUTO_REBOOT_ENABLED ? 1 : 0) == 1;
    }

    /**
     * Get auto-reboot interval in hours
     */
    public int getAutoRebootInterval() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_AUTO_REBOOT_INTERVAL, DEFAULT_AUTO_REBOOT_INTERVAL);
    }

    /**
     * Get last reboot time
     */
    public long getLastRebootTime() {
        return Settings.System.getLong(mContext.getContentResolver(),
                SETTING_LAST_REBOOT_TIME, System.currentTimeMillis());
    }

    /**
     * Check if reboot notification is enabled
     */
    public boolean isRebootNotificationEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_AUTO_REBOOT_NOTIFICATION, DEFAULT_AUTO_REBOOT_NOTIFICATION ? 1 : 0) == 1;
    }

    /**
     * Enable auto-reboot
     */
    public void setAutoRebootEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_AUTO_REBOOT_ENABLED, enabled ? 1 : 0);
        
        if (enabled) {
            scheduleNextReboot();
        } else {
            cancelScheduledReboot();
        }
        
        if (DEBUG) Log.d(TAG, "Auto-reboot " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Set auto-reboot interval
     */
    public void setAutoRebootInterval(int hours) {
        if (hours < 1 || hours > 168) { // Max 1 week
            throw new IllegalArgumentException("Reboot interval must be between 1 and 168 hours");
        }
        
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_AUTO_REBOOT_INTERVAL, hours);
        
        if (isAutoRebootEnabled()) {
            scheduleNextReboot();
        }
        
        if (DEBUG) Log.d(TAG, "Auto-reboot interval set to " + hours + " hours");
    }

    /**
     * Set reboot notification enabled
     */
    public void setRebootNotificationEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_AUTO_REBOOT_NOTIFICATION, enabled ? 1 : 0);
    }

    /**
     * Update last reboot time
     */
    public void updateLastRebootTime() {
        long currentTime = System.currentTimeMillis();
        Settings.System.putLong(mContext.getContentResolver(),
                SETTING_LAST_REBOOT_TIME, currentTime);
        
        if (DEBUG) Log.d(TAG, "Last reboot time updated: " + mDateFormat.format(new Date(currentTime)));
    }

    /**
     * Schedule next reboot
     */
    private void scheduleNextReboot() {
        if (!isAutoRebootEnabled()) {
            return;
        }
        
        long lastRebootTime = getLastRebootTime();
        long intervalMillis = getAutoRebootInterval() * 60 * 60 * 1000L; // Convert hours to milliseconds
        long nextRebootTime = lastRebootTime + intervalMillis;
        long currentTime = System.currentTimeMillis();
        
        if (nextRebootTime <= currentTime) {
            // Time for reboot
            performReboot();
        } else {
            // Schedule future reboot
            long delay = nextRebootTime - currentTime;
            mHandler.postDelayed(this::performReboot, delay);
            
            if (DEBUG) {
                Log.d(TAG, "Next reboot scheduled for: " + mDateFormat.format(new Date(nextRebootTime)));
            }
        }
    }

    /**
     * Cancel scheduled reboot
     */
    private void cancelScheduledReboot() {
        mHandler.removeCallbacks(this::performReboot);
        if (DEBUG) Log.d(TAG, "Scheduled reboot cancelled");
    }

    /**
     * Perform reboot
     */
    private void performReboot() {
        if (DEBUG) Log.d(TAG, "Performing scheduled reboot for security");
        
        // Log reboot event
        SecurityLogger.logSecurityViolation("Scheduled security reboot", "system", Process.myUid());
        
        // Update last reboot time before rebooting
        updateLastRebootTime();
        
        // Perform reboot
        try {
            mPowerManager.reboot(null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform reboot", e);
        }
    }

    /**
     * Get time until next reboot
     */
    public long getTimeUntilNextReboot() {
        if (!isAutoRebootEnabled()) {
            return -1; // Disabled
        }
        
        long lastRebootTime = getLastRebootTime();
        long intervalMillis = getAutoRebootInterval() * 60 * 60 * 1000L;
        long nextRebootTime = lastRebootTime + intervalMillis;
        long currentTime = System.currentTimeMillis();
        
        return Math.max(0, nextRebootTime - currentTime);
    }

    /**
     * Get auto-reboot status
     */
    public String getAutoRebootStatus() {
        if (!isAutoRebootEnabled()) {
            return "Auto-reboot disabled";
        }
        
        long timeUntilReboot = getTimeUntilNextReboot();
        if (timeUntilReboot <= 0) {
            return "Reboot pending";
        }
        
        long hours = timeUntilReboot / (60 * 60 * 1000L);
        long minutes = (timeUntilReboot % (60 * 60 * 1000L)) / (60 * 1000L);
        
        return String.format("Next reboot in %d hours %d minutes", hours, minutes);
    }

    /**
     * Reset to default settings
     */
    public void resetToDefaults() {
        setAutoRebootEnabled(DEFAULT_AUTO_REBOOT_ENABLED);
        setAutoRebootInterval(DEFAULT_AUTO_REBOOT_INTERVAL);
        setRebootNotificationEnabled(DEFAULT_AUTO_REBOOT_NOTIFICATION);
        updateLastRebootTime();
    }

    /**
     * Get security benefits
     */
    public String getSecurityBenefits() {
        return "Mitigates firmware exploits by reducing attack window through periodic reboots";
    }
}
