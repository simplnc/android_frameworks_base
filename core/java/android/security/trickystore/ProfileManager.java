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

import android.util.Log;

import android.security.trickystore.SecurityAuditor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages app-specific profiles for different spoofing levels
 * @hide
 */
public final class ProfileManager {
    private static final String TAG = "TrickyStore.ProfileManager";

    // Profile types
    public static final int PROFILE_DISABLED = 0;
    public static final int PROFILE_BASIC = 1;
    public static final int PROFILE_STANDARD = 2;
    public static final int PROFILE_ADVANCED = 3;
    public static final int PROFILE_MAXIMUM = 4;

    // Profile names for UI
    public static final String[] PROFILE_NAMES = {
        "Disabled",
        "Basic",
        "Standard",
        "Advanced",
        "Maximum"
    };

    private static volatile ProfileManager sInstance;
    private final Map<String, AppProfile> mAppProfiles = new ConcurrentHashMap<>();
    private final Map<Integer, ProfileConfig> mProfileConfigs = new HashMap<>();
    private volatile boolean mLoaded = false;

    private ProfileManager() {
        initializeDefaultProfiles();
        loadProfiles();
    }

    public static ProfileManager getInstance() {
        if (sInstance == null) {
            synchronized (ProfileManager.class) {
                if (sInstance == null) {
                    sInstance = new ProfileManager();
                }
            }
        }
        return sInstance;
    }

    private void initializeDefaultProfiles() {
        // Basic profile - minimal spoofing
        mProfileConfigs.put(PROFILE_BASIC, new ProfileConfig(
            "Basic spoofing for simple apps",
            false, // no custom patches
            false, // no keybox
            false  // no dynamic keys
        ));

        // Standard profile - balanced spoofing
        mProfileConfigs.put(PROFILE_STANDARD, new ProfileConfig(
            "Standard spoofing for most apps",
            true,  // custom patches
            true,  // keybox support
            false  // no dynamic keys
        ));

        // Advanced profile - comprehensive spoofing
        mProfileConfigs.put(PROFILE_ADVANCED, new ProfileConfig(
            "Advanced spoofing with custom patches",
            true,  // custom patches
            true,  // keybox support
            true   // dynamic keys
        ));

        // Maximum profile - maximum compatibility
        mProfileConfigs.put(PROFILE_MAXIMUM, new ProfileConfig(
            "Maximum spoofing for stubborn apps",
            true,  // custom patches
            true,  // keybox support
            true   // dynamic keys
        ));
    }

    private void loadProfiles() {
        if (mLoaded) return;

        try {
            File profilesDir = new File("/data/misc/trickystore/profiles");
            if (!profilesDir.exists() || !profilesDir.isDirectory()) {
                Log.i(TAG, "Profiles directory not found, using defaults");
                mLoaded = true;
                return;
            }

            // Load app-specific profiles from files
            File[] profileFiles = profilesDir.listFiles((dir, name) ->
                name.endsWith(".profile"));

            if (profileFiles != null) {
                for (File profileFile : profileFiles) {
                    loadAppProfile(profileFile);
                }
            }

            Log.i(TAG, "Loaded " + mAppProfiles.size() + " app profiles");
            mLoaded = true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to load profiles", e);
            mLoaded = true; // Don't retry on failure
        }
    }

    private void loadAppProfile(File profileFile) {
        try {
            String packageName = profileFile.getName().replace(".profile", "");
            String content = readFileSafely(profileFile).trim();

            if (content.isEmpty()) return;

            String[] lines = content.split("\\n");
            int profileLevel = PROFILE_STANDARD;
            CustomPatchLevel customPatches = null;
            boolean useKeybox = true;

            for (String line : lines) {
                String[] parts = line.split(":", 2);
                if (parts.length != 2) continue;

                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                switch (key) {
                    case "level":
                        profileLevel = parseProfileLevel(value);
                        break;
                    case "system_patch":
                        if (customPatches == null) customPatches = new CustomPatchLevel(null, null, null);
                        customPatches.system = value;
                        break;
                    case "vendor_patch":
                        if (customPatches == null) customPatches = new CustomPatchLevel(null, null, null);
                        customPatches.vendor = value;
                        break;
                    case "boot_patch":
                        if (customPatches == null) customPatches = new CustomPatchLevel(null, null, null);
                        customPatches.boot = value;
                        break;
                    case "use_keybox":
                        useKeybox = Boolean.parseBoolean(value);
                        break;
                }
            }

            AppProfile profile = new AppProfile(packageName, profileLevel, customPatches, useKeybox);
            mAppProfiles.put(packageName, profile);
            Log.d(TAG, "Loaded profile for " + packageName + ": " + PROFILE_NAMES[profileLevel]);

            // Audit profile loading
            SecurityAuditor.getInstance().logProfileChange(packageName, "load_profile",
                "Profile loaded: " + PROFILE_NAMES[profileLevel]);

        } catch (Exception e) {
            Log.w(TAG, "Failed to load profile: " + profileFile.getName(), e);
        }
    }

    private int parseProfileLevel(String level) {
        switch (level.toLowerCase()) {
            case "disabled": return PROFILE_DISABLED;
            case "basic": return PROFILE_BASIC;
            case "standard": return PROFILE_STANDARD;
            case "advanced": return PROFILE_ADVANCED;
            case "maximum": return PROFILE_MAXIMUM;
            default:
                try {
                    int intLevel = Integer.parseInt(level);
                    return Math.max(PROFILE_DISABLED, Math.min(PROFILE_MAXIMUM, intLevel));
                } catch (NumberFormatException e) {
                    return PROFILE_STANDARD;
                }
        }
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

    /**
     * Get the profile for a specific package
     */
    public AppProfile getProfileForPackage(String packageName) {
        if (packageName == null) return null;

        AppProfile profile = mAppProfiles.get(packageName);
        if (profile != null) {
            return profile;
        }

        // Check for wildcard patterns
        for (Map.Entry<String, AppProfile> entry : mAppProfiles.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.contains("*")) {
                String regex = pattern.replace("*", ".*");
                if (packageName.matches(regex)) {
                    return entry.getValue();
                }
            }
        }

        return null; // No specific profile found
    }

    /**
     * Set a profile for a specific package
     */
    public void setProfileForPackage(String packageName, int profileLevel,
            CustomPatchLevel customPatches, boolean useKeybox) {
        if (packageName == null) return;

        AppProfile profile = new AppProfile(packageName, profileLevel, customPatches, useKeybox);
        mAppProfiles.put(packageName, profile);

        // Audit profile creation/update
        SecurityAuditor.getInstance().logProfileChange(packageName, "set_profile",
            "Profile set to: " + PROFILE_NAMES[profileLevel]);

        // Save to file
        saveProfileToFile(profile);
    }

    private void saveProfileToFile(AppProfile profile) {
        try {
            File profilesDir = new File("/data/misc/trickystore/profiles");
            if (!profilesDir.exists()) {
                profilesDir.mkdirs();
            }

            File profileFile = new File(profilesDir, profile.packageName + ".profile");
            StringBuilder content = new StringBuilder();

            content.append("level:").append(getProfileLevelName(profile.level)).append("\n");

            if (profile.customPatches != null) {
                if (profile.customPatches.system != null) {
                    content.append("system_patch:").append(profile.customPatches.system).append("\n");
                }
                if (profile.customPatches.vendor != null) {
                    content.append("vendor_patch:").append(profile.customPatches.vendor).append("\n");
                }
                if (profile.customPatches.boot != null) {
                    content.append("boot_patch:").append(profile.customPatches.boot).append("\n");
                }
            }

            content.append("use_keybox:").append(profile.useKeybox).append("\n");

            try (java.io.FileWriter writer = new java.io.FileWriter(profileFile)) {
                writer.write(content.toString());
            }

            Log.d(TAG, "Saved profile for " + profile.packageName);

        } catch (Exception e) {
            Log.e(TAG, "Failed to save profile for " + profile.packageName, e);
        }
    }

    private String getProfileLevelName(int level) {
        if (level >= 0 && level < PROFILE_NAMES.length) {
            return PROFILE_NAMES[level].toLowerCase().replace(" ", "_");
        }
        return "standard";
    }

    /**
     * Remove a profile for a package
     */
    public void removeProfileForPackage(String packageName) {
        if (packageName == null) return;

        mAppProfiles.remove(packageName);

        // Audit profile removal
        SecurityAuditor.getInstance().logProfileChange(packageName, "remove_profile",
            "Profile removed");

        // Delete profile file
        File profilesDir = new File("/data/misc/trickystore/profiles");
        File profileFile = new File(profilesDir, packageName + ".profile");
        if (profileFile.exists()) {
            profileFile.delete();
        }

        Log.d(TAG, "Removed profile for " + packageName);
    }

    /**
     * Get all configured app profiles
     */
    public Map<String, AppProfile> getAllProfiles() {
        return new HashMap<>(mAppProfiles);
    }

    /**
     * Get profile configuration for a level
     */
    public ProfileConfig getProfileConfig(int level) {
        return mProfileConfigs.get(level);
    }

    /**
     * App-specific profile configuration
     */
    public static class AppProfile {
        public final String packageName;
        public final int level;
        public final CustomPatchLevel customPatches;
        public final boolean useKeybox;

        public AppProfile(String packageName, int level, CustomPatchLevel customPatches, boolean useKeybox) {
            this.packageName = packageName;
            this.level = Math.max(PROFILE_DISABLED, Math.min(PROFILE_MAXIMUM, level));
            this.customPatches = customPatches;
            this.useKeybox = useKeybox;
        }

        @Override
        public String toString() {
            return "AppProfile{" +
                    "package='" + packageName + '\'' +
                    ", level=" + level +
                    ", customPatches=" + customPatches +
                    ", useKeybox=" + useKeybox +
                    '}';
        }
    }

    /**
     * Profile configuration template
     */
    public static class ProfileConfig {
        public final String description;
        public final boolean supportsCustomPatches;
        public final boolean supportsKeybox;
        public final boolean supportsDynamicKeys;

        public ProfileConfig(String description, boolean supportsCustomPatches,
                boolean supportsKeybox, boolean supportsDynamicKeys) {
            this.description = description;
            this.supportsCustomPatches = supportsCustomPatches;
            this.supportsKeybox = supportsKeybox;
            this.supportsDynamicKeys = supportsDynamicKeys;
        }
    }
}