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

package com.android.server.locksettings;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.os.Handler;
import android.os.SystemProperties;
import java.util.concurrent.TimeUnit;

/**
 * DuressPINHandler - Handles duress PIN input during lock screen authentication
 * 
 * This handler intercepts PIN input and checks for duress PIN codes,
 * triggering emergency wipe when detected.
 */
public class DuressPINHandler {
    private static final String TAG = "DuressPINHandler";
    
    // Default duress PIN codes
    private static final String[] DURESS_PINS = {
        "9119",    // Emergency duress PIN
        "911",     // Alternative emergency PIN
        "9999"     // Secondary duress PIN
    };
    
    private final Context mContext;
    private final Handler mHandler;
    
    public DuressPINHandler(Context context) {
        mContext = context;
        mHandler = new Handler();
    }
    
    /**
     * Check if entered PIN is a duress PIN
     */
    public boolean isDuressPIN(String enteredPIN) {
        if (enteredPIN == null || enteredPIN.isEmpty()) {
            return false;
        }
        
        // Check against default duress PINs
        for (String duressPIN : DURESS_PINS) {
            if (enteredPIN.equals(duressPIN)) {
                Log.w(TAG, "Duress PIN detected: " + maskPIN(enteredPIN));
                return true;
            }
        }
        
        // Check against custom duress PIN from settings
        String customDuressPIN = Settings.Secure.getString(
            mContext.getContentResolver(), 
            Settings.Secure.DURESS_PIN_CODE
        );
        
        if (customDuressPIN != null && !customDuressPIN.isEmpty() && 
            enteredPIN.equals(customDuressPIN)) {
            Log.w(TAG, "Custom duress PIN detected: " + maskPIN(enteredPIN));
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle duress PIN detection
     */
    public void handleDuressPIN(String duressPIN) {
        Log.w(TAG, "Duress PIN activated - initiating emergency wipe");
        
        // Log the duress PIN activation
        Log.w(TAG, "DUress PIN ACTIVATED: " + maskPIN(duressPIN) + " at " + 
            System.currentTimeMillis());
        
        // Start emergency wipe sequence
        startEmergencyWipeSequence();
    }
    
    /**
     * Start emergency wipe sequence with countdown
     */
    private void startEmergencyWipeSequence() {
        Log.w(TAG, "Starting emergency wipe sequence...");
        
        // Get wipe delay from settings or use default
        long wipeDelay = getWipeDelay();
        
        // Start countdown
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeEmergencyWipe();
            }
        }, wipeDelay);
        
        // Log countdown start
        Log.w(TAG, "Emergency wipe countdown started: " + (wipeDelay / 1000) + " seconds");
    }
    
    /**
     * Execute emergency wipe
     */
    private void executeEmergencyWipe() {
        Log.w(TAG, "EXECUTING EMERGENCY WIPE");
        
        try {
            // Set wipe flag
            Settings.Secure.putInt(mContext.getContentResolver(), 
                Settings.Secure.EMERGENCY_WIPE_TRIGGERED, 1);
            
            // Trigger factory reset
            SystemProperties.set("sys.factory_reset", "1");
            SystemProperties.set("persist.sys.factory_reset", "1");
            
            // Trigger secure deletion
            SystemProperties.set("sys.secure_deletion", "1");
            SystemProperties.set("persist.sys.secure_deletion", "1");
            
            // Log wipe execution
            Log.w(TAG, "Emergency wipe executed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute emergency wipe", e);
        }
    }
    
    /**
     * Get wipe delay from settings
     */
    private long getWipeDelay() {
        try {
            long delay = Settings.Secure.getLong(mContext.getContentResolver(), 
                Settings.Secure.DURESS_PIN_WIPE_DELAY, 5000);
            return Math.max(1000, Math.min(delay, 30000)); // 1-30 seconds
        } catch (Exception e) {
            return 5000; // Default 5 seconds
        }
    }
    
    /**
     * Mask PIN for logging (show only first and last digit)
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
    
    /**
     * Check if duress PIN is enabled
     */
    public boolean isDuressPINEnabled() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(), 
                Settings.Secure.DURESS_PIN_ENABLED, 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get duress PIN status for debugging
     */
    public String getDuressPINStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Duress PIN Handler Status:\n");
        status.append("Enabled: ").append(isDuressPINEnabled() ? "YES" : "NO").append("\n");
        
        if (isDuressPINEnabled()) {
            String customPIN = Settings.Secure.getString(mContext.getContentResolver(), 
                Settings.Secure.DURESS_PIN_CODE);
            status.append("Custom PIN: ").append(customPIN != null ? maskPIN(customPIN) : "NOT_SET").append("\n");
        }
        
        status.append("Default PINs: ");
        for (int i = 0; i < DURESS_PINS.length; i++) {
            status.append(maskPIN(DURESS_PINS[i]));
            if (i < DURESS_PINS.length - 1) status.append(", ");
        }
        status.append("\n");
        
        return status.toString();
    }
}

