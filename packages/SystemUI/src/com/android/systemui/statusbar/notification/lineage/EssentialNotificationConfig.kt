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

import android.os.SystemProperties
import com.android.systemui.dagger.SysUISingleton
import javax.inject.Inject

/**
 * Configuration class for essential notifications system properties
 */
@SysUISingleton
class EssentialNotificationConfig @Inject constructor() {

    companion object {
        // System properties for advanced configuration
        private const val PROP_ESSENTIAL_ENABLED = "persist.sys.essential_notifications.enabled"
        private const val PROP_DEBUG_LOGGING = "persist.sys.essential_notifications.debug"
        private const val PROP_FORCE_ALL_SYSTEM = "persist.sys.essential_notifications.force_all_system"
    }

    /**
     * Check if essential notifications are enabled via system property
     * This overrides the settings value if set
     */
    fun isEssentialNotificationsEnabled(): Boolean? {
        return SystemProperties.getBoolean(PROP_ESSENTIAL_ENABLED, null)
    }

    /**
     * Check if debug logging is enabled
     */
    fun isDebugLoggingEnabled(): Boolean {
        return SystemProperties.getBoolean(PROP_DEBUG_LOGGING, false)
    }

    /**
     * Check if all system notifications should be forced as essential
     */
    fun shouldForceAllSystemEssential(): Boolean {
        return SystemProperties.getBoolean(PROP_FORCE_ALL_SYSTEM, false)
    }

    /**
     * Get combined enabled state (system property takes precedence)
     */
    fun getEffectiveEnabledState(defaultValue: Boolean): Boolean {
        return isEssentialNotificationsEnabled() ?: defaultValue
    }
}