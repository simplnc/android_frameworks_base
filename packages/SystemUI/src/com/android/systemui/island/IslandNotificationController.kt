/*
 * Copyright (C) 2024 LineageOS
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
package com.android.systemui.island

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import javax.inject.Inject

/**
 * Island notification controller - manages Dynamic Island notifications
 * Handles notification display, expansion, and dismissal
 */
@SysUISingleton
class IslandNotificationController @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "IslandNotificationController"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }
    
    private var mIslandView: IslandView? = null
    private var mCurrentNotification: NotificationEntry? = null
    private var mIsEnabled = false
    
    /**
     * Set the Island view reference
     */
    fun setIslandView(islandView: IslandView) {
        mIslandView = islandView
        if (DEBUG) {
            Log.d(TAG, "Island view set")
        }
    }
    
    /**
     * Show notification in island format
     */
    fun showNotification(entry: NotificationEntry) {
        if (!mIsEnabled || mIslandView == null) return
        
        mCurrentNotification = entry
        val notification = entry.sbn.notification
        
        // Create island notification data
        val icon = notification.smallIcon?.loadDrawable(context)
        val title = notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val content = notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val islandData = IslandView.IslandNotificationData(
            icon = icon,
            title = title,
            content = content,
            packageName = entry.sbn.packageName
        )
        
        mIslandView?.setNotificationData(islandData)
        mIslandView?.visibility = android.view.View.VISIBLE
        
        if (DEBUG) {
            Log.d(TAG, "Showing island notification: $title")
        }
    }
    
    /**
     * Dismiss current island notification
     */
    fun dismissNotification() {
        if (mIslandView != null) {
            mIslandView?.dismiss()
            mCurrentNotification = null
            
            if (DEBUG) {
                Log.d(TAG, "Dismissed island notification")
            }
        }
    }
    
    /**
     * Enable or disable island notifications
     */
    fun setEnabled(enabled: Boolean) {
        mIsEnabled = enabled
        if (!enabled) {
            dismissNotification()
        }
        
        if (DEBUG) {
            Log.d(TAG, "Island notifications ${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    /**
     * Check if island notifications are enabled
     */
    fun isEnabled(): Boolean {
        return mIsEnabled
    }
    
    /**
     * Get current notification
     */
    fun getCurrentNotification(): NotificationEntry? {
        return mCurrentNotification
    }
}
