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

package com.android.systemui.statusbar.notification.row

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator

/**
 * Enhanced squishiness handler for notification rows
 * Provides tactile feedback when notifications are pressed
 */
class NotificationSquishinessHandler(
    private val notificationRow: ExpandableNotificationRow
) {
    private var squishAnimator: ValueAnimator? = null
    private var isPressed = false
    private var squishinessFraction: Float = 1f
    
    fun handleTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isPressed) {
                    isPressed = true
                    animateSquishiness(0.90f) // More squishy for notifications
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPressed) {
                    isPressed = false
                    animateSquishiness(1.0f) // Bounce back with overshoot
                }
            }
        }
        return false
    }
    
    private fun animateSquishiness(targetSquishiness: Float) {
        squishAnimator?.cancel()
        
        squishAnimator = ValueAnimator.ofFloat(squishinessFraction, targetSquishiness).apply {
            duration = if (targetSquishiness < 1.0f) 120L else 180L
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animator ->
                squishinessFraction = animator.animatedValue as Float
                updateNotificationScale()
            }
            start()
        }
    }
    
    private fun updateNotificationScale() {
        notificationRow.scaleX = squishinessFraction
        notificationRow.scaleY = squishinessFraction
    }
    
    fun cleanup() {
        squishAnimator?.cancel()
        squishAnimator = null
    }
}
