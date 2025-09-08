package com.android.systemui.qs.tileimpl

import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.os.SystemProperties
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context

/**
 * Advanced QS physics handler for individual tiles.
 * Provides per-tile squishiness animations and haptic feedback.
 * Enhanced for realistic button depression feel.
 */
class QSTileAdvancedPhysicsHandler(private val targetView: View) {

    // Enhanced depression parameters for realistic button feel
    private val pressScale: Float = 0.88f  // More pronounced depression
    private val pressAlpha: Float = 0.85f  // More noticeable fade
    private val pressElevation: Float = -4f  // Push down effect
    private val releaseDurationMs: Long = 200L
    private val pressDurationMs: Long = 100L
    private val vibrator: Vibrator? = targetView.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    
    // Track animation state to prevent conflicts
    private var isAnimating = false
    private var isPressed = false

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled()) return false
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Immediate depression for instant feedback
                immediatePress()
                if (!isAnimating) {
                    animatePress()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPressed) {
                    animateRelease()
                }
            }
        }
        return true // Always consume the event to ensure consistent behavior
    }

    fun cleanup() {
        // Cancel any running animations and reset state
        targetView.animate().cancel()
        targetView.scaleX = 1.0f
        targetView.scaleY = 1.0f
        targetView.alpha = 1.0f
        targetView.translationX = 0.0f
        targetView.translationY = 0.0f
        targetView.elevation = 0.0f
        isAnimating = false
        isPressed = false
    }
    
    /**
     * Immediate press effect for instant tactile feedback
     */
    private fun immediatePress() {
        if (isPressed) return
        
        isPressed = true
        
        // Instant depression - no animation delay
        targetView.scaleX = pressScale
        targetView.scaleY = pressScale
        targetView.alpha = pressAlpha
        targetView.translationY = pressElevation
        
        // Strong haptic feedback for immediate response
        vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun animatePress() {
        if (isAnimating) return // Prevent multiple simultaneous animations
        
        val duration = if (android.os.SystemProperties.getBoolean("sysui.qs.anim_perf", true)) {
            80L // perf mode: shorter duration
        } else {
            pressDurationMs
        }
        
        isAnimating = true
        
        // Enhanced press animation with bounce effect
        targetView.animate()
            .setDuration(duration)
            .scaleX(pressScale * 0.95f) // Extra squish for bounce
            .scaleY(pressScale * 0.95f)
            .alpha(pressAlpha * 0.9f) // Deeper fade
            .translationY(pressElevation * 1.2f) // Deeper depression
            .setInterpolator(ACCEL_DECEL)
            .withLayer()
            .withEndAction {
                // Bounce back to normal press state
                targetView.animate()
                    .setDuration(duration / 2)
                    .scaleX(pressScale)
                    .scaleY(pressScale)
                    .alpha(pressAlpha)
                    .translationY(pressElevation)
                    .setInterpolator(ACCEL_DECEL)
                    .start()
            }
            .start()
            
        // Enhanced shake effect for more tactile feel
        animateEnhancedShake()
    }

    fun animateRelease() {
        val duration = if (android.os.SystemProperties.getBoolean("sysui.qs.anim_perf", true)) {
            150L // perf mode: shorter duration
        } else {
            releaseDurationMs
        }
        
        // Enhanced release with overshoot bounce
        targetView.animate()
            .setDuration(duration)
            .scaleX(1.05f) // Overshoot bounce
            .scaleY(1.05f)
            .alpha(1.0f)
            .translationY(0.0f)
            .setInterpolator(ACCEL_DECEL)
            .withLayer()
            .withEndAction {
                // Settle back to normal size
                targetView.animate()
                    .setDuration(duration / 3)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setInterpolator(ACCEL_DECEL)
                    .withEndAction {
                        isAnimating = false
                        isPressed = false
                    }
                    .start()
            }
            .start()
            
        // Release haptic feedback
        vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun animateEnhancedShake() {
        // Enhanced shake with more pronounced movement
        val shakeDistance = 3f // More noticeable shake
        val shakeDuration = 80L // Slightly longer for better feel
        
        targetView.animate()
            .setDuration(shakeDuration / 4)
            .translationX(shakeDistance)
            .setInterpolator(ACCEL_DECEL)
            .withEndAction {
                targetView.animate()
                    .setDuration(shakeDuration / 4)
                    .translationX(-shakeDistance)
                    .setInterpolator(ACCEL_DECEL)
                    .withEndAction {
                        targetView.animate()
                            .setDuration(shakeDuration / 4)
                            .translationX(shakeDistance / 2)
                            .setInterpolator(ACCEL_DECEL)
                            .withEndAction {
                                targetView.animate()
                                    .setDuration(shakeDuration / 4)
                                    .translationX(0f)
                                    .setInterpolator(ACCEL_DECEL)
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun isEnabled(): Boolean {
        // Always enabled by default, can be disabled via sysprop
        return SystemProperties.getBoolean("sysui.qs.advanced_physics", true)
    }
    
    /**
     * Force enable physics for this tile (useful for testing or specific tiles)
     */
    fun forceEnable(): Boolean {
        return true
    }

    companion object {
        private val ACCEL_DECEL = AccelerateDecelerateInterpolator()
    }
}
