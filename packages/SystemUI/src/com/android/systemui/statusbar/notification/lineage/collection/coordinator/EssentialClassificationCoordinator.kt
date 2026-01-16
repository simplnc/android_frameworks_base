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

package com.android.systemui.statusbar.notification.lineage.collection.coordinator

import android.app.Notification
import android.app.NotificationManager
import android.util.Log
import com.android.systemui.statusbar.notification.collection.Coordinator
import com.android.systemui.statusbar.notification.collection.ListEntry
import com.android.systemui.statusbar.notification.collection.NotifPipeline
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.notification.lineage.EssentialNotificationConfig
import com.android.systemui.statusbar.notification.lineage.EssentialNotificationSettings
import com.android.systemui.statusbar.notification.lineage.collection.provider.EssentialProvider
import javax.inject.Inject

/**
 * Coordinator that automatically classifies notifications as essential based on system criteria
 */
class EssentialClassificationCoordinator @Inject constructor(
    private val essentialProvider: EssentialProvider,
    private val essentialSettings: EssentialNotificationSettings,
    private val essentialConfig: EssentialNotificationConfig
) : Coordinator {

    companion object {
        private const val TAG = "EssentialClassification"
    }

    // System apps that should always have essential notifications
    private val systemEssentialApps = setOf(
        "com.android.phone",           // Phone calls
        "com.android.mms",             // SMS/MMS
        "com.google.android.apps.messaging", // Google Messages
        "com.android.dialer",          // Dialer
        "com.android.calendar",        // Calendar
        "com.android.alarmclock",      // Clock/Alarms
        "com.android.deskclock",       // Desk Clock
        "com.android.settings",        // Settings (system updates, etc)
        "android",                     // System notifications
        "com.android.systemui",        // System UI
    )

    // Notification categories that should be essential
    private val essentialCategories = setOf(
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_MESSAGE,
        Notification.CATEGORY_EMAIL,
        Notification.CATEGORY_EVENT,
        Notification.CATEGORY_ALARM,
        Notification.CATEGORY_REMINDER
    )

    // Notification channels that should be essential
    private val essentialChannels = setOf(
        "calls", "messages", "alerts", "alarms", "reminders"
    )

    override fun attach(pipeline: NotifPipeline) {
        pipeline.addOnBeforeTransformGroupsListener(::classifyEssentialNotifications)
    }

    private fun classifyEssentialNotifications(entries: List<ListEntry>) {
        // Check if essential notifications are enabled (system property takes precedence)
        val enabled = essentialConfig.getEffectiveEnabledState(
            essentialSettings.isEssentialNotificationsEnabled()
        )

        if (essentialConfig.isDebugLoggingEnabled()) {
            Log.d(TAG, "Essential notifications enabled: $enabled")
        }

        if (!enabled) {
            return
        }

        entries.forEach { listEntry ->
            if (listEntry is NotificationEntry) {
                if (shouldBeEssential(listEntry)) {
                    essentialProvider.setEssential(listEntry)
                    if (essentialConfig.isDebugLoggingEnabled()) {
                        Log.d(TAG, "Marked notification as essential: ${listEntry.key} from ${listEntry.sbn.packageName}")
                    }
                }
            }
        }
    }

    /**
     * Determine if a notification should be considered essential
     */
    private fun shouldBeEssential(entry: NotificationEntry): Boolean {
        val sbn = entry.sbn

        // Check if already marked as essential
        if (essentialProvider.isEssentialNotification(entry)) {
            return true
        }

        // User-configured essential apps
        if (essentialSettings.isAppMarkedEssential(sbn.packageName)) {
            return true
        }

        // Force all system apps to be essential if configured
        if (essentialConfig.shouldForceAllSystemEssential() &&
            (sbn.packageName.startsWith("com.android.") ||
             sbn.packageName.startsWith("android") ||
             sbn.packageName == "com.android.systemui")) {
            return true
        }

        // System essential apps
        if (sbn.packageName in systemEssentialApps) {
            return true
        }

        // High importance notifications
        if (entry.importance >= NotificationManager.IMPORTANCE_HIGH) {
            // Check for essential categories
            if (sbn.notification.category in essentialCategories) {
                return true
            }

            // Check for essential channel names (case insensitive)
            val channelName = entry.channel?.name?.toString()?.lowercase() ?: ""
            if (essentialChannels.any { channelName.contains(it) }) {
                return true
            }

            // System notifications
            if (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
                return true
            }
        }

        // Security/emergency notifications
        if (sbn.notification.category == Notification.CATEGORY_ERROR ||
            sbn.notification.category == Notification.CATEGORY_SYSTEM ||
            sbn.notification.flags and Notification.FLAG_INSISTENT != 0) {
            return true
        }

        return false
    }
}