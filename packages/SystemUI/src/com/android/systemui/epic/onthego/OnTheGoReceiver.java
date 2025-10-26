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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

/**
 * Broadcast receiver to handle OnTheGo service control from Settings
 */
public class OnTheGoReceiver extends BroadcastReceiver {

    private static final String TAG = "OnTheGoReceiver";
    private static final ComponentName ONTHEGO_SERVICE = new ComponentName(
            "com.android.systemui",
            "com.android.systemui.epic.onthego.OnTheGoService");

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.e(TAG, "========================================");
        Log.e(TAG, "OnTheGoReceiver: Received broadcast!");
        Log.e(TAG, "Action: " + action);
        Log.e(TAG, "Package: " + intent.getPackage());
        Log.e(TAG, "========================================");

        if ("com.android.systemui.epic.onthego.START".equals(action)) {
            Log.e(TAG, "Processing START request...");
            startOnTheGoService(context);
        } else if ("com.android.systemui.epic.onthego.STOP".equals(action)) {
            Log.e(TAG, "Processing STOP request...");
            stopOnTheGoService(context);
        } else {
            Log.e(TAG, "Unknown action: " + action);
        }
    }

    private void startOnTheGoService(Context context) {
        try {
            Log.e(TAG, "Creating service intent to START OnTheGo");
            Intent serviceIntent = new Intent();
            serviceIntent.setComponent(ONTHEGO_SERVICE);
            serviceIntent.setAction("start");
            Log.e(TAG, "Calling startService() with component: " + ONTHEGO_SERVICE);
            ComponentName result = context.startService(serviceIntent);
            if (result != null) {
                Log.e(TAG, "✓ Successfully started OnTheGo service: " + result);
            } else {
                Log.e(TAG, "⚠️ startService returned null");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ ERROR starting OnTheGo service: " + e.getMessage(), e);
        }
    }

    private void stopOnTheGoService(Context context) {
        try {
            Log.e(TAG, "Creating service intent to STOP OnTheGo");
            Intent serviceIntent = new Intent();
            serviceIntent.setComponent(ONTHEGO_SERVICE);
            serviceIntent.setAction("stop");
            Log.e(TAG, "Calling startService() with component: " + ONTHEGO_SERVICE);
            ComponentName result = context.startService(serviceIntent);
            if (result != null) {
                Log.e(TAG, "✓ Successfully stopped OnTheGo service: " + result);
            } else {
                Log.e(TAG, "⚠️ startService returned null");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ ERROR stopping OnTheGo service: " + e.getMessage(), e);
        }
    }
}
