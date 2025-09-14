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
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.res.R;
import com.android.systemui.lineage.LineageLockScreenSettings;
import lineageos.providers.LineageSettings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * OnePlus-style lock screen controller
 * Manages the large clock, date, weather, and quick actions
 */
public class OnePlusLockScreenController {
    
    private static final String TAG = "OnePlusLockScreenController";
    
    private Context mContext;
    private Handler mHandler;
    private Runnable mUpdateTimeRunnable;
    
    // UI Components
    private TextView mClockTime;
    private TextView mClockDate;
    private TextView mClockWeather;
    private TextView mBatteryText;
    private TextView mNotificationCount;
    private ImageView mFlashlightButton;
    private ImageView mCameraButton;
    
    // Settings
    private boolean mOnePlusStyleEnabled;
    private boolean mShowWeather;
    private boolean mShowBattery;
    private boolean mShowNotifications;
    
    public OnePlusLockScreenController(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        
        // Initialize settings
        mOnePlusStyleEnabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageLockScreenSettings.LOCKSCREEN_ONEPLUS_STYLE, 0) == 1;
        mShowWeather = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageLockScreenSettings.LOCKSCREEN_SHOW_WEATHER, 0) == 1;
        mShowBattery = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageLockScreenSettings.LOCKSCREEN_SHOW_BATTERY, 0) == 1;
        mShowNotifications = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageLockScreenSettings.LOCKSCREEN_SHOW_NOTIFICATIONS, 0) == 1;
    }
    
    public void initializeViews(View rootView) {
        if (!mOnePlusStyleEnabled) return;
        
        // Find UI components
        mClockTime = rootView.findViewById(R.id.oneplus_clock_time);
        mClockDate = rootView.findViewById(R.id.oneplus_clock_date);
        mClockWeather = rootView.findViewById(R.id.oneplus_clock_weather);
        mBatteryText = rootView.findViewById(R.id.oneplus_battery_text);
        mNotificationCount = rootView.findViewById(R.id.oneplus_notification_count);
        mFlashlightButton = rootView.findViewById(R.id.oneplus_flashlight_button);
        mCameraButton = rootView.findViewById(R.id.oneplus_camera_button);
        
        // Setup click listeners
        setupClickListeners();
        
        // Start time updates
        startTimeUpdates();
        
        // Update initial state
        updateWeatherVisibility();
        updateBatteryVisibility();
        updateNotificationVisibility();
    }
    
    private void setupClickListeners() {
        if (mFlashlightButton != null) {
            mFlashlightButton.setOnClickListener(v -> {
                // Launch flashlight
                Intent intent = new Intent("com.android.systemui.action.TOGGLE_FLASHLIGHT");
                mContext.sendBroadcast(intent);
            });
        }
        
        if (mCameraButton != null) {
            mCameraButton.setOnClickListener(v -> {
                // Launch camera
                Intent intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            });
        }
    }
    
    private void startTimeUpdates() {
        mUpdateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                mHandler.postDelayed(this, 1000); // Update every second
            }
        };
        mHandler.post(mUpdateTimeRunnable);
    }
    
    private void updateTime() {
        if (mClockTime == null || mClockDate == null) return;
        
        Calendar calendar = Calendar.getInstance();
        
        // Update time
        String timeFormat = DateFormat.is24HourFormat(mContext) ? "HH:mm" : "h:mm";
        String time = new SimpleDateFormat(timeFormat, Locale.getDefault())
                .format(calendar.getTime());
        mClockTime.setText(time);
        
        // Update date
        String dateFormat = "EEEE, MMMM d";
        String date = new SimpleDateFormat(dateFormat, Locale.getDefault())
                .format(calendar.getTime());
        mClockDate.setText(date);
        
        // Update weather (mock data for now)
        if (mShowWeather && mClockWeather != null) {
            mClockWeather.setText("22° Sunny");
        }
        
        // Update battery (mock data for now)
        if (mShowBattery && mBatteryText != null) {
            mBatteryText.setText("85%");
        }
        
        // Update notification count (mock data for now)
        if (mShowNotifications && mNotificationCount != null) {
            mNotificationCount.setText("3 notifications");
        }
    }
    
    private void updateWeatherVisibility() {
        if (mClockWeather != null) {
            mClockWeather.setVisibility(mShowWeather ? View.VISIBLE : View.GONE);
        }
    }
    
    private void updateBatteryVisibility() {
        View batteryContainer = mContext.findViewById(R.id.oneplus_battery_container);
        if (batteryContainer != null) {
            batteryContainer.setVisibility(mShowBattery ? View.VISIBLE : View.GONE);
        }
    }
    
    private void updateNotificationVisibility() {
        if (mNotificationCount != null) {
            mNotificationCount.setVisibility(mShowNotifications ? View.VISIBLE : View.GONE);
        }
    }
    
    public void setOnePlusStyleEnabled(boolean enabled) {
        mOnePlusStyleEnabled = enabled;
        if (mClockTime != null) {
            mClockTime.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }
    
    public void setShowWeather(boolean show) {
        mShowWeather = show;
        updateWeatherVisibility();
    }
    
    public void setShowBattery(boolean show) {
        mShowBattery = show;
        updateBatteryVisibility();
    }
    
    public void setShowNotifications(boolean show) {
        mShowNotifications = show;
        updateNotificationVisibility();
    }
    
    public void onDestroy() {
        if (mUpdateTimeRunnable != null) {
            mHandler.removeCallbacks(mUpdateTimeRunnable);
        }
    }
}
