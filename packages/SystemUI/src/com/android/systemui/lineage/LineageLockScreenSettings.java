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
package com.android.systemui.lineage;

/**
 * LineageOS Lock Screen Settings Constants
 * Enhanced lock screen customization options
 */
public class LineageLockScreenSettings {
    
    // Clock Style Settings
    public static final String LOCKSCREEN_CLOCK_STYLE = "lockscreen_clock_style";
    public static final String LOCKSCREEN_SHOW_DATE = "lockscreen_show_date";
    public static final String LOCKSCREEN_DATE_FORMAT = "lockscreen_date_format";
    public static final String LOCKSCREEN_CLOCK_FONT = "lockscreen_clock_font";
    
    // Weather Settings
    public static final String LOCKSCREEN_SHOW_WEATHER = "lockscreen_show_weather";
    public static final String LOCKSCREEN_WEATHER_LOCATION = "lockscreen_weather_location";
    
    // OnePlus Style Settings
    public static final String LOCKSCREEN_ONEPLUS_STYLE = "lockscreen_oneplus_style";
    public static final String LOCKSCREEN_SHOW_BATTERY = "lockscreen_show_battery";
    public static final String LOCKSCREEN_SHOW_NOTIFICATIONS = "lockscreen_show_notifications";
    
    // Media Controls Settings
    public static final String LOCKSCREEN_MEDIA_ENHANCED = "lockscreen_media_enhanced";
    public static final String LOCKSCREEN_MEDIA_BLUR = "lockscreen_media_blur";
    
    // Quick Actions Settings
    public static final String LOCKSCREEN_QUICK_ACTIONS = "lockscreen_quick_actions";
    public static final String LOCKSCREEN_QUICK_ACTIONS_COUNT = "lockscreen_quick_actions_count";
    
    // Clock Style Values
    public static final int CLOCK_STYLE_DIGITAL = 0;
    public static final int CLOCK_STYLE_ANALOG = 1;
    public static final int CLOCK_STYLE_CUSTOM = 2;
    
    // Default Values
    public static final String DEFAULT_DATE_FORMAT = "EEE, MMM dd";
    public static final int DEFAULT_QUICK_ACTIONS_COUNT = 2;
}
