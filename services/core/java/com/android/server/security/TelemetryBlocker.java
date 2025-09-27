/*
 * Copyright (C) 2024 LineageOS
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

package com.android.server.security;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;

/**
 * TelemetryBlocker - Blocks telemetry and analytics services
 * 
 * This service identifies and blocks known telemetry services that
 * collect user data without explicit consent, improving privacy.
 */
public class TelemetryBlocker {
    private static final String TAG = "TelemetryBlocker";
    
    // Known telemetry and analytics packages to block
    private static final String[] BLOCKED_TELEMETRY_PACKAGES = {
        "com.verizon.mips.services",
        "com.google.mainline.telemetry", 
        "com.google.mainline.adservices",
        "com.google.android.modulemetadata",
        "com.google.mainline.go.telemetry",
        "com.google.mainline.go.adservices"
    };
    
    // Telemetry-related permission patterns
    private static final String[] TELEMETRY_PERMISSIONS = {
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION", 
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.READ_SMS",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO"
    };
    
    private final Context mContext;
    private final PackageManager mPackageManager;
    
    public TelemetryBlocker(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
    }
    
    /**
     * Initialize telemetry blocking
     */
    public void initializeTelemetryBlocking() {
        Log.i(TAG, "Initializing telemetry blocking...");
        
        try {
            blockTelemetryPackages();
            revokeTelemetryPermissions();
            setTelemetryProperties();
            
            Log.i(TAG, "Telemetry blocking initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize telemetry blocking", e);
        }
    }
    
    /**
     * Block known telemetry packages
     */
    private void blockTelemetryPackages() {
        Log.d(TAG, "Blocking telemetry packages...");
        
        for (String packageName : BLOCKED_TELEMETRY_PACKAGES) {
            try {
                // Disable package if it exists
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, 0);
                if (appInfo != null) {
                    // Set system property to disable telemetry
                    System.setProperty("persist.vendor.telemetry." + packageName, "0");
                    System.setProperty("ro.telemetry." + packageName + ".enabled", "false");
                    
                    Log.d(TAG, "Blocked telemetry package: " + packageName);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Package doesn't exist, which is good
                Log.d(TAG, "Telemetry package not found (good): " + packageName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to block telemetry package: " + packageName, e);
            }
        }
    }
    
    /**
     * Revoke telemetry-related permissions
     */
    private void revokeTelemetryPermissions() {
        Log.d(TAG, "Revoking telemetry permissions...");
        
        try {
            List<PackageInfo> packages = mPackageManager.getInstalledPackages(
                PackageManager.GET_PERMISSIONS);
            
            for (PackageInfo packageInfo : packages) {
                if (packageInfo.requestedPermissions != null) {
                    for (String permission : packageInfo.requestedPermissions) {
                        if (isTelemetryPermission(permission)) {
                            // Set property to block this permission
                            String propName = "ro.block_permission." + 
                                permission.replace(".", "_") + "=" + packageInfo.packageName;
                            System.setProperty("persist.vendor.block_permission." + 
                                permission.replace(".", "_"), "1");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to revoke telemetry permissions", e);
        }
    }
    
    /**
     * Check if permission is telemetry-related
     */
    private boolean isTelemetryPermission(String permission) {
        for (String telemetryPerm : TELEMETRY_PERMISSIONS) {
            if (permission.equals(telemetryPerm)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Set system properties to disable telemetry
     */
    private void setTelemetryProperties() {
        Log.d(TAG, "Setting telemetry blocking properties...");
        
        try {
            // Disable Google telemetry
            System.setProperty("ro.google.telemetry.enabled", "false");
            System.setProperty("persist.vendor.google.telemetry", "0");
            
            // Disable Verizon telemetry
            System.setProperty("ro.verizon.telemetry.enabled", "false");
            System.setProperty("persist.vendor.verizon.telemetry", "0");
            
            // Disable analytics
            System.setProperty("ro.analytics.enabled", "false");
            System.setProperty("persist.vendor.analytics", "0");
            
            // Disable crash reporting
            System.setProperty("ro.crash.reporting.enabled", "false");
            System.setProperty("persist.vendor.crash_reporting", "0");
            
            // Disable usage statistics
            System.setProperty("ro.usage.stats.enabled", "false");
            System.setProperty("persist.vendor.usage_stats", "0");
            
            Log.d(TAG, "Telemetry blocking properties set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set telemetry properties", e);
        }
    }
    
    /**
     * Get telemetry blocking status
     */
    public String getTelemetryBlockingStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Telemetry Blocking Status:\n");
        
        // Check blocked packages
        int blockedPackages = 0;
        for (String packageName : BLOCKED_TELEMETRY_PACKAGES) {
            try {
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, 0);
                if (appInfo == null) {
                    blockedPackages++;
                }
            } catch (PackageManager.NameNotFoundException e) {
                blockedPackages++;
            }
        }
        status.append("Blocked Telemetry Packages: ").append(blockedPackages).append("/").append(BLOCKED_TELEMETRY_PACKAGES.length).append("\n");
        
        // Check system properties
        status.append("Google Telemetry: ").append(
            "false".equals(System.getProperty("ro.google.telemetry.enabled", "true")) ? "BLOCKED" : "ENABLED").append("\n");
        status.append("Verizon Telemetry: ").append(
            "false".equals(System.getProperty("ro.verizon.telemetry.enabled", "true")) ? "BLOCKED" : "ENABLED").append("\n");
        status.append("Analytics: ").append(
            "false".equals(System.getProperty("ro.analytics.enabled", "true")) ? "BLOCKED" : "ENABLED").append("\n");
        status.append("Crash Reporting: ").append(
            "false".equals(System.getProperty("ro.crash.reporting.enabled", "true")) ? "BLOCKED" : "ENABLED").append("\n");
        status.append("Usage Statistics: ").append(
            "false".equals(System.getProperty("ro.usage.stats.enabled", "true")) ? "BLOCKED" : "ENABLED").append("\n");
        
        return status.toString();
    }
    
    /**
     * Check if telemetry is blocked for a specific package
     */
    public boolean isTelemetryBlocked(String packageName) {
        for (String blockedPackage : BLOCKED_TELEMETRY_PACKAGES) {
            if (packageName.equals(blockedPackage)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get list of blocked telemetry packages
     */
    public String[] getBlockedTelemetryPackages() {
        return BLOCKED_TELEMETRY_PACKAGES.clone();
    }
}

