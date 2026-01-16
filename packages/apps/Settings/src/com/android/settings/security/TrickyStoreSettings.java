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

package com.android.settings.security;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.util.epic.PixelPropsUtils;
import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

import android.security.trickystore.ProfileManager;
import android.security.trickystore.TrickyStoreService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Settings fragment for TrickyStore configuration
 */
public class TrickyStoreSettings extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "TrickyStoreSettings";

    // Preference keys
    private static final String KEY_TRICKYSTORE_ENABLE = "trickystore_enable";
    private static final String KEY_GLOBAL_PATCHES = "global_patches";
    private static final String KEY_APP_PROFILES = "app_profiles";
    private static final String KEY_CACHE_STATS = "cache_stats";

    private SwitchPreferenceCompat mEnablePreference;
    private Preference mGlobalPatchesPreference;
    private Preference mAppProfilesPreference;
    private Preference mCacheStatsPreference;

    private TrickyStoreService mTrickyStoreService;
    private ProfileManager mProfileManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.trickystore_settings, rootKey);

        mTrickyStoreService = TrickyStoreService.getInstance();
        mProfileManager = ProfileManager.getInstance();

        setupPreferences();
        updatePreferenceStates();
    }

    private void setupPreferences() {
        mEnablePreference = findPreference(KEY_TRICKYSTORE_ENABLE);
        mGlobalPatchesPreference = findPreference(KEY_GLOBAL_PATCHES);
        mAppProfilesPreference = findPreference(KEY_APP_PROFILES);
        mCacheStatsPreference = findPreference(KEY_CACHE_STATS);

        if (mEnablePreference != null) {
            mEnablePreference.setOnPreferenceChangeListener(this);
            mEnablePreference.setChecked(TrickyStoreService.isEnabled());
        }

        if (mGlobalPatchesPreference != null) {
            mGlobalPatchesPreference.setOnPreferenceClickListener(preference -> {
                showGlobalPatchesDialog();
                return true;
            });
        }

        if (mAppProfilesPreference != null) {
            mAppProfilesPreference.setOnPreferenceClickListener(preference -> {
                showAppProfilesDialog();
                return true;
            });
        }

        if (mCacheStatsPreference != null) {
            mCacheStatsPreference.setOnPreferenceClickListener(preference -> {
                showCacheStatsDialog();
                return true;
            });
            updateCacheStats();
        }
    }

    private void updatePreferenceStates() {
        boolean enabled = TrickyStoreService.isEnabled();

        if (mGlobalPatchesPreference != null) {
            mGlobalPatchesPreference.setEnabled(enabled);
        }
        if (mAppProfilesPreference != null) {
            mAppProfilesPreference.setEnabled(enabled);
        }
        if (mCacheStatsPreference != null) {
            mCacheStatsPreference.setEnabled(enabled);
        }
    }

    private void updateCacheStats() {
        if (mCacheStatsPreference != null) {
            try {
                android.security.trickystore.DynamicKeyManager.CacheStats stats =
                    android.security.trickystore.DynamicKeyManager.getInstance().getCacheStats();
                mCacheStatsPreference.setSummary(getString(R.string.trickystore_cache_stats_summary,
                    stats.cachedKeys));
            } catch (Exception e) {
                Log.w(TAG, "Failed to get cache stats", e);
                mCacheStatsPreference.setSummary(R.string.trickystore_cache_stats_error);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_TRICKYSTORE_ENABLE.equals(preference.getKey())) {
            boolean enabled = (Boolean) newValue;
            SystemProperties.set(PixelPropsUtils.ENABLE_TRICKYSTORE, enabled ? "true" : "false");

            // Clear dynamic keys when disabling
            if (!enabled) {
                try {
                    android.security.trickystore.DynamicKeyManager.getInstance().clearAllKeys();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to clear keys on disable", e);
                }
            }

            updatePreferenceStates();
            updateCacheStats();

            Toast.makeText(getContext(),
                enabled ? R.string.trickystore_enabled : R.string.trickystore_disabled,
                Toast.LENGTH_SHORT).show();

            return true;
        }
        return false;
    }

    private void showGlobalPatchesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.trickystore_global_patches_title);

        View dialogView = LayoutInflater.from(getContext()).inflate(
            R.layout.trickystore_global_patches_dialog, null);

        EditText systemPatchEdit = dialogView.findViewById(R.id.system_patch);
        EditText vendorPatchEdit = dialogView.findViewById(R.id.vendor_patch);
        EditText bootPatchEdit = dialogView.findViewById(R.id.boot_patch);

        // Load current values
        TrickyStoreService.CustomPatchLevel currentLevel = mTrickyStoreService.getCustomPatchLevel();
        if (currentLevel != null) {
            if (currentLevel.system != null) systemPatchEdit.setText(currentLevel.system);
            if (currentLevel.vendor != null) vendorPatchEdit.setText(currentLevel.vendor);
            if (currentLevel.boot != null) bootPatchEdit.setText(currentLevel.boot);
        }

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String systemPatch = systemPatchEdit.getText().toString().trim();
            String vendorPatch = vendorPatchEdit.getText().toString().trim();
            String bootPatch = bootPatchEdit.getText().toString().trim();

            if (systemPatch.isEmpty() && vendorPatch.isEmpty() && bootPatch.isEmpty()) {
                mTrickyStoreService.setCustomPatchLevel(null);
            } else {
                TrickyStoreService.CustomPatchLevel level = new TrickyStoreService.CustomPatchLevel(
                    systemPatch.isEmpty() ? null : systemPatch,
                    vendorPatch.isEmpty() ? null : vendorPatch,
                    bootPatch.isEmpty() ? null : bootPatch
                );
                mTrickyStoreService.setCustomPatchLevel(level);
            }

            Toast.makeText(getContext(), R.string.trickystore_patches_saved,
                Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showAppProfilesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.trickystore_app_profiles_title);

        View dialogView = LayoutInflater.from(getContext()).inflate(
            R.layout.trickystore_app_profiles_dialog, null);

        ListView profilesList = dialogView.findViewById(R.id.profiles_list);
        Button addProfileButton = dialogView.findViewById(R.id.add_profile_button);

        List<String> profileItems = new ArrayList<>();
        Map<String, ProfileManager.AppProfile> profiles = mProfileManager.getAllProfiles();

        if (profiles.isEmpty()) {
            profileItems.add(getString(R.string.trickystore_no_profiles));
        } else {
            for (Map.Entry<String, ProfileManager.AppProfile> entry : profiles.entrySet()) {
                ProfileManager.AppProfile profile = entry.getValue();
                profileItems.add(entry.getKey() + " - " +
                    ProfileManager.PROFILE_NAMES[profile.level]);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
            android.R.layout.simple_list_item_1, profileItems);
        profilesList.setAdapter(adapter);

        profilesList.setOnItemClickListener((parent, view, position, id) -> {
            if (profiles.isEmpty()) {
                showAddProfileDialog();
            } else {
                String packageName = new ArrayList<>(profiles.keySet()).get(position);
                showEditProfileDialog(packageName, profiles.get(packageName));
            }
        });

        addProfileButton.setOnClickListener(v -> showAddProfileDialog());

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.done, null);
        builder.show();
    }

    private void showAddProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.trickystore_add_profile_title);

        View dialogView = LayoutInflater.from(getContext()).inflate(
            R.layout.trickystore_add_profile_dialog, null);

        EditText packageEdit = dialogView.findViewById(R.id.package_name);
        Spinner profileSpinner = dialogView.findViewById(R.id.profile_level);
        Button browseAppsButton = dialogView.findViewById(R.id.browse_apps);

        // Setup profile level spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
            android.R.layout.simple_spinner_item, ProfileManager.PROFILE_NAMES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(spinnerAdapter);

        browseAppsButton.setOnClickListener(v -> showAppBrowserDialog(packageEdit));

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.add, (dialog, which) -> {
            String packageName = packageEdit.getText().toString().trim();
            int profileLevel = profileSpinner.getSelectedItemPosition();

            if (!packageName.isEmpty()) {
                mTrickyStoreService.setProfileForPackage(packageName, profileLevel, null, true);
                Toast.makeText(getContext(), R.string.trickystore_profile_added,
                    Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showEditProfileDialog(String packageName, ProfileManager.AppProfile profile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.trickystore_edit_profile_title, packageName));

        View dialogView = LayoutInflater.from(getContext()).inflate(
            R.layout.trickystore_edit_profile_dialog, null);

        Spinner profileSpinner = dialogView.findViewById(R.id.profile_level);
        Switch useKeyboxSwitch = dialogView.findViewById(R.id.use_keybox);
        Button deleteButton = dialogView.findViewById(R.id.delete_profile);

        // Setup profile level spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
            android.R.layout.simple_spinner_item, ProfileManager.PROFILE_NAMES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(spinnerAdapter);
        profileSpinner.setSelection(profile.level);

        useKeyboxSwitch.setChecked(profile.useKeybox);

        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                .setTitle(R.string.trickystore_delete_profile_title)
                .setMessage(getString(R.string.trickystore_delete_profile_message, packageName))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    mTrickyStoreService.removeProfileForPackage(packageName);
                    Toast.makeText(getContext(), R.string.trickystore_profile_deleted,
                        Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        });

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            int newLevel = profileSpinner.getSelectedItemPosition();
            boolean newUseKeybox = useKeyboxSwitch.isChecked();

            mTrickyStoreService.setProfileForPackage(packageName, newLevel, null, newUseKeybox);
            Toast.makeText(getContext(), R.string.trickystore_profile_updated,
                Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showAppBrowserDialog(EditText packageEdit) {
        PackageManager pm = getContext().getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        List<String> appNames = new ArrayList<>();
        List<String> packageNames = new ArrayList<>();

        for (PackageInfo pkg : packages) {
            if ((pkg.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                // User apps only
                String appName = pkg.applicationInfo.loadLabel(pm).toString();
                appNames.add(appName + " (" + pkg.packageName + ")");
                packageNames.add(pkg.packageName);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.trickystore_select_app);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
            android.R.layout.simple_list_item_1, appNames);

        builder.setAdapter(adapter, (dialog, which) -> {
            packageEdit.setText(packageNames.get(which));
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showCacheStatsDialog() {
        try {
            android.security.trickystore.DynamicKeyManager.CacheStats stats =
                android.security.trickystore.DynamicKeyManager.getInstance().getCacheStats();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.trickystore_cache_stats_title);
            builder.setMessage(getString(R.string.trickystore_cache_stats_details,
                stats.cachedKeys));
            builder.setPositiveButton(R.string.trickystore_clear_cache, (dialog, which) -> {
                android.security.trickystore.DynamicKeyManager.getInstance().clearAllKeys();
                updateCacheStats();
                Toast.makeText(getContext(), R.string.trickystore_cache_cleared,
                    Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton(R.string.done, null);
            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to show cache stats", e);
            Toast.makeText(getContext(), R.string.trickystore_cache_stats_error,
                Toast.LENGTH_SHORT).show();
        }
    }
}