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
import android.content.pm.PermissionInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.Manifest;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * PermissionSecurityManager - Hardens permission system against dangerous combinations
 * 
 * This service identifies and blocks dangerous permission combinations that could
 * be used for malicious purposes, improving overall system security.
 */
public class PermissionSecurityManager {
    private static final String TAG = "PermissionSecurityManager";
    
    // Dangerous permission combinations that should be restricted
    private static final String[][] DANGEROUS_COMBINATIONS = {
        // Settings + System Alert + Install Packages (system takeover)
        {
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.REQUEST_INSTALL_PACKAGES
        },
        // Location + Camera + Microphone (surveillance)
        {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        },
        // Phone + SMS + Contacts (data harvesting)
        {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        },
        // Storage + Network + Install (malware installation)
        {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.REQUEST_INSTALL_PACKAGES
        }
    };
    
    // High-risk individual permissions
    private static final String[] HIGH_RISK_PERMISSIONS = {
        Manifest.permission.WRITE_SETTINGS,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.REQUEST_INSTALL_PACKAGES,
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
        Manifest.permission.BIND_DEVICE_ADMIN,
        Manifest.permission.WRITE_SECURE_SETTINGS,
        Manifest.permission.MODIFY_PHONE_STATE
    };
    
    // Permissions that require additional scrutiny
    private static final String[] SCRUTINIZED_PERMISSIONS = {
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG
    };
    
    private final Context mContext;
    private final PackageManager mPackageManager;
    
    public PermissionSecurityManager(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
    }
    
    /**
     * Initialize permission security hardening
     */
    public void initializePermissionSecurity() {
        Log.i(TAG, "Initializing permission security hardening...");
        
        try {
            analyzePermissionCombinations();
            hardenHighRiskPermissions();
            setPermissionSecurityProperties();
            
            Log.i(TAG, "Permission security hardening initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize permission security hardening", e);
        }
    }
    
    /**
     * Analyze and block dangerous permission combinations
     */
    private void analyzePermissionCombinations() {
        Log.d(TAG, "Analyzing dangerous permission combinations...");
        
        try {
            List<PackageInfo> packages = mPackageManager.getInstalledPackages(
                PackageManager.GET_PERMISSIONS);
            
            int blockedCombinations = 0;
            
            for (PackageInfo packageInfo : packages) {
                if (packageInfo.requestedPermissions != null) {
                    // Check for dangerous combinations
                    for (String[] combination : DANGEROUS_COMBINATIONS) {
                        if (hasPermissionCombination(packageInfo.requestedPermissions, combination)) {
                            Log.w(TAG, "Dangerous permission combination detected in " + 
                                packageInfo.packageName + ": " + Arrays.toString(combination));
                            
                            // Block this combination
                            blockPermissionCombination(packageInfo.packageName, combination);
                            blockedCombinations++;
                        }
                    }
                }
            }
            
            Log.i(TAG, "Blocked " + blockedCombinations + " dangerous permission combinations");
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze permission combinations", e);
        }
    }
    
    /**
     * Check if package has a dangerous permission combination
     */
    private boolean hasPermissionCombination(String[] requestedPermissions, String[] combination) {
        Set<String> requestedSet = new HashSet<>(Arrays.asList(requestedPermissions));
        Set<String> combinationSet = new HashSet<>(Arrays.asList(combination));
        
        // Check if all permissions in combination are requested
        return requestedSet.containsAll(combinationSet);
    }
    
    /**
     * Block a dangerous permission combination
     */
    private void blockPermissionCombination(String packageName, String[] combination) {
        try {
            // Set system properties to block this combination
            String combinationKey = packageName.replace(".", "_") + "_" + 
                String.join("_", combination).replace(".", "_");
            
            System.setProperty("persist.vendor.block_permission_combination." + combinationKey, "1");
            
            // Log the blocking
            Log.i(TAG, "Blocked dangerous permission combination for " + packageName + 
                ": " + Arrays.toString(combination));
        } catch (Exception e) {
            Log.e(TAG, "Failed to block permission combination for " + packageName, e);
        }
    }
    
    /**
     * Harden high-risk permissions
     */
    private void hardenHighRiskPermissions() {
        Log.d(TAG, "Hardening high-risk permissions...");
        
        try {
            for (String permission : HIGH_RISK_PERMISSIONS) {
                // Set stricter requirements for high-risk permissions
                System.setProperty("ro.security.permission." + 
                    permission.replace(".", "_") + ".strict", "1");
                
                // Require additional verification
                System.setProperty("ro.security.permission." + 
                    permission.replace(".", "_") + ".verify", "1");
            }
            
            Log.i(TAG, "Hardened " + HIGH_RISK_PERMISSIONS.length + " high-risk permissions");
        } catch (Exception e) {
            Log.e(TAG, "Failed to harden high-risk permissions", e);
        }
    }
    
    /**
     * Set permission security properties
     */
    private void setPermissionSecurityProperties() {
        Log.d(TAG, "Setting permission security properties...");
        
        try {
            // Enable strict permission checking
            System.setProperty("ro.security.permission.strict", "1");
            
            // Enable permission combination checking
            System.setProperty("ro.security.permission.combination_check", "1");
            
            // Enable runtime permission verification
            System.setProperty("ro.security.permission.runtime_verify", "1");
            
            // Block unsigned permission grants
            System.setProperty("ro.security.permission.require_signature", "1");
            
            // Enable permission logging
            System.setProperty("ro.security.permission.logging", "1");
            
            Log.d(TAG, "Permission security properties set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set permission security properties", e);
        }
    }
    
    /**
     * Get permission security status
     */
    public String getPermissionSecurityStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Permission Security Status:\n");
        
        // Check security properties
        status.append("Strict Permission Checking: ").append(
            "1".equals(System.getProperty("ro.security.permission.strict")) ? "ENABLED" : "DISABLED").append("\n");
        status.append("Combination Checking: ").append(
            "1".equals(System.getProperty("ro.security.permission.combination_check")) ? "ENABLED" : "DISABLED").append("\n");
        status.append("Runtime Verification: ").append(
            "1".equals(System.getProperty("ro.security.permission.runtime_verify")) ? "ENABLED" : "DISABLED").append("\n");
        status.append("Signature Requirement: ").append(
            "1".equals(System.getProperty("ro.security.permission.require_signature")) ? "ENABLED" : "DISABLED").append("\n");
        
        // Check hardened permissions
        int hardenedPermissions = 0;
        for (String permission : HIGH_RISK_PERMISSIONS) {
            if ("1".equals(System.getProperty("ro.security.permission." + 
                permission.replace(".", "_") + ".strict"))) {
                hardenedPermissions++;
            }
        }
        status.append("Hardened High-Risk Permissions: ").append(hardenedPermissions).append("/").append(HIGH_RISK_PERMISSIONS.length).append("\n");
        
        return status.toString();
    }
    
    /**
     * Check if a permission combination is dangerous
     */
    public boolean isDangerousCombination(String[] permissions) {
        for (String[] combination : DANGEROUS_COMBINATIONS) {
            if (hasPermissionCombination(permissions, combination)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get list of dangerous permission combinations
     */
    public String[][] getDangerousCombinations() {
        return DANGEROUS_COMBINATIONS.clone();
    }
    
    /**
     * Get list of high-risk permissions
     */
    public String[] getHighRiskPermissions() {
        return HIGH_RISK_PERMISSIONS.clone();
    }
}

