/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.systemui.epic.onthego;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

/**
 * ContentObserver to monitor Settings.System changes and
 * trigger OnTheGo service accordingly
 */
public class OnTheGoObserver extends ContentObserver {

    private static final String TAG = "OnTheGoObserver";
    private static final ComponentName ONTHEGO_SERVICE = new ComponentName(
            "com.android.systemui",
            "com.android.systemui.epic.onthego.OnTheGoService");

    private final Context mContext;

    public OnTheGoObserver(Context context) {
        super(new Handler(Looper.getMainLooper()));
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        handleOnTheGoChange();
    }

    private void handleOnTheGoChange() {
        try {
            int enabled = Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.ON_THE_GO_ENABLED, 0);
            
            Log.d(TAG, "Settings.System.ON_THE_GO_ENABLED changed to: " + enabled);

            Intent serviceIntent = new Intent();
            serviceIntent.setComponent(ONTHEGO_SERVICE);
            
            if (enabled == 1) {
                serviceIntent.setAction("start");
                Log.d(TAG, "Starting OnTheGo service via observer");
                mContext.startService(serviceIntent);
            } else {
                serviceIntent.setAction("stop");
                Log.d(TAG, "Stopping OnTheGo service via observer");
                mContext.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in OnTheGoObserver: " + e.getMessage(), e);
        }
    }

    public void register() {
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ON_THE_GO_ENABLED),
                false, this);
        Log.d(TAG, "OnTheGoObserver registered");
    }

    public void unregister() {
        mContext.getContentResolver().unregisterContentObserver(this);
        Log.d(TAG, "OnTheGoObserver unregistered");
    }
}
