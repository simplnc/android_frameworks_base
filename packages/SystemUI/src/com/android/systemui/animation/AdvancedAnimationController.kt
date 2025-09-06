package com.android.systemui.animation

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import com.android.systemui.dagger.SysUISingleton
import javax.inject.Inject

/**
 * Advanced Animation Controller for Phase 13
 * Provides enhanced animation controls and customization options
 */
@SysUISingleton
class AdvancedAnimationController @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AdvancedAnimationController"
        
        // Animation Scale Types
        const val ANIMATION_SCALE_WINDOW = "animation_scale_window"
        const val ANIMATION_SCALE_TRANSITION = "animation_scale_transition"
        const val ANIMATION_SCALE_ANIMATOR = "animation_scale_animator"
        
        // Animation Types
        const val ANIMATION_TYPE_SLIDE = "slide"
        const val ANIMATION_TYPE_FADE = "fade"
        const val ANIMATION_TYPE_SCALE = "scale"
        const val ANIMATION_TYPE_ROTATE = "rotate"
        const val ANIMATION_TYPE_BOUNCE = "bounce"
        
        // Interpolator Types
        const val INTERPOLATOR_LINEAR = "linear"
        const val INTERPOLATOR_ACCELERATE_DECELERATE = "accelerate_decelerate"
        const val INTERPOLATOR_BOUNCE = "bounce"
        const val INTERPOLATOR_OVERSHOOT = "overshoot"
        const val INTERPOLATOR_CUSTOM = "custom"
    }
    
    /**
     * Animation configuration data class
     */
    data class AnimationConfig(
        val type: String,
        val duration: Long,
        val scale: Float,
        val interpolator: String,
        val customInterpolator: Interpolator? = null
    )
    
    /**
     * Get animation scale for specific type
     */
    fun getAnimationScale(type: String): Float {
        return Settings.Global.getFloat(
            context.contentResolver,
            type,
            1.0f
        )
    }
    
    /**
     * Set animation scale for specific type
     */
    fun setAnimationScale(type: String, scale: Float) {
        Settings.Global.putFloat(
            context.contentResolver,
            type,
            scale
        )
        Log.d(TAG, "Animation scale $type set to $scale")
    }
    
    /**
     * Get all animation scales
     */
    fun getAllAnimationScales(): Map<String, Float> {
        return mapOf(
            ANIMATION_SCALE_WINDOW to getAnimationScale(ANIMATION_SCALE_WINDOW),
            ANIMATION_SCALE_TRANSITION to getAnimationScale(ANIMATION_SCALE_TRANSITION),
            ANIMATION_SCALE_ANIMATOR to getAnimationScale(ANIMATION_SCALE_ANIMATOR)
        )
    }
    
    /**
     * Set all animation scales
     */
    fun setAllAnimationScales(scale: Float) {
        setAnimationScale(ANIMATION_SCALE_WINDOW, scale)
        setAnimationScale(ANIMATION_SCALE_TRANSITION, scale)
        setAnimationScale(ANIMATION_SCALE_ANIMATOR, scale)
        Log.d(TAG, "All animation scales set to $scale")
    }
    
    /**
     * Get interpolator by type
     */
    fun getInterpolator(type: String): Interpolator {
        return when (type) {
            INTERPOLATOR_LINEAR -> LinearInterpolator()
            INTERPOLATOR_ACCELERATE_DECELERATE -> AccelerateDecelerateInterpolator()
            INTERPOLATOR_BOUNCE -> BounceInterpolator()
            INTERPOLATOR_OVERSHOOT -> OvershootInterpolator()
            else -> AccelerateDecelerateInterpolator()
        }
    }
    
    /**
     * Create custom animation configuration
     */
    fun createAnimationConfig(
        type: String,
        duration: Long = 300L,
        scale: Float = 1.0f,
        interpolator: String = INTERPOLATOR_ACCELERATE_DECELERATE
    ): AnimationConfig {
        return AnimationConfig(
            type = type,
            duration = duration,
            scale = scale,
            interpolator = interpolator
        )
    }
    
    /**
     * Apply animation configuration to animation
     */
    fun applyAnimationConfig(animation: Animation, config: AnimationConfig) {
        animation.duration = (config.duration * config.scale).toLong()
        animation.interpolator = config.customInterpolator ?: getInterpolator(config.interpolator)
        
        Log.d(TAG, "Applied animation config: type=${config.type}, duration=${animation.duration}, scale=${config.scale}")
    }
    
    /**
     * Get animation duration based on scale
     */
    fun getScaledDuration(baseDuration: Long, scale: Float): Long {
        return (baseDuration * scale).toLong()
    }
    
    /**
     * Check if animations are enabled
     */
    fun areAnimationsEnabled(): Boolean {
        val windowScale = getAnimationScale(ANIMATION_SCALE_WINDOW)
        val transitionScale = getAnimationScale(ANIMATION_SCALE_TRANSITION)
        val animatorScale = getAnimationScale(ANIMATION_SCALE_ANIMATOR)
        
        return windowScale > 0.0f || transitionScale > 0.0f || animatorScale > 0.0f
    }
    
    /**
     * Enable/Disable all animations
     */
    fun setAnimationsEnabled(enabled: Boolean) {
        val scale = if (enabled) 1.0f else 0.0f
        setAllAnimationScales(scale)
        Log.d(TAG, "Animations ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Get animation performance mode
     */
    fun getAnimationPerformanceMode(): String {
        val windowScale = getAnimationScale(ANIMATION_SCALE_WINDOW)
        val transitionScale = getAnimationScale(ANIMATION_SCALE_TRANSITION)
        val animatorScale = getAnimationScale(ANIMATION_SCALE_ANIMATOR)
        
        val avgScale = (windowScale + transitionScale + animatorScale) / 3.0f
        
        return when {
            avgScale == 0.0f -> "disabled"
            avgScale < 0.5f -> "reduced"
            avgScale < 1.0f -> "standard"
            avgScale < 1.5f -> "enhanced"
            else -> "maximum"
        }
    }
    
    /**
     * Set animation performance mode
     */
    fun setAnimationPerformanceMode(mode: String) {
        val scale = when (mode) {
            "disabled" -> 0.0f
            "reduced" -> 0.5f
            "standard" -> 1.0f
            "enhanced" -> 1.2f
            "maximum" -> 1.5f
            else -> 1.0f
        }
        
        setAllAnimationScales(scale)
        Log.d(TAG, "Animation performance mode set to $mode (scale: $scale)")
    }
    
    /**
     * Get animation statistics
     */
    fun getAnimationStatistics(): Map<String, Any> {
        val scales = getAllAnimationScales()
        val enabled = areAnimationsEnabled()
        val mode = getAnimationPerformanceMode()
        
        return mapOf(
            "scales" to scales,
            "enabled" to enabled,
            "mode" to mode,
            "window_scale" to scales[ANIMATION_SCALE_WINDOW] ?: 0.0f,
            "transition_scale" to scales[ANIMATION_SCALE_TRANSITION] ?: 0.0f,
            "animator_scale" to scales[ANIMATION_SCALE_ANIMATOR] ?: 0.0f
        )
    }
}
