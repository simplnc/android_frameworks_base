package com.android.server.security;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Duress PIN manager for emergency device wipe capability.
 * Implements GrapheneOS-style duress PIN functionality.
 */
public class DuressPINManager {
    private static final String TAG = "DuressPINManager";
    private static final boolean DEBUG = false;
    
    private final Context mContext;
    private static DuressPINManager sInstance;
    
    // Duress PIN settings
    private static final String DURESS_PIN_ENABLED = "duress_pin_enabled";
    private static final String DURESS_PIN_CODE = "duress_pin_code";
    private static final String DURESS_WIPE_DELAY = "duress_wipe_delay";
    
    // Default values
    private static final int DEFAULT_WIPE_DELAY = 5000; // 5 seconds
    
    private DuressPINManager(Context context) {
        mContext = context;
        initializeDefaultDuressPIN();
    }
    
    /**
     * Initialize default duress PIN from boot if not already configured.
     */
    private void initializeDefaultDuressPIN() {
        try {
            // Check if duress PIN is already configured
            if (!isDuressPINEnabled()) {
                // Set default duress PIN if not configured
                String defaultDuressPIN = getDefaultDuressPIN();
                if (defaultDuressPIN != null && !defaultDuressPIN.isEmpty()) {
                    setDuressPINCode(defaultDuressPIN);
                    
                    // Log that default duress PIN was set (for admin reference)
                    Log.i(TAG, "Default duress PIN initialized: " + maskPIN(defaultDuressPIN));
                    
                    if (DEBUG) {
                        Log.d(TAG, "Default duress PIN set from boot");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize default duress PIN", e);
        }
    }
    
    /**
     * Get the default duress PIN from system properties or build configuration.
     */
    private String getDefaultDuressPIN() {
        try {
            // First try to get from system property (settable via build)
            String defaultPIN = SystemProperties.get("ro.security.default_duress_pin");
            if (defaultPIN != null && !defaultPIN.isEmpty()) {
                return defaultPIN;
            }
            
            // Fallback to hardcoded default (can be customized in build)
            return "9119"; // Default emergency duress PIN
        } catch (Exception e) {
            Log.e(TAG, "Failed to get default duress PIN", e);
            return null;
        }
    }
    
    /**
     * Mask PIN for logging (show only first and last digit).
     */
    private String maskPIN(String pin) {
        if (pin == null || pin.length() < 2) {
            return "****";
        }
        if (pin.length() == 4) {
            return pin.charAt(0) + "**" + pin.charAt(3);
        }
        return pin.charAt(0) + "***" + pin.charAt(pin.length() - 1);
    }
    
    public static synchronized DuressPINManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DuressPINManager(context);
        }
        return sInstance;
    }
    
    /**
     * Check if duress PIN is enabled.
     */
    public boolean isDuressPINEnabled() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.DURESS_PIN_ENABLED, 0) != 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the configured duress PIN code.
     */
    public String getDuressPINCode() {
        try {
            return Settings.Secure.getString(mContext.getContentResolver(),
                    Settings.Secure.DURESS_PIN_CODE);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Set the duress PIN code.
     */
    public void setDuressPINCode(String pinCode) {
        try {
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.DURESS_PIN_CODE, pinCode);
            
            // Enable duress PIN when code is set
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.DURESS_PIN_ENABLED, 1);
            
            if (DEBUG) {
                Log.d(TAG, "Duress PIN code set");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set duress PIN code", e);
        }
    }
    
    /**
     * Verify if entered PIN is the duress PIN.
     */
    public boolean isDuressPIN(String enteredPIN) {
        if (!isDuressPINEnabled()) {
            return false;
        }
        
        String duressPIN = getDuressPINCode();
        if (duressPIN == null || duressPIN.isEmpty()) {
            return false;
        }
        
        return duressPIN.equals(enteredPIN);
    }
    
    /**
     * Trigger emergency wipe when duress PIN is entered.
     */
    public void triggerEmergencyWipe() {
        try {
            Log.w(TAG, "DURESS PIN ENTERED - EMERGENCY WIPE INITIATED");
            
            // Set system property to indicate emergency wipe
            SystemProperties.set("ro.emergency_wipe_triggered", "1");
            
            // Clear all user data securely
            clearUserDataSecurely();
            
            // Clear system logs
            clearSystemLogs();
            
            // Clear forensic artifacts
            clearForensicArtifacts();
            
            // Schedule device reboot
            scheduleEmergencyReboot();
            
            if (DEBUG) {
                Log.d(TAG, "Emergency wipe sequence completed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Emergency wipe failed", e);
        }
    }
    
    /**
     * Clear all user data securely using multiple overwrite passes.
     */
    private void clearUserDataSecurely() {
        try {
            // Clear user data directories
            String[] userDataPaths = {
                "/data/data",
                "/data/user",
                "/data/app",
                "/data/system",
                "/data/misc"
            };
            
            for (String path : userDataPaths) {
                secureWipeDirectory(path);
            }
            
            if (DEBUG) {
                Log.d(TAG, "User data securely cleared");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear user data securely", e);
        }
    }
    
    /**
     * Securely wipe a directory with multiple overwrite passes.
     */
    private void secureWipeDirectory(String dirPath) {
        try {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            secureWipeFile(file.getAbsolutePath());
                        } else if (file.isDirectory()) {
                            secureWipeDirectory(file.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to securely wipe directory: " + dirPath, e);
        }
    }
    
    /**
     * Securely wipe a file with multiple overwrite passes.
     */
    private void secureWipeFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                // 3-pass secure wipe
                for (int pass = 0; pass < 3; pass++) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write("0".repeat((int) file.length()));
                        writer.flush();
                    }
                }
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to securely wipe file: " + filePath, e);
        }
    }
    
    /**
     * Clear system logs to remove evidence.
     */
    private void clearSystemLogs() {
        try {
            Runtime.getRuntime().exec("logcat -c");
            Runtime.getRuntime().exec("dmesg -c");
            Runtime.getRuntime().exec("auditctl -D");
            
            if (DEBUG) {
                Log.d(TAG, "System logs cleared");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear system logs", e);
        }
    }
    
    /**
     * Clear forensic artifacts.
     */
    private void clearForensicArtifacts() {
        try {
            // Clear temporary directories
            String[] tempPaths = {
                "/data/local/tmp",
                "/cache",
                "/data/system/dropbox"
            };
            
            for (String path : tempPaths) {
                secureWipeDirectory(path);
            }
            
            if (DEBUG) {
                Log.d(TAG, "Forensic artifacts cleared");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear forensic artifacts", e);
        }
    }
    
    /**
     * Schedule emergency reboot after wipe.
     */
    private void scheduleEmergencyReboot() {
        try {
            // Set reboot reason
            SystemProperties.set("sys.powerctl", "reboot,emergency_wipe");
            
            // Execute reboot after delay
            new Thread(() -> {
                try {
                    Thread.sleep(DEFAULT_WIPE_DELAY);
                    Runtime.getRuntime().exec("reboot");
                } catch (Exception e) {
                    Log.e(TAG, "Emergency reboot failed", e);
                }
            }).start();
            
            if (DEBUG) {
                Log.d(TAG, "Emergency reboot scheduled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule emergency reboot", e);
        }
    }
    
    /**
     * Get duress PIN status.
     */
    public String getDuressPINStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Duress PIN Status:\n");
        status.append("Enabled: ").append(isDuressPINEnabled() ? "YES" : "NO").append("\n");
        
        if (isDuressPINEnabled()) {
            String pinCode = getDuressPINCode();
            status.append("PIN Code: ").append(pinCode != null ? maskPIN(pinCode) : "NOT_SET").append("\n");
            status.append("Wipe Delay: ").append(DEFAULT_WIPE_DELAY).append("ms\n");
            status.append("Using Default: ").append(isUsingDefaultDuressPIN() ? "YES" : "NO").append("\n");
        }
        
        status.append("Default PIN: ").append(maskPIN(getDefaultDuressPIN())).append("\n");
        status.append("System Property: ").append(
                SystemProperties.get("ro.security.default_duress_pin", "not_set")).append("\n");
        
        return status.toString();
    }
    
    /**
     * Get the current duress PIN for user reference (masked for security).
     */
    public String getCurrentDuressPINMasked() {
        if (!isDuressPINEnabled()) {
            return "Not enabled";
        }
        
        String pinCode = getDuressPINCode();
        if (pinCode == null || pinCode.isEmpty()) {
            return "Not set";
        }
        
        return maskPIN(pinCode);
    }
    
    /**
     * Check if duress PIN is using default value.
     */
    public boolean isUsingDefaultDuressPIN() {
        try {
            String currentPIN = getDuressPINCode();
            String defaultPIN = getDefaultDuressPIN();
            
            return currentPIN != null && defaultPIN != null && currentPIN.equals(defaultPIN);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the actual duress PIN code (for admin/debugging purposes only).
     * WARNING: This exposes the actual PIN - use with extreme caution!
     */
    public String getDuressPINCodeUnmasked() {
        if (!isDuressPINEnabled()) {
            return null;
        }
        return getDuressPINCode();
    }
}
