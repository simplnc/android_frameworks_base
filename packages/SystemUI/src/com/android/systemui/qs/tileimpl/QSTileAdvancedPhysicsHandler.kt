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

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator

/**
 * Advanced spring physics handler for QS tiles
 * Provides sophisticated tactile feedback with spring physics
 */
class QSTileAdvancedPhysicsHandler(
    private val tileView: QSTileViewImpl
) {
    private var springAnimator: AnimatorSet? = null
    private var hoverAnimator: AnimatorSet? = null
    private var isPressed = false
    private var isHovered = false
    private var squishinessFraction: Float = 1f
    private var hoverFraction: Float = 1f

    fun handleTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isPressed) {
                    isPressed = true
                    animateSpringPress()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPressed) {
                    isPressed = false
                    animateSpringRelease()
                }
            }
            MotionEvent.ACTION_HOVER_ENTER -> {
                if (!isHovered) {
                    isHovered = true
                    animateHoverEnter()
                }
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                if (isHovered) {
                    isHovered = false
                    animateHoverExit()
                }
            }
        }
        return false
    }

    private fun animateSpringPress() {
        springAnimator?.cancel()

        val scaleDownX = ObjectAnimator.ofFloat(tileView, "scaleX", 1f, 0.82f)
        val scaleDownY = ObjectAnimator.ofFloat(tileView, "scaleY", 1f, 0.82f)
        val alphaDown = ObjectAnimator.ofFloat(tileView, "alpha", 1f, 0.88f)

        scaleDownX.duration = 300
        scaleDownY.duration = 300
        alphaDown.duration = 200

        scaleDownX.interpolator = OvershootInterpolator(1.2f)
        scaleDownY.interpolator = OvershootInterpolator(1.2f)
        alphaDown.interpolator = AccelerateInterpolator()

        springAnimator = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY, alphaDown)
            start()
        }
    }

    private fun animateSpringRelease() {
        springAnimator?.cancel()

        val scaleUpX = ObjectAnimator.ofFloat(tileView, "scaleX", 0.82f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(tileView, "scaleY", 0.82f, 1f)
        val alphaUp = ObjectAnimator.ofFloat(tileView, "alpha", 0.88f, 1f)
        val elevationUp = ObjectAnimator.ofFloat(tileView, "translationZ", 0f, 3f, 0f)

        scaleUpX.duration = 400
        scaleUpY.duration = 400
        alphaUp.duration = 300
        elevationUp.duration = 200

        scaleUpX.interpolator = OvershootInterpolator(1.5f)
        scaleUpY.interpolator = OvershootInterpolator(1.5f)
        alphaUp.interpolator = OvershootInterpolator(1.2f)
        elevationUp.interpolator = OvershootInterpolator(1.8f)

        springAnimator = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY, alphaUp, elevationUp)
            start()
        }
    }

    private fun animateHoverEnter() {
        hoverAnimator?.cancel()

        val scaleUpX = ObjectAnimator.ofFloat(tileView, "scaleX", 1f, 1.05f)
        val scaleUpY = ObjectAnimator.ofFloat(tileView, "scaleY", 1f, 1.05f)
        val alphaDown = ObjectAnimator.ofFloat(tileView, "alpha", 1f, 0.96f)
        val elevationUp = ObjectAnimator.ofFloat(tileView, "translationZ", 0f, 4f)

        scaleUpX.duration = 250
        scaleUpY.duration = 250
        alphaDown.duration = 200
        elevationUp.duration = 200

        scaleUpX.interpolator = OvershootInterpolator(1.1f)
        scaleUpY.interpolator = OvershootInterpolator(1.1f)
        alphaDown.interpolator = DecelerateInterpolator()
        elevationUp.interpolator = DecelerateInterpolator()

        hoverAnimator = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY, alphaDown, elevationUp)
            start()
        }
    }

    private fun animateHoverExit() {
        hoverAnimator?.cancel()

        val scaleDownX = ObjectAnimator.ofFloat(tileView, "scaleX", 1.05f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(tileView, "scaleY", 1.05f, 1f)
        val alphaUp = ObjectAnimator.ofFloat(tileView, "alpha", 0.96f, 1f)
        val elevationDown = ObjectAnimator.ofFloat(tileView, "translationZ", 4f, 0f)

        scaleDownX.duration = 200
        scaleDownY.duration = 200
        alphaUp.duration = 150
        elevationDown.duration = 150

        scaleDownX.interpolator = OvershootInterpolator(1.3f)
        scaleDownY.interpolator = OvershootInterpolator(1.3f)
        alphaUp.interpolator = OvershootInterpolator(1.1f)
        elevationDown.interpolator = OvershootInterpolator(1.2f)

        hoverAnimator = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY, alphaUp, elevationDown)
            start()
        }
    }

    fun cleanup() {
        springAnimator?.cancel()
        hoverAnimator?.cancel()
        springAnimator = null
        hoverAnimator = null
    }
}
