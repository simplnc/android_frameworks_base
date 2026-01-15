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

package com.android.server.security;

import static android.Manifest.permission.MANAGE_DEVICE_POLICY_SANDBOX;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static android.content.Context.LINEAGE_SANDBOX_SERVICE;

import android.annotation.NonNull;
import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;

import com.android.internal.app.ILineageSandboxService;
import com.android.server.SystemService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for LineageOS sandbox functionality.
 * Provides basic app locking capabilities.
 */
public class LineageSandboxService extends SystemService {
    private static final String TAG = "LineageSandboxService";

    private final Context mContext;
    private final Set<String> mLockedApps = new ArraySet<>();

    public LineageSandboxService(@NonNull Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        publishBinderService(Context.LINEAGE_SANDBOX_SERVICE, mBinder);
    }

    private final ILineageSandboxService.Stub mBinder = new ILineageSandboxService.Stub() {
        @Override
        public boolean isAppLocked(String packageName) {
            enforceManageDevicePolicySandbox();
            synchronized (mLockedApps) {
                return mLockedApps.contains(packageName);
            }
        }

        @Override
        public boolean lockApp(String packageName) {
            enforceManageDevicePolicySandbox();
            validatePackageName(packageName);

            synchronized (mLockedApps) {
                boolean added = mLockedApps.add(packageName);
                if (added) {
                    Slog.i(TAG, "Locked app: " + packageName);
                }
                return added;
            }
        }

        @Override
        public boolean unlockApp(String packageName) {
            enforceManageDevicePolicySandbox();
            synchronized (mLockedApps) {
                boolean removed = mLockedApps.remove(packageName);
                if (removed) {
                    Slog.i(TAG, "Unlocked app: " + packageName);
                }
                return removed;
            }
        }

        @Override
        public List<String> getLockedApps() {
            enforceManageDevicePolicySandbox();
            synchronized (mLockedApps) {
                return new ArrayList<>(mLockedApps);
            }
        }

        @Override
        public void clearAllLocks() {
            enforceManageDevicePolicySandbox();
            synchronized (mLockedApps) {
                mLockedApps.clear();
                Slog.i(TAG, "Cleared all app locks");
            }
        }
    };

    private void enforceManageDevicePolicySandbox() {
        if (mContext.checkCallingOrSelfPermission(MANAGE_DEVICE_POLICY_SANDBOX)
                != PERMISSION_GRANTED) {
            throw new SecurityException("Requires MANAGE_DEVICE_POLICY_SANDBOX permission");
        }
    }

    private void validatePackageName(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }

        try {
            ApplicationInfo info = AppGlobals.getPackageManager()
                    .getApplicationInfo(packageName, 0, UserHandle.getCallingUserId());
            if (info == null) {
                throw new IllegalArgumentException("Package not found: " + packageName);
            }
        } catch (RemoteException | PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Invalid package name: " + packageName, e);
        }
    }
}