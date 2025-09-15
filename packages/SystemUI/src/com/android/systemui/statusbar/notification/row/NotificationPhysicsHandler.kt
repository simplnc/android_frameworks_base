package com.android.systemui.statusbar.notification.row

import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.os.SystemProperties
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context

/**
 * Enhanced physics handler for notification rows.
 * Provides squishiness animations and elasticity when being removed.
 * Matches QS tile physics for consistent user experience.
 */
class NotificationPhysicsHandler(private val targetView: View) {

    // Physics parameters matching QS tiles for consistency
    private val pressScale: Float = 0.95f  // Slightly less aggressive than QS tiles
    private val pressAlpha: Float = 0.90f  // Subtle fade
    private val pressElevation: Float = -4f  // Moderate depression
    private val releaseDurationMs: Long = 300L  // Smooth release
    private val pressDurationMs: Long = 120L   // Quick press
    private val bounceScale: Float = 1.02f  // Subtle bounce back
    private val bounceDurationMs: Long = 200L  // Bounce animation duration
    
    // Dismissal animation parameters
    private val dismissScale: Float = 0.8f
    private val dismissAlpha: Float = 0.3f
    private val dismissDurationMs: Long = 400L
    
    private val vibrator: Vibrator? = targetView.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    
    // Track animation state
    private var isAnimating = false
    private var isPressed = false
    private var isDismissing = false

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled() || isDismissing) return false
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
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
        return false // Don't consume event, let normal touch handling continue
    }

    fun animateDismissal(onComplete: () -> Unit = {}) {
        if (isDismissing) return
        
        isDismissing = true
        isAnimating = true
        
        targetView.animate()
            .scaleX(dismissScale)
            .scaleY(dismissScale)
            .alpha(dismissAlpha)
            .translationX(targetView.width * 0.5f) // Slide out to the right
            .setDuration(dismissDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isAnimating = false
                isDismissing = false
                onComplete()
            }
            .start()
            
        // No haptic feedback for notifications to avoid interference with system feedback
        // vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun animateSwipeDismiss(direction: Float, onComplete: () -> Unit = {}) {
        if (isDismissing) return
        
        isDismissing = true
        isAnimating = true
        
        val swipeDistance = targetView.width * direction
        
        targetView.animate()
            .translationX(swipeDistance)
            .scaleX(dismissScale)
            .scaleY(dismissScale)
            .alpha(dismissAlpha)
            .setDuration(dismissDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isAnimating = false
                isDismissing = false
                onComplete()
            }
            .start()
    }

    private fun animatePress() {
        if (isAnimating) return
        
        isAnimating = true
        isPressed = true
        
        targetView.animate()
            .scaleX(pressScale)
            .scaleY(pressScale)
            .alpha(pressAlpha)
            .translationY(pressElevation)
            .setDuration(pressDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isAnimating = false
            }
            .start()
            
        // No haptic feedback for notifications to avoid interference with system feedback
        // vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun animateRelease() {
        if (isAnimating) return
        
        isAnimating = true
        isPressed = false
        
        // Enhanced release animation with bounce effect for elasticity
        targetView.animate()
            .scaleX(bounceScale)
            .scaleY(bounceScale)
            .alpha(1.0f)
            .translationY(-1.0f)  // Slight upward movement
            .setDuration(bounceDurationMs)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                // Return to normal state after bounce
                targetView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .translationY(0.0f)
                    .setDuration(releaseDurationMs - bounceDurationMs)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        isAnimating = false
                    }
                    .start()
            }
            .start()
            
        // No haptic feedback for notifications to avoid interference with system feedback
        // vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun cleanup() {
        targetView.animate().cancel()
        targetView.scaleX = 1.0f
        targetView.scaleY = 1.0f
        targetView.alpha = 1.0f
        targetView.translationX = 0.0f
        targetView.translationY = 0.0f
        targetView.elevation = 0.0f
        isAnimating = false
        isPressed = false
        isDismissing = false
    }

    private fun isEnabled(): Boolean {
        return SystemProperties.getBoolean("sysui.notification.physics", true)
    }

    companion object {
        private val ACCEL_DECEL = AccelerateDecelerateInterpolator()
        private val OVERSHOOT = OvershootInterpolator(1.2f)
    }
}
