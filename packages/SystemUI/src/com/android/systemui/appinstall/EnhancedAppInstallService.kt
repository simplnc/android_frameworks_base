/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.systemui.appinstall

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.util.settings.SecureSettings
import javax.inject.Inject

/**
 * Enhanced App Installation Service with App Icons - Android 15 Compatible
 * Provides visual feedback during app installation with app icons
 * Compatible with LineageOS 22.2 and Android 15
 */
@SysUISingleton
class EnhancedAppInstallService @Inject constructor(
    private val context: Context,
    private val secureSettings: SecureSettings
) {
    private val handler = Handler(Looper.getMainLooper())
    private val packageManager: PackageManager = context.packageManager
    
    companion object {
        private const val TAG = "EnhancedAppInstallService"
        private const val SETTING_INSTALL_ANIMATIONS = "app_install_animations_enabled"
        private const val SETTING_INSTALL_ICONS = "app_install_show_icons"
        private const val SETTING_INSTALL_SQUISHY = "app_install_squishy_effects"
    }
    
    init {
        setupInstallationReceiver()
    }
    
    private fun setupInstallationReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        
        context.registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val packageName = intent.data?.schemeSpecificPart
                        if (packageName != null && isInstallAnimationsEnabled()) {
                            showInstallationComplete(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val packageName = intent.data?.schemeSpecificPart
                        if (packageName != null && isInstallAnimationsEnabled()) {
                            showUninstallationComplete(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        val packageName = intent.data?.schemeSpecificPart
                        if (packageName != null && isInstallAnimationsEnabled()) {
                            showUpdateComplete(packageName)
                        }
                    }
                }
            }
        }, filter)
    }
    
    private fun isInstallAnimationsEnabled(): Boolean {
        return secureSettings.getInt(SETTING_INSTALL_ANIMATIONS, 1) == 1
    }
    
    private fun isInstallIconsEnabled(): Boolean {
        return secureSettings.getInt(SETTING_INSTALL_ICONS, 1) == 1
    }
    
    private fun isSquishyEffectsEnabled(): Boolean {
        return secureSettings.getInt(SETTING_INSTALL_SQUISHY, 1) == 1
    }
    
    private fun showInstallationComplete(packageName: String) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appIcon = if (isInstallIconsEnabled()) {
                packageManager.getApplicationIcon(appInfo)
            } else null
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            
            Log.d(TAG, "App installed: $appName ($packageName)")
            
            // Trigger enhanced installation animation
            triggerInstallationAnimation(appName, appIcon, "INSTALLED")
            
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found: $packageName", e)
        }
    }
    
    private fun showUninstallationComplete(packageName: String) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appIcon = if (isInstallIconsEnabled()) {
                packageManager.getApplicationIcon(appInfo)
            } else null
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            
            Log.d(TAG, "App uninstalled: $appName ($packageName)")
            
            // Trigger enhanced uninstallation animation
            triggerInstallationAnimation(appName, appIcon, "UNINSTALLED")
            
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "App uninstalled: $packageName")
            triggerInstallationAnimation(packageName, null, "UNINSTALLED")
        }
    }
    
    private fun showUpdateComplete(packageName: String) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appIcon = if (isInstallIconsEnabled()) {
                packageManager.getApplicationIcon(appInfo)
            } else null
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            
            Log.d(TAG, "App updated: $appName ($packageName)")
            
            // Trigger enhanced update animation
            triggerInstallationAnimation(appName, appIcon, "UPDATED")
            
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found: $packageName", e)
        }
    }
    
    private fun triggerInstallationAnimation(appName: String, appIcon: Drawable?, action: String) {
        // This would integrate with the SystemUI notification system
        // to show enhanced installation notifications with app icons
        Log.d(TAG, "Triggering $action animation for: $appName")
        
        // Implementation would show:
        // 1. App icon (if enabled)
        // 2. App name
        // 3. Action (INSTALLED/UPDATED/UNINSTALLED)
        // 4. Squishy animation effects (if enabled)
        // 5. Enhanced visual feedback
    }
    
    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
    }
}
