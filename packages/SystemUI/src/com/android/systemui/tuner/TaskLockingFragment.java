/*
 * Copyright (C) 2023-2024 The RisingOS Android Project
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

package com.android.systemui.tuner;

import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.recents.TaskLockingController;
import com.android.systemui.dagger.SysUISingleton;

import javax.inject.Inject;

/**
 * Settings fragment for task locking functionality.
 * Allows users to enable/disable task locking and manage locked packages.
 */
@SysUISingleton
public class TaskLockingFragment extends PreferenceFragment 
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "TaskLockingFragment";
    
    private static final String KEY_TASK_LOCKING_ENABLED = "task_locking_enabled";
    private static final String KEY_CLEAR_ALL_LOCKS = "clear_all_locks";
    
    private TaskLockingController mTaskLockingController;
    private SwitchPreference mTaskLockingEnabledPref;
    private Preference mClearAllLocksPref;
    
    @Inject
    public TaskLockingFragment(TaskLockingController taskLockingController) {
        mTaskLockingController = taskLockingController;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.task_locking_settings);
        
        mTaskLockingEnabledPref = (SwitchPreference) findPreference(KEY_TASK_LOCKING_ENABLED);
        mClearAllLocksPref = findPreference(KEY_CLEAR_ALL_LOCKS);
        
        if (mTaskLockingEnabledPref != null) {
            mTaskLockingEnabledPref.setOnPreferenceChangeListener(this);
            updateTaskLockingEnabledState();
        }
        
        if (mClearAllLocksPref != null) {
            mClearAllLocksPref.setOnPreferenceClickListener(preference -> {
                mTaskLockingController.clearAllLockedTasks();
                return true;
            });
        }
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTaskLockingEnabledPref) {
            boolean enabled = (Boolean) newValue;
            mTaskLockingController.setTaskLockingEnabled(enabled);
            Log.d(TAG, "Task locking " + (enabled ? "enabled" : "disabled"));
            return true;
        }
        return false;
    }
    
    private void updateTaskLockingEnabledState() {
        if (mTaskLockingEnabledPref != null) {
            boolean enabled = mTaskLockingController.isTaskLockingEnabled();
            mTaskLockingEnabledPref.setChecked(enabled);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateTaskLockingEnabledState();
    }
}
