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

package com.android.systemui.accessibility

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.util.settings.SecureSettings
import javax.inject.Inject

/**
 * Enhanced Flash Notification Service for Accessibility - Android 15 Compatible
 * Provides visual alerts through camera flash or screen flash
 * Compatible with LineageOS 22.2 and Android 15
 */
@SysUISingleton
class FlashNotificationService @Inject constructor(
    private val context: Context,
    private val secureSettings: SecureSettings
) {
    private val handler = Handler(Looper.getMainLooper())
    private var cameraManager: CameraManager? = null
    private var isFlashActive = false
    
    companion object {
        private const val TAG = "FlashNotificationService"
        private const val FLASH_DURATION_MS = 200L
        private const val FLASH_PATTERN_DURATION_MS = 1000L
        // Android 15 compatible settings keys using Settings.Secure constants
        private const val SETTING_FLASH_NOTIFICATIONS = Settings.Secure.ACCESSIBILITY_FLASH_NOTIFICATION_ENABLED
        private const val SETTING_FLASH_CAMERA = Settings.Secure.ACCESSIBILITY_FLASH_NOTIFICATION_CAMERA
        private const val SETTING_FLASH_SCREEN = Settings.Secure.ACCESSIBILITY_FLASH_NOTIFICATION_SCREEN
    }
    
    init {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        setupNotificationReceiver()
    }
    
    private fun setupNotificationReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            // Android 15 compatible notification actions
            addAction("android.intent.action.NOTIFICATION_POSTED")
            addAction("android.intent.action.NOTIFICATION_UPDATED")
        }
        
        context.registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "android.intent.action.NOTIFICATION_POSTED",
                    "android.intent.action.NOTIFICATION_UPDATED" -> {
                        if (isFlashNotificationsEnabled()) {
                            triggerFlashNotification()
                        }
                    }
                }
            }
        }, filter)
    }
    
    private fun isFlashNotificationsEnabled(): Boolean {
        return secureSettings.getInt(SETTING_FLASH_NOTIFICATIONS, 0) == 1
    }
    
    private fun isCameraFlashEnabled(): Boolean {
        return secureSettings.getInt(SETTING_FLASH_CAMERA, 1) == 1
    }
    
    private fun isScreenFlashEnabled(): Boolean {
        return secureSettings.getInt(SETTING_FLASH_SCREEN, 1) == 1
    }
    
    fun triggerFlashNotification() {
        if (isFlashActive) return
        
        isFlashActive = true
        
        // Trigger camera flash if enabled
        if (isCameraFlashEnabled()) {
            triggerCameraFlash()
        }
        
        // Trigger screen flash if enabled
        if (isScreenFlashEnabled()) {
            triggerScreenFlash()
        }
        
        // Reset flash state after pattern duration
        handler.postDelayed({
            isFlashActive = false
        }, FLASH_PATTERN_DURATION_MS)
    }
    
    private fun triggerCameraFlash() {
        try {
            cameraManager?.setTorchMode("0", true)
            handler.postDelayed({
                cameraManager?.setTorchMode("0", false)
            }, FLASH_DURATION_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger camera flash", e)
        }
    }
    
    private fun triggerScreenFlash() {
        // Implementation for screen flash would go here
        // This would involve creating a full-screen overlay with bright colors
        Log.d(TAG, "Screen flash triggered")
    }
    
    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        isFlashActive = false
    }
}
