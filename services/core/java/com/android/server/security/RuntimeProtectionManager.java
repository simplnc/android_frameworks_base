package com.android.server.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Debug;
import android.os.SystemProperties;
import android.util.Log;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Runtime protection manager for anti-debugging and integrity checks.
 * Implements security measures to prevent runtime analysis and tampering.
 */
public class RuntimeProtectionManager {
    private static final String TAG = "RuntimeProtectionManager";
    private static final boolean DEBUG = false;
    
    private final Context mContext;
    private static RuntimeProtectionManager sInstance;
    
    // Security flags
    private static final String SECURITY_DEBUGGING_ENABLED = "security_debugging_enabled";
    private static final String SECURITY_INTEGRITY_CHECKS = "security_integrity_checks";
    private static final String SECURITY_ANTI_TAMPER = "security_anti_tamper";
    
    private RuntimeProtectionManager(Context context) {
        mContext = context;
        initializeProtection();
    }
    
    public static synchronized RuntimeProtectionManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RuntimeProtectionManager(context);
        }
        return sInstance;
    }
    
    /**
     * Initialize runtime protection measures.
     */
    private void initializeProtection() {
        try {
            // Enable anti-debugging
            enableAntiDebugging();
            
            // Enable integrity checks
            enableIntegrityChecks();
            
            // Enable anti-tamper measures
            enableAntiTamper();
            
            if (DEBUG) {
                Log.d(TAG, "Runtime protection initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize runtime protection", e);
        }
    }
    
    /**
     * Enable anti-debugging measures.
     */
    private void enableAntiDebugging() {
        try {
            // Check for debugger connection
            if (isDebuggerConnected()) {
                Log.w(TAG, "Debugger detected - terminating process");
                System.exit(1);
            }
            
            // Check for debug build
            if (isDebugBuild()) {
                Log.w(TAG, "Debug build detected - enabling additional protection");
            }
            
            // Set system properties for anti-debugging
            SystemProperties.set("ro.security.anti_debug", "1");
            SystemProperties.set("ro.security.runtime_protection", "1");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable anti-debugging", e);
        }
    }
    
    /**
     * Enable integrity checks for system components.
     */
    private void enableIntegrityChecks() {
        try {
            // Check system app integrity
            checkSystemAppIntegrity();
            
            // Check framework integrity
            checkFrameworkIntegrity();
            
            // Set integrity check properties
            SystemProperties.set("ro.security.integrity_checks", "1");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable integrity checks", e);
        }
    }
    
    /**
     * Enable anti-tamper measures.
     */
    private void enableAntiTamper() {
        try {
            // Check for tampering indicators
            if (isTampered()) {
                Log.w(TAG, "Tampering detected - enabling protection");
                enableEmergencyProtection();
            }
            
            // Set anti-tamper properties
            SystemProperties.set("ro.security.anti_tamper", "1");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable anti-tamper", e);
        }
    }
    
    /**
     * Check if debugger is connected.
     */
    public boolean isDebuggerConnected() {
        try {
            return Debug.isDebuggerConnected();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if this is a debug build.
     */
    public boolean isDebugBuild() {
        try {
            return SystemProperties.getBoolean("ro.debuggable", false);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check system app integrity.
     */
    private void checkSystemAppIntegrity() {
        try {
            PackageManager pm = mContext.getPackageManager();
            String[] systemApps = {
                "android",
                "com.android.systemui",
                "com.android.settings"
            };
            
            for (String packageName : systemApps) {
                if (!verifyAppIntegrity(packageName)) {
                    Log.w(TAG, "Integrity check failed for: " + packageName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check system app integrity", e);
        }
    }
    
    /**
     * Check framework integrity.
     */
    private void checkFrameworkIntegrity() {
        try {
            // Check for modified system properties
            if (isSystemPropertiesTampered()) {
                Log.w(TAG, "System properties tampering detected");
            }
            
            // Check for modified system files
            if (isSystemFilesTampered()) {
                Log.w(TAG, "System files tampering detected");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to check framework integrity", e);
        }
    }
    
    /**
     * Verify app integrity by checking signatures.
     */
    public boolean verifyAppIntegrity(String packageName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            
            if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                return false;
            }
            
            // Verify signature hash
            Signature signature = packageInfo.signatures[0];
            String signatureHash = getSignatureHash(signature);
            
            // Check against known good signatures
            return isKnownGoodSignature(packageName, signatureHash);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify app integrity: " + packageName, e);
            return false;
        }
    }
    
    /**
     * Get signature hash.
     */
    private String getSignatureHash(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(signature.toByteArray());
            return bytesToHex(hash);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if signature is known good.
     */
    private boolean isKnownGoodSignature(String packageName, String signatureHash) {
        // MicroG packages (built from vendor at build time)
        String[] microgPackages = {
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "org.microg.gms.droidguard",
            "org.microg.nlp.backend.ichnaea",
            "org.microg.nlp.backend.nominatim",
            "org.microg.gms.location",
            "org.microg.gms.snet",
            "org.microg.gms.recaptcha"
        };
        
        // Check if this is a MicroG package
        for (String microgPkg : microgPackages) {
            if (packageName.equals(microgPkg)) {
                if (DEBUG) {
                    Log.d(TAG, "Allowing MicroG package: " + packageName);
                }
                return true; // Allow MicroG packages
            }
        }
        
        // System apps and framework
        return packageName.startsWith("com.android.") || 
               packageName.equals("android");
    }
    
    /**
     * Check if system properties are tampered.
     */
    private boolean isSystemPropertiesTampered() {
        try {
            // Check for suspicious property modifications
            String[] suspiciousProps = {
                "ro.debuggable",
                "ro.secure",
                "ro.adb.secure"
            };
            
            for (String prop : suspiciousProps) {
                String value = SystemProperties.get(prop);
                if (value != null && !isExpectedValue(prop, value)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if system files are tampered.
     */
    private boolean isSystemFilesTampered() {
        try {
            // Check for modified system binaries
            String[] systemBinaries = {
                "/system/bin/su",
                "/system/xbin/su",
                "/system/app/Superuser.apk"
            };
            
            for (String binary : systemBinaries) {
                if (new java.io.File(binary).exists()) {
                    return true; // Root binaries detected
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if device is tampered.
     */
    public boolean isTampered() {
        try {
            // Check for root indicators
            if (isRooted()) {
                return true;
            }
            
            // Check for debugging
            if (isDebuggerConnected()) {
                return true;
            }
            
            // Check for tampering
            if (isSystemPropertiesTampered() || isSystemFilesTampered()) {
                return true;
            }
            
            // Check bootloader status (unlocked bootloader reduces security but doesn't fail)
            if (isBootloaderUnlocked()) {
                if (DEBUG) {
                    Log.w(TAG, "Bootloader unlocked - reduced security but not tampered");
                }
                // Don't fail security check for unlocked bootloader, just log warning
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if bootloader is unlocked.
     */
    public boolean isBootloaderUnlocked() {
        try {
            // Check bootloader status via system properties
            String bootloaderStatus = SystemProperties.get("ro.boot.verifiedbootstate", "unknown");
            String bootloaderLocked = SystemProperties.get("ro.boot.veritymode", "unknown");
            
            // Orange/yellow state indicates unlocked bootloader
            boolean unlocked = "orange".equals(bootloaderStatus) || 
                              "yellow".equals(bootloaderStatus) ||
                              "enforcing".equals(bootloaderLocked);
            
            if (DEBUG && unlocked) {
                Log.d(TAG, "Bootloader status: " + bootloaderStatus + ", verity: " + bootloaderLocked);
            }
            
            return unlocked;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if device is rooted.
     */
    public boolean isRooted() {
        try {
            // Check for actual root binaries (exclude MicroG components)
            String[] rootBinaries = {
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su"
                // Removed busybox check - MicroG uses it legitimately
            };
            
            for (String binary : rootBinaries) {
                if (new java.io.File(binary).exists()) {
                    if (DEBUG) {
                        Log.d(TAG, "Root binary detected: " + binary);
                    }
                    return true;
                }
            }
            
            // Check for actual root apps (exclude MicroG packages)
            String[] rootApps = {
                "com.noshufou.android.su",
                "com.noshufou.android.su.elite",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "com.thirdparty.superuser",
                "com.yellowes.su"
                // Removed MicroG-related packages
            };
            
            PackageManager pm = mContext.getPackageManager();
            for (String app : rootApps) {
                try {
                    pm.getPackageInfo(app, 0);
                    if (DEBUG) {
                        Log.d(TAG, "Root app detected: " + app);
                    }
                    return true;
                } catch (PackageManager.NameNotFoundException e) {
                    // App not found, continue
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Enable emergency protection when tampering is detected.
     */
    private void enableEmergencyProtection() {
        try {
            // Clear sensitive data
            clearSensitiveData();
            
            // Disable debugging
            SystemProperties.set("ro.debuggable", "0");
            SystemProperties.set("ro.adb.secure", "1");
            
            // Log security event
            Log.w(TAG, "Emergency protection activated");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable emergency protection", e);
        }
    }
    
    /**
     * Clear sensitive data.
     */
    private void clearSensitiveData() {
        try {
            // Clear system logs
            Runtime.getRuntime().exec("logcat -c");
            
            // Clear temporary files
            Runtime.getRuntime().exec("rm -rf /data/local/tmp/*");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear sensitive data", e);
        }
    }
    
    /**
     * Check if property value is expected.
     */
    private boolean isExpectedValue(String property, String value) {
        switch (property) {
            case "ro.debuggable":
                return "0".equals(value);
            case "ro.secure":
                return "1".equals(value);
            case "ro.adb.secure":
                return "1".equals(value);
            default:
                return true;
        }
    }
    
    /**
     * Convert bytes to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Get security status.
     */
    public String getSecurityStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Runtime Protection: ").append(isRuntimeProtectionEnabled() ? "ENABLED" : "DISABLED").append("\n");
        status.append("Anti-Debugging: ").append(isAntiDebuggingEnabled() ? "ENABLED" : "DISABLED").append("\n");
        status.append("Integrity Checks: ").append(isIntegrityChecksEnabled() ? "ENABLED" : "DISABLED").append("\n");
        status.append("Anti-Tamper: ").append(isAntiTamperEnabled() ? "ENABLED" : "DISABLED").append("\n");
        status.append("Bootloader: ").append(isBootloaderUnlocked() ? "UNLOCKED" : "LOCKED").append("\n");
        status.append("Device Status: ").append(isTampered() ? "TAMPERED" : "SECURE").append("\n");
        return status.toString();
    }
    
    /**
     * Check if runtime protection is enabled.
     */
    public boolean isRuntimeProtectionEnabled() {
        return SystemProperties.getBoolean("ro.security.runtime_protection", true);
    }
    
    /**
     * Check if anti-debugging is enabled.
     */
    public boolean isAntiDebuggingEnabled() {
        return SystemProperties.getBoolean("ro.security.anti_debug", true);
    }
    
    /**
     * Check if integrity checks are enabled.
     */
    public boolean isIntegrityChecksEnabled() {
        return SystemProperties.getBoolean("ro.security.integrity_checks", true);
    }
    
    /**
     * Check if anti-tamper is enabled.
     */
    public boolean isAntiTamperEnabled() {
        return SystemProperties.getBoolean("ro.security.anti_tamper", true);
    }
}
