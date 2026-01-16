/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.security.trickystore;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.util.epic.PixelPropsUtils;

import java.io.File;
import java.util.Map;

/**
 * @hide
 */
public final class TrickyStoreService {
    private static final String TAG = "TrickyStoreService";

    private static volatile TrickyStoreService sInstance;
    private volatile CustomPatchLevel mCustomPatchLevel;
    private volatile boolean mInitialized = false;

    private TrickyStoreService() {
        initialize();
    }

    private void initialize() {
        try {
            loadCustomPatchLevel();
            mInitialized = true;
            Log.i(TAG, "TrickyStoreService initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TrickyStoreService", e);
            mInitialized = false;
        }
    }

    public static synchronized TrickyStoreService getInstance() {
        if (sInstance == null) {
            sInstance = new TrickyStoreService();
        }
        return sInstance;
    }

    public static boolean isEnabled() {
        // Check system property and ensure service is initialized
        boolean enabled = SystemProperties.getBoolean(PixelPropsUtils.ENABLE_TRICKYSTORE, false) &&
                         getInstance().mInitialized;

        // Additional safety check - only enable if we detect MicroG or signature spoofing environment
        if (enabled) {
            enabled = isSignatureSpoofingEnvironment();
        }

        return enabled;
    }

    private static boolean isSignatureSpoofingEnvironment() {
        try {
            // Check if MicroG GMS is installed and signature spoofing is available
            return SystemProperties.getBoolean("persist.sys.signature_spoofing", false) ||
                   isPackageInstalled("com.google.android.gms") ||
                   SystemProperties.getBoolean("persist.sys.pphooks.enable", false);
        } catch (Exception e) {
            Log.w(TAG, "Failed to check signature spoofing environment", e);
            return false;
        }
    }

    private static boolean isPackageInstalled(String packageName) {
        try {
            android.app.Application app = android.app.ActivityThread.currentApplication();
            if (app != null) {
                return app.getPackageManager().getPackageInfo(packageName, 0) != null;
            }
        } catch (Exception e) {
            // Package not installed or error checking
        }
        return false;
    }

    public CustomPatchLevel getCustomPatchLevel() {
        return mCustomPatchLevel;
    }

    private void loadCustomPatchLevel() {
        try {
            File securityPatchFile = new File("/data/misc/trickystore/security_patch.txt");
            if (securityPatchFile.exists() && securityPatchFile.canRead()) {
                // Use safer file reading method
                String content = readFileSafely(securityPatchFile).trim();
                if (!content.isEmpty()) {
                    String[] lines = content.split("\\n");
                    String systemLevel = null, vendorLevel = null, bootLevel = null;

                    for (String line : lines) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            String key = parts[0].trim().toLowerCase();
                            String value = parts[1].trim();
                            switch (key) {
                                case "system":
                                    systemLevel = value;
                                    break;
                                case "vendor":
                                    vendorLevel = value;
                                    break;
                                case "boot":
                                    bootLevel = value;
                                    break;
                            }
                        }
                    }

                    if (systemLevel != null || vendorLevel != null || bootLevel != null) {
                        mCustomPatchLevel = new CustomPatchLevel(systemLevel, vendorLevel, bootLevel);
                        Log.i(TAG, "Loaded custom patch levels: " + mCustomPatchLevel);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load custom patch level", e);
        }
    }

    /**
     * Get the appropriate patch level for a package based on its profile
     */
    public CustomPatchLevel getPatchLevelForPackage(String packageName) {
        ProfileManager profileManager = ProfileManager.getInstance();
        ProfileManager.AppProfile profile = profileManager.getProfileForPackage(packageName);

        if (profile != null && profile.customPatches != null) {
            // Use app-specific patch levels
            return profile.customPatches;
        }

        // Fall back to global patch levels
        return getCustomPatchLevel();
    }

    /**
     * Set a profile for a package
     */
    public void setProfileForPackage(String packageName, int profileLevel,
            CustomPatchLevel customPatches, boolean useKeybox) {
        ProfileManager.getInstance().setProfileForPackage(packageName, profileLevel,
                customPatches, useKeybox);
    }

    /**
     * Get all configured profiles
     */
    public Map<String, ProfileManager.AppProfile> getAllProfiles() {
        return ProfileManager.getInstance().getAllProfiles();
    }

    /**
     * Remove profile for a package
     */
    public void removeProfileForPackage(String packageName) {
        ProfileManager.getInstance().removeProfileForPackage(packageName);
    }

    /**
     * Set custom patch levels globally
     */
    public void setCustomPatchLevel(TrickyStoreService.CustomPatchLevel patchLevel) {
        mCustomPatchLevel = patchLevel;
        Log.i(TAG, "Global patch levels updated: " + patchLevel);
    }

    /**
     * Get profile for a package
     */
    public ProfileManager.AppProfile getProfileForPackage(String packageName) {
        return ProfileManager.getInstance().getProfileForPackage(packageName);
    }

    private String readFileSafely(File file) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
             java.io.BufferedReader reader = new java.io.BufferedReader(
                 new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
            return content.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read file: " + file.getPath(), e);
            return "";
        }
    }

    public static class CustomPatchLevel {
        public final String system;
        public final String vendor;
        public final String boot;

        public CustomPatchLevel(String system, String vendor, String boot) {
            this.system = system;
            this.vendor = vendor;
            this.boot = boot;
        }

        @Override
        public String toString() {
            return "CustomPatchLevel{system=" + system + ", vendor=" + vendor + ", boot=" + boot + "}";
        }
    }
}