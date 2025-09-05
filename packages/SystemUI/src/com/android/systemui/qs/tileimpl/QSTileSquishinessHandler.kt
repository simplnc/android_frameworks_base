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

package com.android.systemui.qs.tileimpl

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator

/**
 * Enhanced squishiness handler for QS tiles
 * Provides more pronounced tactile feedback when tiles are pressed
 */
class QSTileSquishinessHandler(
    private val tileView: QSTileViewImpl
) {
    private var squishAnimator: ValueAnimator? = null
    private var isPressed = false
    
    fun handleTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isPressed) {
                    isPressed = true
                    animateSquishiness(0.85f) // More pronounced squish
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
        
        squishAnimator = ValueAnimator.ofFloat(tileView.squishinessFraction, targetSquishiness).apply {
            duration = if (targetSquishiness < 1.0f) 100L else 200L
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener { animator ->
                tileView.squishinessFraction = animator.animatedValue as Float
            }
            start()
        }
    }
    
    fun cleanup() {
        squishAnimator?.cancel()
        squishAnimator = null
    }
}
