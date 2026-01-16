/*
 * Copyright (C) 2024 The LineageOS Project
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

package com.android.systemui.statusbar.notification.lineage

import android.content.Context
import android.provider.Settings
import com.android.systemui.dagger.SysUISingleton
import javax.inject.Inject

/**
 * Settings helper for essential notifications configuration
 */
@SysUISingleton
class EssentialNotificationSettings @Inject constructor(
    private val context: Context
) {

    companion object {
        // Setting keys for essential notification configuration
        private const val ESSENTIAL_APPS_SETTING = "essential_notification_apps"
        private const val ESSENTIAL_NOTIFICATIONS_ENABLED = "essential_notifications_enabled"
    }

    /**
     * Check if essential notifications feature is enabled
     */
    fun isEssentialNotificationsEnabled(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            ESSENTIAL_NOTIFICATIONS_ENABLED,
            1 // Default to enabled
        ) == 1
    }

    /**
     * Get the list of user-configured essential apps
     */
    fun getUserEssentialApps(): Set<String> {
        val appsString = Settings.Global.getString(
            context.contentResolver,
            ESSENTIAL_APPS_SETTING
        ) ?: ""

        return if (appsString.isNotEmpty()) {
            appsString.split(",").map { it.trim() }.toSet()
        } else {
            emptySet()
        }
    }

    /**
     * Add an app to the essential apps list
     */
    fun addEssentialApp(packageName: String) {
        val currentApps = getUserEssentialApps().toMutableSet()
        currentApps.add(packageName)
        saveEssentialApps(currentApps)
    }

    /**
     * Remove an app from the essential apps list
     */
    fun removeEssentialApp(packageName: String) {
        val currentApps = getUserEssentialApps().toMutableSet()
        currentApps.remove(packageName)
        saveEssentialApps(currentApps)
    }

    /**
     * Set the essential notifications enabled state
     */
    fun setEssentialNotificationsEnabled(enabled: Boolean) {
        Settings.Global.putInt(
            context.contentResolver,
            ESSENTIAL_NOTIFICATIONS_ENABLED,
            if (enabled) 1 else 0
        )
    }

    /**
     * Save the list of essential apps
     */
    private fun saveEssentialApps(apps: Set<String>) {
        val appsString = apps.joinToString(",")
        Settings.Global.putString(
            context.contentResolver,
            ESSENTIAL_APPS_SETTING,
            appsString
        )
    }

    /**
     * Check if a specific app is marked as essential by the user
     */
    fun isAppMarkedEssential(packageName: String): Boolean {
        return packageName in getUserEssentialApps()
    }
}