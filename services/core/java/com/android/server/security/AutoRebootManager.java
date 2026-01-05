package com.android.server.security;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Auto-reboot manager for enhanced security.
 * Implements GrapheneOS-style auto-reboot functionality.
 */
public class AutoRebootManager {
    private static final String TAG = "AutoRebootManager";
    private static final boolean DEBUG = false;
    
    private final Context mContext;
    private static AutoRebootManager sInstance;
    private Timer mRebootTimer;
    private SettingsObserver mSettingsObserver;
    
    // Auto-reboot settings
    private static final String AUTO_REBOOT_ENABLED = "auto_reboot_enabled";
    private static final String AUTO_REBOOT_DELAY = "auto_reboot_delay";
    
    // Default values
    private static final long DEFAULT_REBOOT_DELAY = 24 * 60 * 60 * 1000; // 24 hours
    private static final long MIN_REBOOT_DELAY = 60 * 60 * 1000; // 1 hour
    
    private AutoRebootManager(Context context) {
        mContext = context;
        mSettingsObserver = new SettingsObserver(new Handler(context.getMainLooper()));
    }
    
    public static synchronized AutoRebootManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AutoRebootManager(context);
        }
        return sInstance;
    }
    
    /**
     * Initialize auto-reboot functionality.
     */
    public void initialize() {
        // Register settings observer to watch for changes
        mSettingsObserver.register();
        
        if (isAutoRebootEnabled()) {
            scheduleAutoReboot();
        }
    }
    
    /**
     * Check if auto-reboot is enabled.
     */
    public boolean isAutoRebootEnabled() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.AUTO_REBOOT_ENABLED, 0) != 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the configured auto-reboot delay.
     */
    public long getAutoRebootDelay() {
        try {
            long delay = Settings.Secure.getLong(mContext.getContentResolver(),
                    Settings.Secure.AUTO_REBOOT_DELAY, DEFAULT_REBOOT_DELAY);
            
            // Ensure minimum delay
            return Math.max(delay, MIN_REBOOT_DELAY);
        } catch (Exception e) {
            return DEFAULT_REBOOT_DELAY;
        }
    }
    
    /**
     * Set auto-reboot delay.
     */
    public void setAutoRebootDelay(long delayMs) {
        try {
            // Ensure minimum delay
            delayMs = Math.max(delayMs, MIN_REBOOT_DELAY);
            
            Settings.Secure.putLong(mContext.getContentResolver(),
                    Settings.Secure.AUTO_REBOOT_DELAY, delayMs);
            
            if (DEBUG) {
                Log.d(TAG, "Auto-reboot delay set to: " + delayMs + "ms");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set auto-reboot delay", e);
        }
    }
    
    /**
     * Enable auto-reboot functionality.
     */
    public void enableAutoReboot(long delayMs) {
        try {
            setAutoRebootDelay(delayMs);
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.AUTO_REBOOT_ENABLED, 1);
            
            scheduleAutoReboot();
            
            if (DEBUG) {
                Log.d(TAG, "Auto-reboot enabled with delay: " + delayMs + "ms");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable auto-reboot", e);
        }
    }
    
    /**
     * Disable auto-reboot functionality.
     */
    public void disableAutoReboot() {
        try {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.AUTO_REBOOT_ENABLED, 0);
            
            cancelScheduledReboot();
            
            if (DEBUG) {
                Log.d(TAG, "Auto-reboot disabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable auto-reboot", e);
        }
    }
    
    /**
     * Schedule auto-reboot based on configured delay.
     */
    private void scheduleAutoReboot() {
        try {
            // Cancel any existing timer
            cancelScheduledReboot();
            
            long delay = getAutoRebootDelay();
            
            mRebootTimer = new Timer("AutoRebootTimer", true);
            mRebootTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    performAutoReboot();
                }
            }, delay);
            
            if (DEBUG) {
                Log.d(TAG, "Auto-reboot scheduled in: " + delay + "ms");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule auto-reboot", e);
        }
    }
    
    /**
     * Cancel scheduled auto-reboot.
     */
    private void cancelScheduledReboot() {
        try {
            if (mRebootTimer != null) {
                mRebootTimer.cancel();
                mRebootTimer = null;
                
                if (DEBUG) {
                    Log.d(TAG, "Scheduled auto-reboot cancelled");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel scheduled reboot", e);
        }
    }
    
    /**
     * Perform automatic reboot.
     */
    private void performAutoReboot() {
        try {
            Log.i(TAG, "Auto-reboot triggered - device will reboot for security");
            
            // Set reboot reason
            SystemProperties.set("sys.powerctl", "reboot,auto_reboot");
            
            // Execute reboot
            Runtime.getRuntime().exec("reboot");
            
        } catch (Exception e) {
            Log.e(TAG, "Auto-reboot failed", e);
        }
    }
    
    /**
     * Reset auto-reboot timer (call when device is unlocked).
     */
    public void resetAutoRebootTimer() {
        try {
            if (isAutoRebootEnabled()) {
                scheduleAutoReboot();
                
                if (DEBUG) {
                    Log.d(TAG, "Auto-reboot timer reset");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset auto-reboot timer", e);
        }
    }
    
    /**
     * Get auto-reboot status.
     */
    public String getAutoRebootStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Auto-Reboot Status:\n");
        status.append("Enabled: ").append(isAutoRebootEnabled() ? "YES" : "NO").append("\n");
        
        if (isAutoRebootEnabled()) {
            long delay = getAutoRebootDelay();
            status.append("Delay: ").append(delay / (60 * 60 * 1000)).append(" hours\n");
            status.append("Timer Active: ").append(mRebootTimer != null ? "YES" : "NO").append("\n");
        }
        
        return status.toString();
    }
    
    /**
     * Settings observer to watch for auto-reboot setting changes.
     */
    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }
        
        void register() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(AUTO_REBOOT_ENABLED),
                    false, this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(AUTO_REBOOT_DELAY),
                    false, this);
        }
        
        void unregister() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }
        
        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) {
                Log.d(TAG, "Auto-reboot settings changed");
            }
            
            // Reschedule reboot if enabled, or cancel if disabled
            if (isAutoRebootEnabled()) {
                scheduleAutoReboot();
            } else {
                cancelScheduledReboot();
            }
        }
    }
}
