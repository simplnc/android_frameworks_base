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

package com.android.systemui.dynamicisland

import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import javax.inject.Inject

/**
 * Controller for Dynamic Island notifications that manages which notifications
 * should be displayed and handles the display logic.
 */
@SysUISingleton
class DynamicIslandController @Inject constructor(
    private val context: Context,
    private val notificationView: DynamicIslandNotificationView
) {
    companion object {
        private const val TAG = "DynamicIslandController"
        private const val MAX_DISPLAY_LENGTH = 30
    }

    /**
     * Show notification in Dynamic Island if it meets criteria
     */
    fun showNotificationIfEligible(entry: NotificationEntry) {
        val sbn = entry.sbn
        val notification = sbn.notification
        
        // Skip if already showing
        if (notificationView.isVisible()) {
            Log.d(TAG, "Dynamic Island already showing, skipping: ${sbn.key}")
            return
        }
        
        // Check if notification should be shown in Dynamic Island
        if (!shouldShowInDynamicIsland(sbn)) {
            return
        }
        
        // Get app icon
        val appIcon = try {
            context.packageManager.getApplicationIcon(sbn.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        
        // Get notification text
        val text = getNotificationText(notification)
        
        // Show in Dynamic Island
        notificationView.showNotification(
            icon = appIcon,
            text = text,
            onTap = {
                // Handle tap - could expand notification or open app
                Log.d(TAG, "Dynamic Island tapped for: ${sbn.key}")
            }
        )
    }
    
    /**
     * Determine if notification should be shown in Dynamic Island
     */
    private fun shouldShowInDynamicIsland(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        
        // Skip system notifications
        if (sbn.isSystemNotification) {
            return false
        }
        
        // Skip if no text content
        if (notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT) == null &&
            notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE) == null) {
            return false
        }
        
        // Skip ongoing notifications (like music players)
        if (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0) {
            return false
        }
        
        // Skip if notification is silent
        if (notification.flags and android.app.Notification.FLAG_NO_SILENT != 0) {
            return false
        }
        
        return true
    }
    
    /**
     * Extract text content from notification
     */
    private fun getNotificationText(notification: android.app.Notification): String {
        val title = notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)
        val text = notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
        
        return when {
            title != null && text != null -> "$title: $text"
            title != null -> title.toString()
            text != null -> text.toString()
            else -> "Notification"
        }.let { fullText ->
            if (fullText.length > MAX_DISPLAY_LENGTH) {
                fullText.substring(0, MAX_DISPLAY_LENGTH - 3) + "..."
            } else {
                fullText
            }
        }
    }
    
    /**
     * Hide current Dynamic Island notification
     */
    fun hideNotification() {
        notificationView.hideNotification()
    }
    
    /**
     * Check if Dynamic Island is currently visible
     */
    fun isVisible(): Boolean = notificationView.isVisible()
    
    /**
     * Cleanup resources
     */
    fun destroy() {
        notificationView.destroy()
    }
}
