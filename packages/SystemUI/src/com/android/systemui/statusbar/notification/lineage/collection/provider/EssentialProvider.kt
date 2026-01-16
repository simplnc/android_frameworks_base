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

package com.android.systemui.statusbar.notification.lineage.collection.provider

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.notification.lineage.EssentialNotificationManager
import com.android.systemui.statusbar.notification.stack.BUCKET_ESSENTIAL
import javax.inject.Inject

/**
 * Provides essential notification functionality and detection
 */
@SysUISingleton
class EssentialProvider @Inject constructor(
    private val essentialNotificationManager: EssentialNotificationManager
) {

    /**
     * Check if a notification entry is essential
     */
    fun isEssentialNotification(entry: NotificationEntry): Boolean {
        return entry.bucket == BUCKET_ESSENTIAL || essentialNotificationManager.isEssential(entry)
    }

    /**
     * Get all essential notifications
     */
    fun getEssentialNotifications() = essentialNotificationManager.getEssentialNotifications()

    /**
     * Check if there are any essential notifications
     */
    fun hasEssentialNotifications(): Boolean {
        return essentialNotificationManager.getEssentialNotifications().isNotEmpty()
    }

    /**
     * Mark a notification as essential
     */
    fun setEssential(entry: NotificationEntry) {
        essentialNotificationManager.setEssential(entry)
    }

    /**
     * Remove essential status from a notification
     */
    fun removeEssential(entry: NotificationEntry) {
        essentialNotificationManager.removeEssential(entry)
    }

    /**
     * Clear all essential notifications
     */
    fun clearAllEssential() {
        essentialNotificationManager.clearAllEssential()
    }
}