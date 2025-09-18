/*
 * Copyright (C) 2025 The LineageOS Project
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

package com.android.systemui.statusbar.notification.row;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.content.Context;
import android.util.Log;

/**
 * Notification card physics handler for squishiness animations.
 * Provides subtle squishiness effects for notification cards when touched.
 */
public class NotificationPhysicsHandler {
    
    private static final String TAG = "NotificationPhysicsHandler";
    
    // Subtle squishiness parameters for notification cards
    private final float pressScale = 0.96f;  // Gentle squishiness
    private final float pressAlpha = 0.95f;  // Subtle fade
    private final float pressElevation = -2f;  // Light depression
    private final long releaseDurationMs = 200L;  // Quick release
    private final long pressDurationMs = 60L;   // Fast press
    private final float bounceScale = 1.02f;  // Subtle bounce
    private final long bounceDurationMs = 120L;  // Quick bounce
    
    // Shadow parameters for depth
    private final float normalElevation = 4f;  // Base shadow elevation
    private final float pressElevationShadow = 2f;  // Reduced shadow when pressed
    private final float bounceElevation = 6f;  // Enhanced shadow during bounce
    
    private final View targetView;
    private final Vibrator vibrator;
    
    // Track animation state to prevent conflicts
    private boolean isAnimating = false;
    private boolean isPressed = false;
    
    public NotificationPhysicsHandler(View view) {
        this.targetView = view;
        this.vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        
        // Initialize with normal shadow elevation
        targetView.setElevation(normalElevation);
    }
    
    public boolean handleTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!isAnimating) {
                    animatePress();
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isPressed && !isAnimating) {
                    animateRelease();
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    private void animatePress() {
        if (isAnimating) return;
        
        isAnimating = true;
        isPressed = true;
        
        long duration = SystemProperties.getBoolean("sysui.notification.physics", true) ? 
            pressDurationMs : 30L; // Performance mode: shorter duration
        
        // Gentle press animation with squishiness
        targetView.animate()
            .setDuration(duration)
            .scaleX(pressScale)
            .scaleY(pressScale)
            .alpha(pressAlpha)
            .translationY(pressElevation)
            .setStartDelay(0)
            .withStartAction(() -> targetView.setElevation(pressElevationShadow))
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withLayer()
            .withEndAction(() -> {
                // Gentle bounce back to press state
                targetView.animate()
                    .setDuration(duration / 2)
                    .scaleX(pressScale)
                    .scaleY(pressScale)
                    .alpha(pressAlpha)
                    .translationY(pressElevation)
                    .withStartAction(() -> targetView.setElevation(pressElevationShadow))
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            })
            .start();
        
        // Subtle haptic feedback
        if (vibrator != null) {
            try {
                vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Exception e) {
                Log.w(TAG, "Failed to provide haptic feedback", e);
            }
        }
    }
    
    private void animateRelease() {
        if (!isPressed) return;
        
        long duration = SystemProperties.getBoolean("sysui.notification.physics", true) ? 
            releaseDurationMs : 100L; // Performance mode: shorter duration
        
        // Gentle release with subtle bounce
        targetView.animate()
            .setDuration(duration)
            .scaleX(bounceScale) // Subtle overshoot bounce
            .scaleY(bounceScale)
            .alpha(1.0f)
            .translationY(0.0f)
            .setStartDelay(0)
            .withStartAction(() -> targetView.setElevation(bounceElevation))
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withLayer()
            .withEndAction(() -> {
                // Settle back to normal size
                targetView.animate()
                    .setDuration(duration / 3)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .withStartAction(() -> targetView.setElevation(normalElevation))
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        isAnimating = false;
                        isPressed = false;
                    })
                    .start();
            })
            .start();
        
        // Release haptic feedback
        if (vibrator != null) {
            try {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Exception e) {
                Log.w(TAG, "Failed to provide haptic feedback", e);
            }
        }
    }
    
    private boolean isEnabled() {
        return SystemProperties.getBoolean("sysui.notification.physics", true);
    }
    
    public void onDestroy() {
        // Clean up any ongoing animations
        if (targetView != null) {
            targetView.animate().cancel();
            targetView.setScaleX(1.0f);
            targetView.setScaleY(1.0f);
            targetView.setAlpha(1.0f);
            targetView.setTranslationY(0.0f);
            targetView.setElevation(normalElevation);
        }
        isAnimating = false;
        isPressed = false;
    }
}
