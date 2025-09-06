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

package com.android.systemui.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log

/**
 * Utility class for handling haptic feedback with intensity-based control
 * Based on RisingOS implementation for QS tile haptic feedback
 */
object VibrationUtils {
    private const val TAG = "VibrationUtils"
    
    /**
     * Trigger haptic feedback with intensity-based control
     * @param context The context to get system services
     * @param intensity The intensity level (0-3, where 0 is disabled)
     */
    fun triggerVibration(context: Context, intensity: Int) {
        if (intensity <= 0) return
        
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator == null || !vibrator.hasVibrator()) {
                Log.w(TAG, "Vibrator not available")
                return
            }
            
            val effect = when (intensity) {
                1 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                2 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                3 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                else -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            }
            
            vibrator.vibrate(effect)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger vibration", e)
        }
    }
    
    /**
     * Get haptic intensity from system settings
     * @param context The context to access settings
     * @return Intensity level (0-3)
     */
    fun getHapticIntensity(context: Context): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.QS_PANEL_TILE_HAPTIC,
                1 // Default to light haptic feedback
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get haptic intensity", e)
            1
        }
    }
}
