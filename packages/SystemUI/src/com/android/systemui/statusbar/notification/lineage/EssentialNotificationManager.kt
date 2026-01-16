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
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.notification.stack.BUCKET_ESSENTIAL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Manages essential notifications and their lifecycle
 */
@SysUISingleton
class EssentialNotificationManager @Inject constructor(
    private val context: Context
) {
    private val essentialNotifications = ConcurrentHashMap<String, EssentialNotification>()

    /**
     * Check if a notification entry is essential
     */
    fun isEssential(entry: NotificationEntry): Boolean {
        return essentialNotifications.containsKey(entry.key)
    }

    /**
     * Mark a notification as essential
     */
    fun setEssential(entry: NotificationEntry, reason: EssentialReason = EssentialReason.USER_DESIGNATED) {
        val essential = EssentialNotification(entry, reason)
        essentialNotifications[entry.key] = essential
        entry.bucket = BUCKET_ESSENTIAL
    }

    /**
     * Remove essential status from a notification
     */
    fun removeEssential(entry: NotificationEntry) {
        essentialNotifications.remove(entry.key)
        // Reset to default bucket - this would need to be handled by the ranking system
        // For now, we'll leave it as is and let the ranking system handle it
    }

    /**
     * Get all essential notifications
     */
    fun getEssentialNotifications(): List<EssentialNotification> {
        return essentialNotifications.values.toList()
    }

    /**
     * Clear all essential notifications
     */
    fun clearAllEssential() {
        essentialNotifications.clear()
    }

    /**
     * Get the reason why a notification is essential
     */
    fun getEssentialReason(entry: NotificationEntry): EssentialReason? {
        return essentialNotifications[entry.key]?.reason
    }
}