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

import android.service.notification.StatusBarNotification
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.NotificationListener
import javax.inject.Inject

/**
 * Notification handler that integrates Dynamic Island with the existing notification system
 */
@SysUISingleton
class DynamicIslandNotificationHandler @Inject constructor(
    private val controller: DynamicIslandController
) : NotificationListener.NotificationHandler {
    
    companion object {
        private const val TAG = "DynamicIslandHandler"
    }
    
    override fun onNotificationPosted(entry: NotificationEntry) {
        Log.d(TAG, "Notification posted: ${entry.key}")
        
        // Show in Dynamic Island if eligible
        controller.showNotificationIfEligible(entry)
    }
    
    override fun onNotificationRemoved(entry: NotificationEntry, reason: Int) {
        Log.d(TAG, "Notification removed: ${entry.key}, reason: $reason")
        
        // Hide Dynamic Island if this was the current notification
        if (controller.isVisible()) {
            controller.hideNotification()
        }
    }
    
    override fun onNotificationRankingUpdate(rankingMap: android.service.notification.NotificationListenerService.RankingMap) {
        // Ranking updates don't affect Dynamic Island display
    }
}
