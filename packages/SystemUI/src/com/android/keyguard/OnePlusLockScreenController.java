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

import com.android.systemui.R;

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
        mClockView = rootView.findViewById(R.id.oneplus_clock);
        mDateView = rootView.findViewById(R.id.oneplus_date);
        mWeatherView = rootView.findViewById(R.id.oneplus_weather);
        mSeparatorView = rootView.findViewById(R.id.oneplus_separator);
        mBatteryView = rootView.findViewById(R.id.oneplus_battery);
        mNotificationsView = rootView.findViewById(R.id.oneplus_notifications);
        mInfoSeparatorView = rootView.findViewById(R.id.oneplus_info_separator);
        mFlashlightButton = rootView.findViewById(R.id.oneplus_flashlight);
        mCameraButton = rootView.findViewById(R.id.oneplus_camera);
        mDateWeatherContainer = rootView.findViewById(R.id.oneplus_date_weather_container);
        mInfoContainer = rootView.findViewById(R.id.oneplus_info_container);
        mQuickActionsContainer = rootView.findViewById(R.id.oneplus_quick_actions);
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
        if (mFlashlightButton != null) {
            mFlashlightButton.setOnClickListener(v -> {
                try {
                    Intent flashlightIntent = new Intent("android.intent.action.FLASHLIGHT");
                    flashlightIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(flashlightIntent);
                } catch (Exception e) {
                    Log.w(TAG, "Could not start flashlight: " + e.getMessage());
                    // Fallback - try to toggle flashlight via broadcast
                    try {
                        Intent toggleIntent = new Intent("com.android.systemui.action.TOGGLE_FLASHLIGHT");
                        mContext.sendBroadcast(toggleIntent);
                    } catch (Exception ex) {
                        Log.e(TAG, "Flashlight toggle failed: " + ex.getMessage());
                    }
                }
            });
        }
        
        if (mCameraButton != null) {
            mCameraButton.setOnClickListener(v -> {
                try {
                    Intent cameraIntent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE");
                    cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mContext.startActivity(cameraIntent);
                } catch (Exception e) {
                    Log.w(TAG, "Could not start camera: " + e.getMessage());
                    // Fallback to regular camera intent
                    try {
                        Intent fallbackIntent = new Intent("android.media.action.IMAGE_CAPTURE");
                        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(fallbackIntent);
                    } catch (Exception ex) {
                        Log.e(TAG, "Camera launch failed: " + ex.getMessage());
                    }
                }
            });
        }
    }
    
    private void updateVisibility() {
        if (!mOnePlusStyleEnabled) {
            // Hide all OnePlus components if disabled
            hideAllComponents();
            return;
        }
        
        // Show/hide date
        if (mDateView != null) {
            mDateView.setVisibility(mShowDate ? View.VISIBLE : View.GONE);
        }
        
        // Show/hide weather and separator
        if (mWeatherView != null && mSeparatorView != null) {
            if (mShowWeather && mShowDate) {
                mWeatherView.setVisibility(View.VISIBLE);
                mSeparatorView.setVisibility(View.VISIBLE);
            } else if (mShowWeather && !mShowDate) {
                mWeatherView.setVisibility(View.VISIBLE);
                mSeparatorView.setVisibility(View.GONE);
            } else {
                mWeatherView.setVisibility(View.GONE);
                mSeparatorView.setVisibility(View.GONE);
            }
        }
        
        // Show/hide battery
        if (mBatteryView != null) {
            mBatteryView.setVisibility(mShowBattery ? View.VISIBLE : View.GONE);
        }
        
        // Show/hide notifications and info separator
        if (mNotificationsView != null && mInfoSeparatorView != null) {
            if (mShowNotifications && mShowBattery) {
                mNotificationsView.setVisibility(View.VISIBLE);
                mInfoSeparatorView.setVisibility(View.VISIBLE);
            } else if (mShowNotifications && !mShowBattery) {
                mNotificationsView.setVisibility(View.VISIBLE);
                mInfoSeparatorView.setVisibility(View.GONE);
            } else {
                mNotificationsView.setVisibility(View.GONE);
                mInfoSeparatorView.setVisibility(View.GONE);
            }
        }
        
        // Hide entire containers if no content
        if (mDateWeatherContainer != null) {
            boolean hasContent = mShowDate || mShowWeather;
            mDateWeatherContainer.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        }
        
        if (mInfoContainer != null) {
            boolean hasContent = mShowBattery || mShowNotifications;
            mInfoContainer.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        }
    }
    
    private void hideAllComponents() {
        if (mDateWeatherContainer != null) mDateWeatherContainer.setVisibility(View.GONE);
        if (mInfoContainer != null) mInfoContainer.setVisibility(View.GONE);
        if (mQuickActionsContainer != null) mQuickActionsContainer.setVisibility(View.GONE);
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
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
                String dateString = dateFormat.format(new Date());
                mDateView.setText(dateString);
            } catch (Exception e) {
                Log.e(TAG, "Error updating date: " + e.getMessage());
                mDateView.setText("Today");
            }
        }
    }
    
    private void updateWeatherDisplay() {
        if (mWeatherView != null && mShowWeather) {
            // Placeholder for weather integration
            // In a real implementation, this would connect to a weather service
            mWeatherView.setText("22°C");
        }
    }
    
    private void updateBatteryDisplay() {
        if (mBatteryView != null && mShowBattery) {
            try {
                String batteryText = mBatteryLevel + "%";
                if (mBatteryCharging) {
                    batteryText += " ⚡";
                }
                mBatteryView.setText(batteryText);
            } catch (Exception e) {
                Log.e(TAG, "Error updating battery: " + e.getMessage());
                mBatteryView.setText("Battery");
            }
        }
    }
    
    private void updateNotificationDisplay() {
        if (mNotificationsView != null && mShowNotifications) {
            try {
                if (mNotificationCount > 0) {
                    mNotificationsView.setText(String.valueOf(mNotificationCount));
                    mNotificationsView.setVisibility(View.VISIBLE);
                } else {
                    mNotificationsView.setVisibility(View.GONE);
                    if (mInfoSeparatorView != null) {
                        mInfoSeparatorView.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating notifications: " + e.getMessage());
            }
        }
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
