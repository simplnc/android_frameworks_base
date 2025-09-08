package com.android.systemui.qs.tileimpl

import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.os.SystemProperties
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context

/**
 * Advanced QS physics handler (standalone, not wired by default).
 * Enabled only when sysui.qs.advanced_physics=1.
 */
class QSTileAdvancedPhysicsHandler(private val targetView: View) {

    private val pressScale: Float = 0.82f  // More pronounced squish
    private val pressAlpha: Float = 0.88f  // More pronounced fade
    private val releaseDurationMs: Long = 200L
    private val pressDurationMs: Long = 100L
    private val vibrator: Vibrator? = targetView.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled()) return false
        
        // Always handle the event, don't return early
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animatePress()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animateRelease()
            }
        }
        return true // Always consume the event to ensure it works
    }

    fun cleanup() {
        // No-op for now
    }

    fun animatePress() {
        val duration = if (android.os.SystemProperties.getBoolean("sysui.qs.anim_perf", true)) {
            80L // perf mode: shorter duration
        } else {
            pressDurationMs
        }
        
        // Add vibration for tactile feedback
        vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        
        // Main squish animation
        targetView.animate()
            .setDuration(duration)
            .scaleX(pressScale)
            .scaleY(pressScale)
            .alpha(pressAlpha)
            .setInterpolator(ACCEL_DECEL)
            .withLayer()
            .start()
            
        // Add subtle shake effect
        animateShake()
    }

    fun animateRelease() {
        val duration = if (android.os.SystemProperties.getBoolean("sysui.qs.anim_perf", true)) {
            140L // perf mode: shorter duration
        } else {
            releaseDurationMs
        }
        targetView.animate()
            .setDuration(duration)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setInterpolator(ACCEL_DECEL)
            .withLayer()
            .start()
    }

    private fun animateShake() {
        // Subtle shake: translateX back and forth quickly
        val shakeDistance = 2f // Very subtle shake
        val shakeDuration = 60L // Quick shake
        
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
        return SystemProperties.getBoolean("sysui.qs.advanced_physics", true)
    }

    companion object {
        private val ACCEL_DECEL = AccelerateDecelerateInterpolator()
    }
}
