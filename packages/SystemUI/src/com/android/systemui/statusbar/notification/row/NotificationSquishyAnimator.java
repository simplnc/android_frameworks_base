package com.android.systemui.statusbar.notification.row;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;

import com.android.app.animation.Interpolators;

/**
 * Handles squishy animations for notification rows
 * Provides elastic/bounce effects on touch interactions
 */
public class NotificationSquishyAnimator {
    
    private static final float PRESS_SCALE = 0.95f;
    private static final float BOUNCE_SCALE = 1.05f;
    private static final long PRESS_DURATION = 100L;
    private static final long BOUNCE_DURATION = 200L;
    private static final long RELEASE_DURATION = 150L;
    
    private final View mTargetView;
    private final Context mContext;
    
    private ObjectAnimator mPressAnimator;
    private ObjectAnimator mReleaseAnimator;
    private ObjectAnimator mBounceAnimator;
    
    private boolean mIsPressed = false;
    private boolean mIsAnimating = false;
    
    public NotificationSquishyAnimator(View targetView) {
        mTargetView = targetView;
        mContext = targetView.getContext();
    }
    
    /**
     * Handle touch down event - start press animation
     */
    public void onTouchDown() {
        if (!isSquishyAnimationsEnabled() || mIsPressed || mIsAnimating) return;
        
        mIsPressed = true;
        startPressAnimation();
    }
    
    /**
     * Handle touch up event - start release animation with bounce
     */
    public void onTouchUp() {
        if (!isSquishyAnimationsEnabled() || !mIsPressed) return;
        
        mIsPressed = false;
        startReleaseAnimation();
    }
    
    /**
     * Handle touch cancel event - reset to normal state
     */
    public void onTouchCancel() {
        if (!mIsPressed) return;
        
        mIsPressed = false;
        resetToNormalState();
    }
    
    /**
     * Check if squishy animations are enabled via settings
     */
    private boolean isSquishyAnimationsEnabled() {
        return Settings.System.getIntForUser(
                mContext.getContentResolver(),
                Settings.System.NOTIFICATION_SQUISHY_ANIMATIONS,
                1, // Default to enabled
                UserHandle.USER_CURRENT) != 0;
    }
    
    /**
     * Start press animation - scale down with elastic feel
     */
    private void startPressAnimation() {
        cancelAllAnimations();
        
        mPressAnimator = ObjectAnimator.ofFloat(mTargetView, "scaleX", 1.0f, PRESS_SCALE);
        mPressAnimator.setDuration(PRESS_DURATION);
        mPressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(mTargetView, "scaleY", 1.0f, PRESS_SCALE);
        scaleYAnimator.setDuration(PRESS_DURATION);
        scaleYAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        mPressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimating = true;
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
            }
        });
        
        mPressAnimator.start();
        scaleYAnimator.start();
    }
    
    /**
     * Start release animation - bounce back with overshoot
     */
    private void startReleaseAnimation() {
        cancelAllAnimations();
        
        // First bounce up
        mBounceAnimator = ObjectAnimator.ofFloat(mTargetView, "scaleX", PRESS_SCALE, BOUNCE_SCALE);
        mBounceAnimator.setDuration(BOUNCE_DURATION);
        mBounceAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        
        ObjectAnimator bounceYAnimator = ObjectAnimator.ofFloat(mTargetView, "scaleY", PRESS_SCALE, BOUNCE_SCALE);
        bounceYAnimator.setDuration(BOUNCE_DURATION);
        bounceYAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        
        // Then settle back to normal
        mReleaseAnimator = ObjectAnimator.ofFloat(mTargetView, "scaleX", BOUNCE_SCALE, 1.0f);
        mReleaseAnimator.setDuration(RELEASE_DURATION);
        mReleaseAnimator.setInterpolator(new BounceInterpolator());
        
        ObjectAnimator releaseYAnimator = ObjectAnimator.ofFloat(mTargetView, "scaleY", BOUNCE_SCALE, 1.0f);
        releaseYAnimator.setDuration(RELEASE_DURATION);
        releaseYAnimator.setInterpolator(new BounceInterpolator());
        
        mBounceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimating = true;
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                // Start the release animation after bounce
                mReleaseAnimator.start();
                releaseYAnimator.start();
            }
        });
        
        mReleaseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
            }
        });
        
        mBounceAnimator.start();
        bounceYAnimator.start();
    }
    
    /**
     * Reset to normal state immediately
     */
    private void resetToNormalState() {
        cancelAllAnimations();
        
        mTargetView.setScaleX(1.0f);
        mTargetView.setScaleY(1.0f);
        mIsAnimating = false;
    }
    
    /**
     * Cancel all running animations
     */
    private void cancelAllAnimations() {
        if (mPressAnimator != null) {
            mPressAnimator.cancel();
            mPressAnimator = null;
        }
        if (mReleaseAnimator != null) {
            mReleaseAnimator.cancel();
            mReleaseAnimator = null;
        }
        if (mBounceAnimator != null) {
            mBounceAnimator.cancel();
            mBounceAnimator = null;
        }
    }
    
    /**
     * Check if currently animating
     */
    public boolean isAnimating() {
        return mIsAnimating;
    }
    
    /**
     * Check if currently pressed
     */
    public boolean isPressed() {
        return mIsPressed;
    }
}
