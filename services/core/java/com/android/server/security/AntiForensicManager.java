package com.android.server.security;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * Anti-forensic security measures to protect against law enforcement intrusion.
 * Implements log sanitization, secure deletion, and forensic countermeasures.
 */
public class AntiForensicManager {
    private static final String TAG = "AntiForensicManager";
    private static final boolean DEBUG = false;
    
    private final Context mContext;
    private final SecureRandom mSecureRandom;
    
    public AntiForensicManager(Context context) {
        mContext = context;
        mSecureRandom = new SecureRandom();
    }
    
    /**
     * Check if anti-forensic mode is enabled.
     * 
     * @return true if anti-forensic mode is enabled
     */
    public boolean isAntiForensicModeEnabled() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ANTI_FORENSIC_MODE, 1) != 0;
        } catch (Exception e) {
            return true; // Default to enabled for security
        }
    }
    
    /**
     * Check if secure deletion is enabled.
     * 
     * @return true if secure deletion is enabled
     */
    public boolean isSecureDeleteEnabled() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.SECURE_DELETE_ENABLED, 1) != 0;
        } catch (Exception e) {
            return true; // Default to enabled for security
        }
    }
    
    /**
     * Sanitize system logs to remove sensitive information.
     * This helps prevent forensic analysis of device activity.
     */
    public void sanitizeLogs() {
        if (!isAntiForensicModeEnabled()) {
            return;
        }
        
        try {
            // Clear system logs
            Runtime.getRuntime().exec("logcat -c");
            
            // Clear kernel logs
            Runtime.getRuntime().exec("dmesg -c");
            
            // Clear audit logs
            Runtime.getRuntime().exec("auditctl -D");
            
            if (DEBUG) {
                Log.d(TAG, "System logs sanitized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to sanitize logs", e);
        }
    }
    
    /**
     * Securely delete a file by overwriting it multiple times with random data.
     * This prevents forensic recovery of deleted files.
     * 
     * @param filePath Path to the file to securely delete
     * @return true if successful, false otherwise
     */
    public boolean secureDelete(String filePath) {
        if (!isSecureDeleteEnabled()) {
            return new File(filePath).delete();
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            return true;
        }
        
        try {
            long fileSize = file.length();
            byte[] randomData = new byte[8192]; // 8KB buffer
            
            // Overwrite file multiple times with random data
            FileOutputStream fos = new FileOutputStream(file);
            for (int pass = 0; pass < 3; pass++) {
                fos.getChannel().position(0);
                long remaining = fileSize;
                
                while (remaining > 0) {
                    int toWrite = (int) Math.min(remaining, randomData.length);
                    mSecureRandom.nextBytes(randomData);
                    fos.write(randomData, 0, toWrite);
                    remaining -= toWrite;
                }
                fos.getFD().sync(); // Force write to disk
            }
            fos.close();
            
            // Delete the file
            boolean deleted = file.delete();
            
            if (DEBUG) {
                Log.d(TAG, "File securely deleted: " + filePath);
            }
            
            return deleted;
        } catch (IOException e) {
            Log.e(TAG, "Failed to securely delete file: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Implement anti-tamper measures to detect forensic tools.
     * This helps detect if the device is being analyzed.
     */
    public void implementAntiTamperMeasures() {
        if (!isAntiForensicModeEnabled()) {
            return;
        }
        
        try {
            // Set system properties to indicate anti-tamper mode
            SystemProperties.set("ro.anti_tamper.enabled", "1");
            SystemProperties.set("ro.forensic_resistance", "1");
            
            // Clear sensitive system properties
            SystemProperties.set("ro.serialno", "");
            SystemProperties.set("ro.boot.serialno", "");
            
            if (DEBUG) {
                Log.d(TAG, "Anti-tamper measures implemented");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to implement anti-tamper measures", e);
        }
    }
    
    /**
     * Clear forensic artifacts that could be used for device analysis.
     * This includes clearing temporary files, caches, and other traces.
     */
    public void clearForensicArtifacts() {
        if (!isAntiForensicModeEnabled()) {
            return;
        }
        
        try {
            // Clear temporary directories
            clearDirectory("/data/local/tmp");
            clearDirectory("/cache");
            clearDirectory("/data/system/dropbox");
            
            // Clear system caches
            Runtime.getRuntime().exec("rm -rf /data/system/cached_*");
            Runtime.getRuntime().exec("rm -rf /data/dalvik-cache/*");
            
            if (DEBUG) {
                Log.d(TAG, "Forensic artifacts cleared");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear forensic artifacts", e);
        }
    }
    
    /**
     * Clear contents of a directory securely.
     * 
     * @param dirPath Path to the directory to clear
     */
    private void clearDirectory(String dirPath) {
        try {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            secureDelete(file.getAbsolutePath());
                        } else if (file.isDirectory()) {
                            clearDirectory(file.getAbsolutePath());
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear directory: " + dirPath, e);
        }
    }
    
    /**
     * Implement emergency wipe procedures.
     * This is triggered when tampering is detected.
     */
    public void emergencyWipe() {
        try {
            Log.w(TAG, "Emergency wipe initiated");
            
            // Clear all user data
            Runtime.getRuntime().exec("rm -rf /data/data/*");
            Runtime.getRuntime().exec("rm -rf /data/user/*");
            
            // Clear system logs
            sanitizeLogs();
            
            // Clear forensic artifacts
            clearForensicArtifacts();
            
            // Reboot to complete wipe
            Runtime.getRuntime().exec("reboot");
            
        } catch (Exception e) {
            Log.e(TAG, "Emergency wipe failed", e);
        }
    }
}
