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

package com.android.systemui.island

import android.content.Context
import android.provider.Settings
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal Dynamic Island controller
 * Safe implementation that won't break builds
 */
@Singleton
class IslandController @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "IslandController"
        private const val SETTING_KEY = "island_notification"
    }
    
    private var islandView: IslandView? = null
    
    /**
     * Set the island view reference
     */
    fun setIslandView(view: IslandView) {
        islandView = view
        Log.d(TAG, "Island view set")
    }
    
    /**
     * Check if Dynamic Island is enabled
     */
    fun isEnabled(): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            SETTING_KEY,
            0
        ) == 1
    }
    
    /**
     * Show notification in island
     */
    fun showNotification(title: String, text: String) {
        if (!isEnabled()) {
            Log.d(TAG, "Dynamic Island disabled")
            return
        }
        
        islandView?.showNotification(title, text)
        Log.d(TAG, "Showing notification: $title")
    }
    
    /**
     * Hide the island
     */
    fun hide() {
        islandView?.hide()
        Log.d(TAG, "Hiding island")
    }
    
    /**
     * Check if island is visible
     */
    fun isVisible(): Boolean {
        return islandView?.isVisible() ?: false
    }
}
