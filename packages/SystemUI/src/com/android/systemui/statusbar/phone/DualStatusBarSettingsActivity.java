/*
 * Copyright (C) 2025 Zeus-OS
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

package com.android.systemui.statusbar.phone;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import com.android.systemui.R;

/**
 * Settings activity for dual status bar configuration
 */
public class DualStatusBarSettingsActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up action bar
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(R.string.dual_status_bar_settings_title);
        
        // Load the preferences fragment
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new DualStatusBarSettingsFragment())
                .commit();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Preferences fragment for dual status bar settings
     */
    public static class DualStatusBarSettingsFragment extends PreferenceFragment {
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            // Load preferences from XML
            addPreferencesFromResource(R.xml.dual_status_bar_prefs);
        }
    }
}
