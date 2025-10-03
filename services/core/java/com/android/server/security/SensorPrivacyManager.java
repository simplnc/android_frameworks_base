package com.android.server.security;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;

/**
 * Sensor privacy manager for enhanced sensor access control.
 * Implements GrapheneOS-style sensor privacy features.
 */
public class SensorPrivacyManager {
    private static final String TAG = "SensorPrivacyManager";
    private static final boolean DEBUG = false;
    
    private final Context mContext;
    private static SensorPrivacyManager sInstance;
    
    // Sensor privacy settings
    private static final String SENSOR_PRIVACY_ENABLED = "sensor_privacy_enabled";
    private static final String SENSOR_ACCESS_CONTROL = "sensor_access_control";
    private static final String OTHER_SENSORS_PERMISSION = "other_sensors_permission";
    
    // Protected sensors
    private static final Set<String> PROTECTED_SENSORS = new HashSet<>();
    
    static {
        PROTECTED_SENSORS.add("accelerometer");
        PROTECTED_SENSORS.add("gyroscope");
        PROTECTED_SENSORS.add("magnetometer");
        PROTECTED_SENSORS.add("proximity");
        PROTECTED_SENSORS.add("light");
        PROTECTED_SENSORS.add("pressure");
        PROTECTED_SENSORS.add("temperature");
        PROTECTED_SENSORS.add("humidity");
        PROTECTED_SENSORS.add("step_counter");
        PROTECTED_SENSORS.add("step_detector");
        PROTECTED_SENSORS.add("heart_rate");
        PROTECTED_SENSORS.add("rotation_vector");
        PROTECTED_SENSORS.add("linear_acceleration");
        PROTECTED_SENSORS.add("gravity");
        PROTECTED_SENSORS.add("game_rotation_vector");
        PROTECTED_SENSORS.add("geomagnetic_rotation_vector");
    }
    
    private SensorPrivacyManager(Context context) {
        mContext = context;
        initializeSensorPrivacy();
    }
    
    public static synchronized SensorPrivacyManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SensorPrivacyManager(context);
        }
        return sInstance;
    }
    
    /**
     * Initialize sensor privacy features.
     */
    private void initializeSensorPrivacy() {
        try {
            // Enable sensor privacy by default
            enableSensorPrivacy();
            
            // Configure sensor access control
            configureSensorAccessControl();
            
            // Set up OTHER_SENSORS permission requirement
            setupOtherSensorsPermission();
            
            if (DEBUG) {
                Log.d(TAG, "Sensor privacy manager initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize sensor privacy", e);
        }
    }
    
    /**
     * Enable sensor privacy protection.
     */
    private void enableSensorPrivacy() {
        try {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.SENSOR_PRIVACY_ENABLED, 1);
            
            // Set system properties
            SystemProperties.set("ro.security.sensor_privacy", "1");
            SystemProperties.set("ro.security.sensor_access_control", "1");
            
            if (DEBUG) {
                Log.d(TAG, "Sensor privacy enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable sensor privacy", e);
        }
    }
    
    /**
     * Configure sensor access control.
     */
    private void configureSensorAccessControl() {
        try {
            // Enable strict sensor access control
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.SENSOR_ACCESS_CONTROL, "strict");
            
            // Set system properties for sensor protection
            SystemProperties.set("ro.security.sensor_protection", "1");
            SystemProperties.set("ro.security.sensor_whitelist", "0");
            
            if (DEBUG) {
                Log.d(TAG, "Sensor access control configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure sensor access control", e);
        }
    }
    
    /**
     * Set up OTHER_SENSORS permission requirement.
     */
    private void setupOtherSensorsPermission() {
        try {
            // Require OTHER_SENSORS permission for sensor access
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.OTHER_SENSORS_PERMISSION, 1);
            
            // Set system properties
            SystemProperties.set("ro.security.other_sensors_permission", "1");
            SystemProperties.set("ro.security.sensor_permission_strict", "1");
            
            if (DEBUG) {
                Log.d(TAG, "OTHER_SENSORS permission requirement configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup OTHER_SENSORS permission", e);
        }
    }
    
    /**
     * Check if sensor access is allowed for a package.
     */
    public boolean isSensorAccessAllowed(String packageName, String sensorType) {
        try {
            if (!isSensorPrivacyEnabled()) {
                return true; // Allow if privacy is disabled
            }
            
            // Check if sensor is protected
            if (!isProtectedSensor(sensorType)) {
                return true; // Allow access to non-protected sensors
            }
            
            // Check if package has OTHER_SENSORS permission
            if (hasOtherSensorsPermission(packageName)) {
                return true; // Allow if permission is granted
            }
            
            // Log denied access attempt
            if (DEBUG) {
                Log.d(TAG, "Sensor access denied for " + packageName + " to " + sensorType);
            }
            
            return false; // Deny access
        } catch (Exception e) {
            Log.e(TAG, "Failed to check sensor access", e);
            return false; // Deny on error
        }
    }
    
    /**
     * Check if sensor privacy is enabled.
     */
    public boolean isSensorPrivacyEnabled() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.SENSOR_PRIVACY_ENABLED, 1) != 0;
        } catch (Exception e) {
            return true; // Default to enabled
        }
    }
    
    /**
     * Check if sensor is protected.
     */
    public boolean isProtectedSensor(String sensorType) {
        return PROTECTED_SENSORS.contains(sensorType.toLowerCase());
    }
    
    /**
     * Check if package has OTHER_SENSORS permission.
     */
    private boolean hasOtherSensorsPermission(String packageName) {
        try {
            // This would typically check with PackageManager
            // For now, we'll implement a basic check
            return false; // Strict by default
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get sensor privacy status.
     */
    public String getSensorPrivacyStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Sensor Privacy Status:\n");
        status.append("Enabled: ").append(isSensorPrivacyEnabled() ? "YES" : "NO").append("\n");
        status.append("Access Control: ").append(
                Settings.Secure.getString(mContext.getContentResolver(),
                        Settings.Secure.SENSOR_ACCESS_CONTROL)).append("\n");
        status.append("OTHER_SENSORS Permission: ").append(
                SystemProperties.getBoolean("ro.security.other_sensors_permission", false) ? "REQUIRED" : "NOT_REQUIRED").append("\n");
        status.append("Protected Sensors: ").append(PROTECTED_SENSORS.size()).append("\n");
        
        return status.toString();
    }
}
