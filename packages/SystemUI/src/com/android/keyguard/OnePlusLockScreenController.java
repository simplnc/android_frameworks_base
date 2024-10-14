/*
 * Copyright (C) 2024 The LineageOS Project
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

package com.android.keyguard;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextClock;

import com.android.systemui.res.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Controller for OnePlus-style lock screen functionality
 * Manages clock, date, weather, battery, notifications, and quick actions
 */
public class OnePlusLockScreenController {
    private static final String TAG = "OnePlusLockScreenController";
    
    // Settings keys for user configuration
    private static final String LOCKSCREEN_ONEPLUS_STYLE = "lockscreen_oneplus_style";
    private static final String LOCKSCREEN_SHOW_WEATHER = "lockscreen_show_weather";
    private static final String LOCKSCREEN_SHOW_BATTERY = "lockscreen_show_battery";
    private static final String LOCKSCREEN_SHOW_NOTIFICATIONS = "lockscreen_show_notifications";
    private static final String LOCKSCREEN_SHOW_DATE = "lockscreen_show_date";
    
    private final Context mContext;
    private final Handler mHandler;
    
    // UI Components
    private TextClock mClockView;
    private TextView mDateView;
    private TextView mWeatherView;
    private TextView mSeparatorView;
    private TextView mBatteryView;
    private TextView mNotificationsView;
    private TextView mInfoSeparatorView;
    private ImageView mFlashlightButton;
    private ImageView mCameraButton;
    private View mDateWeatherContainer;
    private View mInfoContainer;
    private View mQuickActionsContainer;
    
    // Settings
    private boolean mOnePlusStyleEnabled = true;
    private boolean mShowWeather = false;
    private boolean mShowBattery = true;
    private boolean mShowNotifications = false;
    private boolean mShowDate = true;
    
    // Battery info
    private int mBatteryLevel = 0;
    private boolean mBatteryCharging = false;
    
    // Notification count
    private int mNotificationCount = 0;
    
    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                mBatteryCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 
                    BatteryManager.BATTERY_STATUS_UNKNOWN) == BatteryManager.BATTERY_STATUS_CHARGING;
                updateBatteryDisplay();
            }
        }
    };
    
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateSettings();
            updateVisibility();
        }
    };
    
    public OnePlusLockScreenController(Context context, View rootView) {
        mContext = context;
        mHandler = new Handler();
        
        initViews(rootView);
        updateSettings();
        registerReceivers();
        setupClickListeners();
        updateVisibility();
        updateDisplays();
    }
    
    private void initViews(View rootView) {
        // Map to new layout IDs - some views may not exist in new layout
        mClockView = rootView.findViewById(R.id.oos_date); // Use date TextClock as main clock
        mDateView = rootView.findViewById(R.id.oos_date);
        mWeatherView = rootView.findViewById(R.id.weather_text);
        mSeparatorView = null; // Not present in new layout
        mBatteryView = null; // Not present in new layout
        mNotificationsView = null; // Not present in new layout
        mInfoSeparatorView = null; // Not present in new layout
        mFlashlightButton = null; // Not present in new layout
        mCameraButton = null; // Not present in new layout
        mDateWeatherContainer = null; // Not present in new layout
        mInfoContainer = null; // Not present in new layout
        mQuickActionsContainer = null; // Not present in new layout
    }
    
    private void updateSettings() {
        try {
            mOnePlusStyleEnabled = Settings.System.getInt(mContext.getContentResolver(),
                LOCKSCREEN_ONEPLUS_STYLE, 1) == 1;
            mShowWeather = Settings.System.getInt(mContext.getContentResolver(),
                LOCKSCREEN_SHOW_WEATHER, 0) == 1;
            mShowBattery = Settings.System.getInt(mContext.getContentResolver(),
                LOCKSCREEN_SHOW_BATTERY, 1) == 1;
            mShowNotifications = Settings.System.getInt(mContext.getContentResolver(),
                LOCKSCREEN_SHOW_NOTIFICATIONS, 0) == 1;
            mShowDate = Settings.System.getInt(mContext.getContentResolver(),
                LOCKSCREEN_SHOW_DATE, 1) == 1;
        } catch (Exception e) {
            Log.e(TAG, "Error reading settings: " + e.getMessage());
        }
    }
    
    private void registerReceivers() {
        // Register battery receiver
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mBatteryReceiver, batteryFilter);
        
        // Register settings observer
        mContext.getContentResolver().registerContentObserver(
            Settings.System.getUriFor(LOCKSCREEN_ONEPLUS_STYLE), false, mSettingsObserver);
        mContext.getContentResolver().registerContentObserver(
            Settings.System.getUriFor(LOCKSCREEN_SHOW_WEATHER), false, mSettingsObserver);
        mContext.getContentResolver().registerContentObserver(
            Settings.System.getUriFor(LOCKSCREEN_SHOW_BATTERY), false, mSettingsObserver);
        mContext.getContentResolver().registerContentObserver(
            Settings.System.getUriFor(LOCKSCREEN_SHOW_NOTIFICATIONS), false, mSettingsObserver);
        mContext.getContentResolver().registerContentObserver(
            Settings.System.getUriFor(LOCKSCREEN_SHOW_DATE), false, mSettingsObserver);
    }
    
    private void setupClickListeners() {
        // Flashlight and camera buttons are not present in new layout
        // This method is kept for compatibility but does nothing
    }
    
    private void updateVisibility() {
        if (!mOnePlusStyleEnabled) {
            // Hide all OnePlus components if disabled
            hideAllComponents();
            return;
        }
        
        // Show/hide date (oos_date is the main date display)
        if (mDateView != null) {
            mDateView.setVisibility(mShowDate ? View.VISIBLE : View.GONE);
        }
        
        // Show/hide weather
        if (mWeatherView != null) {
            mWeatherView.setVisibility(mShowWeather ? View.VISIBLE : View.GONE);
        }
        
        // Other components are not present in new layout, so no visibility changes needed
    }
    
    private void hideAllComponents() {
        // Most components are not present in new layout, so minimal hiding needed
        if (mDateView != null) mDateView.setVisibility(View.GONE);
        if (mWeatherView != null) mWeatherView.setVisibility(View.GONE);
    }
    
    private void updateDisplays() {
        updateDateDisplay();
        updateWeatherDisplay();
        updateBatteryDisplay();
        updateNotificationDisplay();
    }
    
    private void updateDateDisplay() {
        if (mDateView != null && mShowDate) {
            try {
                // The oos_date TextClock handles its own formatting, so we don't need to set text
                // Just ensure it's visible
                mDateView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error updating date: " + e.getMessage());
            }
        }
    }
    
    private void updateWeatherDisplay() {
        if (mWeatherView != null && mShowWeather) {
            // Placeholder for weather integration
            // In a real implementation, this would connect to a weather service
            mWeatherView.setText("22°C");
            mWeatherView.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateBatteryDisplay() {
        // Battery display not present in new layout
        // This method is kept for compatibility but does nothing
    }
    
    private void updateNotificationDisplay() {
        // Notification display not present in new layout
        // This method is kept for compatibility but does nothing
    }
    
    public void setNotificationCount(int count) {
        mNotificationCount = count;
        updateNotificationDisplay();
        updateVisibility();
    }
    
    public void onDestroy() {
        try {
            mContext.unregisterReceiver(mBatteryReceiver);
            mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage());
        }
    }
    
    public void refresh() {
        updateSettings();
        updateVisibility();
        updateDisplays();
    }
}
