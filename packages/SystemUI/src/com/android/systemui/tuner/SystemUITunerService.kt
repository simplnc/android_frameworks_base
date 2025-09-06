package com.android.systemui.tuner

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import javax.inject.Inject

/**
 * Advanced System UI Tuner Service for Phase 13
 * Provides enhanced customization options for status bar, navigation bar, and system UI elements
 */
@SysUISingleton
class SystemUITunerService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SystemUITunerService"
        
        // Status Bar Customization Options
        const val STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent"
        const val STATUS_BAR_SHOW_CLOCK_SECONDS = "status_bar_show_clock_seconds"
        const val STATUS_BAR_SHOW_NETWORK_SPEED = "status_bar_show_network_speed"
        const val STATUS_BAR_SHOW_CPU_TEMP = "status_bar_show_cpu_temp"
        
        // Navigation Bar Customization Options
        const val NAV_BAR_SHOW_BACK_BUTTON = "nav_bar_show_back_button"
        const val NAV_BAR_SHOW_HOME_BUTTON = "nav_bar_show_home_button"
        const val NAV_BAR_SHOW_RECENT_BUTTON = "nav_bar_show_recent_button"
        const val NAV_BAR_CUSTOM_HEIGHT = "nav_bar_custom_height"
        
        // Animation Controls
        const val ANIMATION_SCALE_WINDOW = "animation_scale_window"
        const val ANIMATION_SCALE_TRANSITION = "animation_scale_transition"
        const val ANIMATION_SCALE_ANIMATOR = "animation_scale_animator"
    }
    
    /**
     * Check if System UI Tuner is enabled
     */
    fun isSystemUITunerEnabled(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.SYSTEM_UI_TUNER_STATUS_BAR,
            0
        ) == 1
    }
    
    /**
     * Enable/Disable System UI Tuner
     */
    fun setSystemUITunerEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.SYSTEM_UI_TUNER_STATUS_BAR,
            if (enabled) 1 else 0
        )
        Log.d(TAG, "System UI Tuner ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Get status bar customization setting
     */
    fun getStatusBarSetting(key: String, defaultValue: Boolean = false): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            key,
            if (defaultValue) 1 else 0
        ) == 1
    }
    
    /**
     * Set status bar customization setting
     */
    fun setStatusBarSetting(key: String, value: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            key,
            if (value) 1 else 0
        )
        Log.d(TAG, "Status bar setting $key set to $value")
    }
    
    /**
     * Get navigation bar customization setting
     */
    fun getNavigationBarSetting(key: String, defaultValue: Boolean = true): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            key,
            if (defaultValue) 1 else 0
        ) == 1
    }
    
    /**
     * Set navigation bar customization setting
     */
    fun setNavigationBarSetting(key: String, value: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            key,
            if (value) 1 else 0
        )
        Log.d(TAG, "Navigation bar setting $key set to $value")
    }
    
    /**
     * Get animation scale setting
     */
    fun getAnimationScale(key: String, defaultValue: Float = 1.0f): Float {
        return Settings.Global.getFloat(
            context.contentResolver,
            key,
            defaultValue
        )
    }
    
    /**
     * Set animation scale setting
     */
    fun setAnimationScale(key: String, value: Float) {
        Settings.Global.putFloat(
            context.contentResolver,
            key,
            value
        )
        Log.d(TAG, "Animation scale $key set to $value")
    }
    
    /**
     * Get custom navigation bar height
     */
    fun getCustomNavigationBarHeight(): Int {
        return Settings.Secure.getInt(
            context.contentResolver,
            NAV_BAR_CUSTOM_HEIGHT,
            48 // Default height in dp
        )
    }
    
    /**
     * Set custom navigation bar height
     */
    fun setCustomNavigationBarHeight(height: Int) {
        Settings.Secure.putInt(
            context.contentResolver,
            NAV_BAR_CUSTOM_HEIGHT,
            height
        )
        Log.d(TAG, "Custom navigation bar height set to $height dp")
    }
    
    /**
     * Check if advanced developer options are enabled
     */
    fun isAdvancedDeveloperOptionsEnabled(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ADVANCED_DEVELOPER_OPTIONS,
            0
        ) == 1
    }
    
    /**
     * Enable/Disable advanced developer options
     */
    fun setAdvancedDeveloperOptionsEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ADVANCED_DEVELOPER_OPTIONS,
            if (enabled) 1 else 0
        )
        Log.d(TAG, "Advanced developer options ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Check if performance monitoring is enabled
     */
    fun isPerformanceMonitoringEnabled(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.PERFORMANCE_MONITORING,
            0
        ) == 1
    }
    
    /**
     * Enable/Disable performance monitoring
     */
    fun setPerformanceMonitoringEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.PERFORMANCE_MONITORING,
            if (enabled) 1 else 0
        )
        Log.d(TAG, "Performance monitoring ${if (enabled) "enabled" else "disabled"}")
    }
}
