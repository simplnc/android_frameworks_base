/*
 * Copyright (C) 2020 The Pixel Experience Project
 *               2021-2025 crDroid Android Project
 *               2024-2025 The Clover Project
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

package com.android.internal.util.clover;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
public final class AttestationHooks {

    private static final String TAG = AttestationHooks.class.getSimpleName();
    private static final String DEVICE = "ro.product.device";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Map<String, Object> propsToChangeGeneric;
    private static final Map<String, Object> propsToChangePixel9ProXL;
    private static final Map<String, Object> propsToChangePixelTablet;
    private static final Map<String, Object> propsToChangePixelXL;

    // Packages to Spoof as the most recent Pixel device
    private static final String[] packagesToChangeRecentPixel = {
            "com.google.android.aicore",
            "com.google.android.apps.aiwallpapers",
            "com.google.android.apps.bard",
            "com.google.android.apps.customization.pixel",
            "com.google.android.apps.emojiwallpaper",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.apps.photos",
            "com.google.android.apps.pixel.agent",
            "com.google.android.apps.pixel.creativeassistant",
            "com.google.android.apps.pixel.support",
            "com.google.android.apps.privacy.wildlife",
            "com.google.android.apps.wallpaper",
            "com.google.android.apps.wallpaper.pixel",
            "com.google.android.apps.weather",
            "com.google.android.googlequicksearchbox",
            "com.google.android.settings.intelligence",
            "com.google.android.wallpaper.effects",
            "com.google.pixel.livewallpaper",
            "com.netflix.mediaclient",
            "com.nhs.online.nhsonline"
    };

    static {
        propsToChangeGeneric = new HashMap<>();
        propsToChangeGeneric.put("TYPE", "user");
        propsToChangeGeneric.put("TAGS", "release-keys");
        propsToChangePixel9ProXL = new HashMap<>();
        propsToChangePixel9ProXL.put("BRAND", "google");
        propsToChangePixel9ProXL.put("MANUFACTURER", "Google");
        propsToChangePixel9ProXL.put("DEVICE", "komodo");
        propsToChangePixel9ProXL.put("PRODUCT", "komodo");
        propsToChangePixel9ProXL.put("HARDWARE", "komodo");
        propsToChangePixel9ProXL.put("MODEL", "Pixel 9 Pro XL");
        propsToChangePixel9ProXL.put("ID", "BP2A.250605.031.A2");
        propsToChangePixel9ProXL.put("FINGERPRINT", "google/komodo/komodo:16/BP2A.250605.031.A2/13578606:user/release-keys");
        propsToChangePixelTablet = new HashMap<>();
        propsToChangePixelTablet.put("BRAND", "google");
        propsToChangePixelTablet.put("MANUFACTURER", "Google");
        propsToChangePixelTablet.put("DEVICE", "tangorpro");
        propsToChangePixelTablet.put("PRODUCT", "tangorpro");
        propsToChangePixelTablet.put("HARDWARE", "tangorpro");
        propsToChangePixelTablet.put("MODEL", "Pixel Tablet");
        propsToChangePixelTablet.put("ID", "BP2A.250605.031.A2");
        propsToChangePixelTablet.put("FINGERPRINT", "google/tangorpro/tangorpro:16/BP2A.250605.031.A2/13578606:user/release-keys");
        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("HARDWARE", "marlin");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
        propsToChangePixelXL.put("ID", "QP1A.191005.007.A3");
        propsToChangePixelXL.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
    }

    public static void setProps(Context context) {
        final String packageName = context.getPackageName();
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        propsToChangeGeneric.forEach((k, v) -> setPropValue(k, v));

        if (Arrays.asList(packagesToChangeRecentPixel).contains(packageName)) {

            Map<String, Object> propsToChange = new HashMap<>();

            if (packageName.equals("com.google.android.apps.photos")) {
                propsToChange.putAll(propsToChangePixelXL);
            } else if (packageName.equals("com.google.android.settings.intelligence")) {
                setPropValue("FINGERPRINT", Build.VERSION.INCREMENTAL);
                return;
            } else {
                if (isDeviceTablet(context.getApplicationContext())) {
                    propsToChange.putAll(propsToChangePixelTablet);
                } else {
                    propsToChange.putAll(propsToChangePixel9ProXL);
                }
            }

            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
                setPropValue(key, value);
            }
        }
    }

    private static boolean isDeviceTablet(Context context) {
        if (context == null) {
            return false;
        }
        Configuration config = context.getResources().getConfiguration();
        boolean isTablet = (config.smallestScreenWidthDp >= 600);
        return isTablet;
    }

    private static void setPropValue(String key, Object value) {
        setPropValue(key, value.toString());
    }

    private static void setPropValue(String key, String value) {
        try {
            if (DEBUG) Log.d(TAG, "Defining prop " + key + " to " + value);
            Class<?> clazz = Build.class;
            if (key.startsWith("VERSION.")) {
                clazz = Build.VERSION.class;
                key = key.substring(8);
            }
            Field field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            // Determine the field type and parse the value accordingly.
            if (field.getType().equals(Integer.TYPE)) {
                field.set(null, Integer.parseInt(value));
            } else if (field.getType().equals(Long.TYPE)) {
                field.set(null, Long.parseLong(value));
            } else {
                field.set(null, value);
            }
            field.setAccessible(false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }
}