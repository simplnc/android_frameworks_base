/*
 * Copyright (C) 2025 The LineageOS Project
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

package android.app;

import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.content.Context;
import android.os.RemoteException;

import com.android.internal.app.ILineageSandboxService;

import java.util.Collections;
import java.util.List;

/**
 * Provides access to Lineage sandbox functionality.
 * @hide
 */
@SystemService(Context.LINEAGE_SANDBOX_SERVICE)
public class LineageSandboxManager {
    private static final String TAG = "LineageSandboxManager";

    private final Context mContext;
    private final ILineageSandboxService mService;

    /**
     * @hide
     */
    public LineageSandboxManager(@NonNull Context context, @NonNull ILineageSandboxService service) {
        mContext = context;
        mService = service;
    }

    /**
     * Check if an app is currently locked.
     * @param packageName The package name to check
     * @return true if the app is locked, false otherwise
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_DEVICE_POLICY_SANDBOX)
    public boolean isAppLocked(@NonNull String packageName) {
        try {
            return mService.isAppLocked(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Lock an application.
     * @param packageName The package name to lock
     * @return true if successfully locked, false otherwise
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_DEVICE_POLICY_SANDBOX)
    public boolean lockApp(@NonNull String packageName) {
        try {
            return mService.lockApp(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Unlock an application.
     * @param packageName The package name to unlock
     * @return true if successfully unlocked, false otherwise
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_DEVICE_POLICY_SANDBOX)
    public boolean unlockApp(@NonNull String packageName) {
        try {
            return mService.unlockApp(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get list of all locked applications.
     * @return List of locked package names
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_DEVICE_POLICY_SANDBOX)
    @NonNull
    public List<String> getLockedApps() {
        try {
            List<String> result = mService.getLockedApps();
            return result != null ? result : Collections.emptyList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Clear all app locks.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_DEVICE_POLICY_SANDBOX)
    public void clearAllLocks() {
        try {
            mService.clearAllLocks();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}