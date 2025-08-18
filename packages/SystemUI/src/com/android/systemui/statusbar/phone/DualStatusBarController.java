/*
 * Copyright (C) 2025 Zeus-OS
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

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.util.settings.SecureSettings;

import javax.inject.Inject;

/**
 * Controller for managing dual status bar functionality
 * Handles enabling/disabling and configuration of the dual status bar
 */
@SysUISingleton
public class DualStatusBarController {
    private static final String TAG = "DualStatusBarController";
    
    // Settings keys
    private static final String KEY_DUAL_STATUS_BAR_ENABLED = "dual_status_bar_enabled";
    private static final String KEY_DUAL_STATUS_BAR_LAYOUT = "dual_status_bar_layout";
    private static final String KEY_DUAL_STATUS_BAR_SHOW_NETWORK_TRAFFIC = "dual_status_bar_show_network_traffic";
    private static final String KEY_DUAL_STATUS_BAR_SHOW_CLOCK_SECONDARY = "dual_status_bar_show_clock_secondary";
    
    // Default values
    private static final boolean DEFAULT_DUAL_STATUS_BAR_ENABLED = false;
    private static final String DEFAULT_DUAL_STATUS_BAR_LAYOUT = "standard";
    private static final boolean DEFAULT_DUAL_STATUS_BAR_SHOW_NETWORK_TRAFFIC = true;
    private static final boolean DEFAULT_DUAL_STATUS_BAR_SHOW_CLOCK_SECONDARY = true;
    
    private final Context mContext;
    private final SecureSettings mSecureSettings;
    private final UserTracker mUserTracker;
    
    private boolean mDualStatusBarEnabled;
    private String mDualStatusBarLayout;
    private boolean mShowNetworkTraffic;
    private boolean mShowClockSecondary;
    
    @Inject
    public DualStatusBarController(Context context, SecureSettings secureSettings, 
                                 UserTracker userTracker) {
        mContext = context;
        mSecureSettings = secureSettings;
        mUserTracker = userTracker;
        
        loadSettings();
    }
    
    /**
     * Load settings from SecureSettings
     */
    private void loadSettings() {
        mDualStatusBarEnabled = mSecureSettings.getIntForUser(
                KEY_DUAL_STATUS_BAR_ENABLED, 
                DEFAULT_DUAL_STATUS_BAR_ENABLED ? 1 : 0, 
                mUserTracker.getUserId()) == 1;
        
        mDualStatusBarLayout = mSecureSettings.getStringForUser(
                KEY_DUAL_STATUS_BAR_LAYOUT, 
                mUserTracker.getUserId());
        if (mDualStatusBarLayout == null) {
            mDualStatusBarLayout = DEFAULT_DUAL_STATUS_BAR_LAYOUT;
        }
        
        mShowNetworkTraffic = mSecureSettings.getIntForUser(
                KEY_DUAL_STATUS_BAR_SHOW_NETWORK_TRAFFIC, 
                DEFAULT_DUAL_STATUS_BAR_SHOW_NETWORK_TRAFFIC ? 1 : 0, 
                mUserTracker.getUserId()) == 1;
        
        mShowClockSecondary = mSecureSettings.getIntForUser(
                KEY_DUAL_STATUS_BAR_SHOW_CLOCK_SECONDARY, 
                DEFAULT_DUAL_STATUS_BAR_SHOW_CLOCK_SECONDARY ? 1 : 0, 
                mUserTracker.getUserId()) == 1;
        
        Log.d(TAG, "Loaded settings: enabled=" + mDualStatusBarEnabled + 
              ", layout=" + mDualStatusBarLayout + 
              ", networkTraffic=" + mShowNetworkTraffic + 
              ", clockSecondary=" + mShowClockSecondary);
    }
    
    /**
     * Check if dual status bar is enabled
     */
    public boolean isDualStatusBarEnabled() {
        return mDualStatusBarEnabled;
    }
    
    /**
     * Get the current dual status bar layout style
     */
    public String getDualStatusBarLayout() {
        return mDualStatusBarLayout;
    }
    
    /**
     * Check if network traffic should be shown in secondary status bar
     */
    public boolean shouldShowNetworkTraffic() {
        return mShowNetworkTraffic;
    }
    
    /**
     * Check if clock should be shown in secondary status bar
     */
    public boolean shouldShowClockSecondary() {
        return mShowClockSecondary;
    }
    
    /**
     * Update settings and reload
     */
    public void updateSettings() {
        loadSettings();
    }
    
    /**
     * Set dual status bar enabled state
     */
    public void setDualStatusBarEnabled(boolean enabled) {
        mSecureSettings.putIntForUser(KEY_DUAL_STATUS_BAR_ENABLED, enabled ? 1 : 0, 
                                    mUserTracker.getUserId());
        mDualStatusBarEnabled = enabled;
        Log.d(TAG, "Dual status bar " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Set dual status bar layout style
     */
    public void setDualStatusBarLayout(String layout) {
        mSecureSettings.putStringForUser(KEY_DUAL_STATUS_BAR_LAYOUT, layout, 
                                       mUserTracker.getUserId());
        mDualStatusBarLayout = layout;
        Log.d(TAG, "Dual status bar layout set to: " + layout);
    }
    
    /**
     * Set whether to show network traffic in secondary status bar
     */
    public void setShowNetworkTraffic(boolean show) {
        mSecureSettings.putIntForUser(KEY_DUAL_STATUS_BAR_SHOW_NETWORK_TRAFFIC, show ? 1 : 0, 
                                    mUserTracker.getUserId());
        mShowNetworkTraffic = show;
        Log.d(TAG, "Network traffic " + (show ? "enabled" : "disabled") + " in secondary status bar");
    }
    
    /**
     * Set whether to show clock in secondary status bar
     */
    public void setShowClockSecondary(boolean show) {
        mSecureSettings.putIntForUser(KEY_DUAL_STATUS_BAR_SHOW_CLOCK_SECONDARY, show ? 1 : 0, 
                                    mUserTracker.getUserId());
        mShowClockSecondary = show;
        Log.d(TAG, "Clock " + (show ? "enabled" : "disabled") + " in secondary status bar");
    }
}
