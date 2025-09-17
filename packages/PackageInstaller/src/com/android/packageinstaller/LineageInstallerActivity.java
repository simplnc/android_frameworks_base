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

package com.android.packageinstaller;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Helper class for LineageOS Package Installer UI enhancements
 * Handles app icon loading and display with proper error handling
 */
public class LineageInstallerActivity {
    
    private static final String TAG = "LineageInstallerActivity";
    
    /**
     * Load and display app icon from PackageInfo
     * @param context Context for accessing PackageManager
     * @param packageInfo PackageInfo of the app being installed
     * @param iconView ImageView to display the icon
     * @param appNameView TextView to display app name (optional)
     */
    public static void loadAppIcon(Context context, PackageInfo packageInfo, 
                                 ImageView iconView, TextView appNameView) {
        if (context == null || packageInfo == null || iconView == null) {
            Log.w(TAG, "Invalid parameters for loadAppIcon");
            return;
        }
        
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            
            // Load app icon
            Drawable appIcon = appInfo.loadIcon(pm);
            if (appIcon != null) {
                iconView.setImageDrawable(appIcon);
            } else {
                // Fallback to default icon
                iconView.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            
            // Load app name if TextView provided
            if (appNameView != null) {
                CharSequence appName = appInfo.loadLabel(pm);
                if (appName != null && appName.length() > 0) {
                    appNameView.setText(appName);
                } else {
                    appNameView.setText(packageInfo.packageName);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading app icon: " + e.getMessage());
            // Set fallback icon on error
            iconView.setImageResource(android.R.drawable.sym_def_app_icon);
            if (appNameView != null) {
                appNameView.setText("Unknown App");
            }
        }
    }
    
    /**
     * Load app icon from ApplicationInfo (for uninstaller)
     * @param context Context for accessing PackageManager
     * @param appInfo ApplicationInfo of the app
     * @param iconView ImageView to display the icon
     * @param appNameView TextView to display app name (optional)
     */
    public static void loadAppIcon(Context context, ApplicationInfo appInfo, 
                                 ImageView iconView, TextView appNameView) {
        if (context == null || appInfo == null || iconView == null) {
            Log.w(TAG, "Invalid parameters for loadAppIcon");
            return;
        }
        
        try {
            PackageManager pm = context.getPackageManager();
            
            // Load app icon
            Drawable appIcon = appInfo.loadIcon(pm);
            if (appIcon != null) {
                iconView.setImageDrawable(appIcon);
            } else {
                // Fallback to default icon
                iconView.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            
            // Load app name if TextView provided
            if (appNameView != null) {
                CharSequence appName = appInfo.loadLabel(pm);
                if (appName != null && appName.length() > 0) {
                    appNameView.setText(appName);
                } else {
                    appNameView.setText(appInfo.packageName);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading app icon: " + e.getMessage());
            // Set fallback icon on error
            iconView.setImageResource(android.R.drawable.sym_def_app_icon);
            if (appNameView != null) {
                appNameView.setText("Unknown App");
            }
        }
    }
    
    /**
     * Format app version for display
     * @param packageInfo PackageInfo containing version information
     * @return Formatted version string
     */
    public static String getFormattedVersion(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return "Unknown version";
        }
        
        try {
            String versionName = packageInfo.versionName;
            if (versionName != null && !versionName.isEmpty()) {
                return "Version " + versionName;
            } else {
                return "Version " + packageInfo.getLongVersionCode();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting version: " + e.getMessage());
            return "Unknown version";
        }
    }
    
    /**
     * Format app size for display
     * @param sizeBytes Size in bytes
     * @return Formatted size string
     */
    public static String getFormattedSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            return "Unknown size";
        }
        
        final String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = sizeBytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
}

