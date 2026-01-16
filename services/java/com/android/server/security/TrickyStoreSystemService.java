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

package com.android.server.security;

import android.content.Context;
import android.util.Log;

import com.android.server.SystemService;

/**
 * System service for TrickyStore functionality.
 * Provides hardware-backed key attestation spoofing for MicroG compatibility.
 *
 * @hide
 */
public class TrickyStoreSystemService extends SystemService {
    private static final String TAG = "TrickyStoreSystemService";

    public TrickyStoreSystemService(Context context) {
        super(context);
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        Log.i(TAG, "TrickyStoreSystemService created");
    }

    @Override
    public void onStart() {
        try {
            Log.i(TAG, "TrickyStoreSystemService started");
            // Initialize TrickyStore service to ensure it's ready
            android.security.trickystore.TrickyStoreService.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start TrickyStoreSystemService", e);
            // Don't throw - allow system to continue booting
        }
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            try {
                Log.i(TAG, "TrickyStoreSystemService boot completed");
            } catch (Exception e) {
                Log.w(TAG, "Error during boot completion", e);
            }
        }
    }
}