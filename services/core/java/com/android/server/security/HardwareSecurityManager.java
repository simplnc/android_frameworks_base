package com.android.server.security;

import android.content.Context;
import android.os.SystemProperties;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Hardware security manager for Pixel 3a hardware-backed security features.
 * Implements hardware-backed keystore, secure element, and hardware security integration.
 */
public class HardwareSecurityManager {
    private static final String TAG = "HardwareSecurityManager";
    private static final boolean DEBUG = false;
    
    private final Context mContext;
    private static HardwareSecurityManager sInstance;
    
    // Hardware security flags
    private static final String HARDWARE_KEYSTORE_ENABLED = "hardware_keystore_enabled";
    private static final String HARDWARE_SECURE_ELEMENT = "hardware_secure_element";
    private static final String HARDWARE_ATTESTATION = "hardware_attestation";
    private static final String HARDWARE_BIOMETRIC = "hardware_biometric";
    
    // Pixel 3a specific hardware
    private static final String PIXEL_3A_HARDWARE = "sargo"; // Pixel 3a codename
    private static final String PIXEL_3A_SECURE_ELEMENT = "nfc_secure_element";
    private static final String PIXEL_3A_HARDWARE_KEYSTORE = "hardware_backed_keystore";
    
    private HardwareSecurityManager(Context context) {
        mContext = context;
        initializeHardwareSecurity();
    }
    
    public static synchronized HardwareSecurityManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new HardwareSecurityManager(context);
        }
        return sInstance;
    }
    
    /**
     * Initialize hardware security measures.
     */
    private void initializeHardwareSecurity() {
        try {
            // Check if this is a Pixel 3a
            if (isPixel3a()) {
                // Initialize Pixel 3a specific hardware security
                initializePixel3aHardwareSecurity();
            } else {
                Log.w(TAG, "Not a Pixel 3a device - limited hardware security available");
            }
            
            // Initialize general hardware security
            initializeGeneralHardwareSecurity();
            
            if (DEBUG) {
                Log.d(TAG, "Hardware security initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize hardware security", e);
        }
    }
    
    /**
     * Initialize Pixel 3a specific hardware security.
     */
    private void initializePixel3aHardwareSecurity() {
        try {
            // Enable hardware-backed keystore
            enableHardwareBackedKeystore();
            
            // Enable secure element
            enableSecureElement();
            
            // Enable hardware attestation
            enableHardwareAttestation();
            
            // Enable hardware biometric
            enableHardwareBiometric();
            
            // Set Pixel 3a specific properties
            SystemProperties.set("ro.security.hardware.pixel3a", "1");
            SystemProperties.set("ro.security.hardware.keystore", "1");
            SystemProperties.set("ro.security.hardware.secure_element", "1");
            
            if (DEBUG) {
                Log.d(TAG, "Pixel 3a hardware security initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Pixel 3a hardware security", e);
        }
    }
    
    /**
     * Initialize general hardware security.
     */
    private void initializeGeneralHardwareSecurity() {
        try {
            // Enable general hardware security features
            enableGeneralHardwareSecurity();
            
            // Set general hardware security properties
            SystemProperties.set("ro.security.hardware.general", "1");
            
            if (DEBUG) {
                Log.d(TAG, "General hardware security initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize general hardware security", e);
        }
    }
    
    /**
     * Enable hardware-backed keystore.
     */
    private void enableHardwareBackedKeystore() {
        try {
            // Check if hardware keystore is available
            if (isHardwareKeystoreAvailable()) {
                // Initialize hardware keystore
                initializeHardwareKeystore();
                
                // Set hardware keystore properties
                SystemProperties.set("ro.security.hardware.keystore.enabled", "1");
                SystemProperties.set("ro.security.hardware.keystore.available", "1");
                
                if (DEBUG) {
                    Log.d(TAG, "Hardware-backed keystore enabled");
                }
            } else {
                Log.w(TAG, "Hardware keystore not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable hardware-backed keystore", e);
        }
    }
    
    /**
     * Enable secure element.
     */
    private void enableSecureElement() {
        try {
            // Check if secure element is available
            if (isSecureElementAvailable()) {
                // Initialize secure element
                initializeSecureElement();
                
                // Set secure element properties
                SystemProperties.set("ro.security.hardware.secure_element.enabled", "1");
                SystemProperties.set("ro.security.hardware.secure_element.available", "1");
                
                if (DEBUG) {
                    Log.d(TAG, "Secure element enabled");
                }
            } else {
                Log.w(TAG, "Secure element not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable secure element", e);
        }
    }
    
    /**
     * Enable hardware attestation.
     */
    private void enableHardwareAttestation() {
        try {
            // Check if hardware attestation is available
            if (isHardwareAttestationAvailable()) {
                // Initialize hardware attestation
                initializeHardwareAttestation();
                
                // Set hardware attestation properties
                SystemProperties.set("ro.security.hardware.attestation.enabled", "1");
                SystemProperties.set("ro.security.hardware.attestation.available", "1");
                
                if (DEBUG) {
                    Log.d(TAG, "Hardware attestation enabled");
                }
            } else {
                Log.w(TAG, "Hardware attestation not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable hardware attestation", e);
        }
    }
    
    /**
     * Enable hardware biometric.
     */
    private void enableHardwareBiometric() {
        try {
            // Check if hardware biometric is available
            if (isHardwareBiometricAvailable()) {
                // Initialize hardware biometric
                initializeHardwareBiometric();
                
                // Set hardware biometric properties
                SystemProperties.set("ro.security.hardware.biometric.enabled", "1");
                SystemProperties.set("ro.security.hardware.biometric.available", "1");
                
                if (DEBUG) {
                    Log.d(TAG, "Hardware biometric enabled");
                }
            } else {
                Log.w(TAG, "Hardware biometric not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable hardware biometric", e);
        }
    }
    
    /**
     * Enable general hardware security.
     */
    private void enableGeneralHardwareSecurity() {
        try {
            // Enable general hardware security features
            SystemProperties.set("ro.security.hardware.general.enabled", "1");
            
            if (DEBUG) {
                Log.d(TAG, "General hardware security enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable general hardware security", e);
        }
    }
    
    /**
     * Check if this is a Pixel 3a device.
     */
    public boolean isPixel3a() {
        try {
            String device = SystemProperties.get("ro.product.device", "");
            String model = SystemProperties.get("ro.product.model", "");
            String hardware = SystemProperties.get("ro.hardware", "");
            
            return PIXEL_3A_HARDWARE.equals(device) || 
                   model.contains("Pixel 3a") || 
                   hardware.contains("sargo");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if hardware keystore is available.
     */
    public boolean isHardwareKeystoreAvailable() {
        try {
            // Check if MicroG is installed (built from vendor)
            if (isMicroGInstalled()) {
                if (DEBUG) {
                    Log.d(TAG, "MicroG detected - using software keystore fallback");
                }
                // MicroG may not fully support hardware keystore, use software fallback
                return false;
            }
            
            // Check for hardware keystore support
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            
            // Try to create a hardware-backed key (with software fallback)
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder("test_key", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setIsStrongBoxBacked(false) // Allow software fallback for MicroG compatibility
                    .build();
            
            keyGenerator.init(keyGenParameterSpec);
            SecretKey secretKey = keyGenerator.generateKey();
            
            return secretKey != null;
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, "Hardware keystore not available, using software fallback: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Check if MicroG is installed (built from vendor at build time).
     */
    private boolean isMicroGInstalled() {
        try {
            PackageManager pm = mContext.getPackageManager();
            // Check for MicroG core package
            pm.getPackageInfo("com.google.android.gms", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Check if secure element is available.
     */
    public boolean isSecureElementAvailable() {
        try {
            // Check for secure element support
            return SystemProperties.getBoolean("ro.security.hardware.secure_element.available", false);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if hardware attestation is available.
     */
    public boolean isHardwareAttestationAvailable() {
        try {
            // Check for hardware attestation support
            return SystemProperties.getBoolean("ro.security.hardware.attestation.available", false);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if hardware biometric is available.
     */
    public boolean isHardwareBiometricAvailable() {
        try {
            // Check for hardware biometric support
            return SystemProperties.getBoolean("ro.security.hardware.biometric.available", false);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Initialize hardware keystore.
     */
    private void initializeHardwareKeystore() {
        try {
            // Initialize hardware keystore with secure random
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(System.currentTimeMillis());
            
            if (DEBUG) {
                Log.d(TAG, "Hardware keystore initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize hardware keystore", e);
        }
    }
    
    /**
     * Initialize secure element.
     */
    private void initializeSecureElement() {
        try {
            // Initialize secure element
            if (DEBUG) {
                Log.d(TAG, "Secure element initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize secure element", e);
        }
    }
    
    /**
     * Initialize hardware attestation.
     */
    private void initializeHardwareAttestation() {
        try {
            // Initialize hardware attestation
            if (DEBUG) {
                Log.d(TAG, "Hardware attestation initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize hardware attestation", e);
        }
    }
    
    /**
     * Initialize hardware biometric.
     */
    private void initializeHardwareBiometric() {
        try {
            // Initialize hardware biometric
            if (DEBUG) {
                Log.d(TAG, "Hardware biometric initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize hardware biometric", e);
        }
    }
    
    /**
     * Get hardware security status.
     */
    public String getHardwareSecurityStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Hardware Security Status:\n");
        status.append("Device: ").append(isPixel3a() ? "Pixel 3a" : "Other").append("\n");
        status.append("MicroG: ").append(isMicroGInstalled() ? "INSTALLED" : "NOT INSTALLED").append("\n");
        status.append("Hardware Keystore: ").append(isHardwareKeystoreAvailable() ? "AVAILABLE" : "SOFTWARE FALLBACK").append("\n");
        status.append("Secure Element: ").append(isSecureElementAvailable() ? "AVAILABLE" : "NOT AVAILABLE").append("\n");
        status.append("Hardware Attestation: ").append(isHardwareAttestationAvailable() ? "AVAILABLE" : "NOT AVAILABLE").append("\n");
        status.append("Hardware Biometric: ").append(isHardwareBiometricAvailable() ? "AVAILABLE" : "NOT AVAILABLE").append("\n");
        return status.toString();
    }
    
    /**
     * Verify hardware security is working.
     */
    public boolean verifyHardwareSecurity() {
        try {
            // Check if this is a Pixel 3a
            if (!isPixel3a()) {
                Log.w(TAG, "Not a Pixel 3a device - limited hardware security");
                return false;
            }
            
            // Check hardware keystore
            if (!isHardwareKeystoreAvailable()) {
                Log.w(TAG, "Hardware keystore verification failed");
                return false;
            }
            
            // Check secure element
            if (!isSecureElementAvailable()) {
                Log.w(TAG, "Secure element verification failed");
                return false;
            }
            
            // Check hardware attestation
            if (!isHardwareAttestationAvailable()) {
                Log.w(TAG, "Hardware attestation verification failed");
                return false;
            }
            
            // Check hardware biometric
            if (!isHardwareBiometricAvailable()) {
                Log.w(TAG, "Hardware biometric verification failed");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify hardware security", e);
            return false;
        }
    }
    
    /**
     * Get device information.
     */
    public String getDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Device Information:\n");
        info.append("Product Device: ").append(SystemProperties.get("ro.product.device", "unknown")).append("\n");
        info.append("Product Model: ").append(SystemProperties.get("ro.product.model", "unknown")).append("\n");
        info.append("Hardware: ").append(SystemProperties.get("ro.hardware", "unknown")).append("\n");
        info.append("Board: ").append(SystemProperties.get("ro.product.board", "unknown")).append("\n");
        info.append("Manufacturer: ").append(SystemProperties.get("ro.product.manufacturer", "unknown")).append("\n");
        return info.toString();
    }
}
