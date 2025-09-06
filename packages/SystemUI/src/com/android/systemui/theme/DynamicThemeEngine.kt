package com.android.systemui.theme

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import javax.inject.Inject

/**
 * Dynamic Theme Engine for Phase 13
 * Provides advanced theme customization and dynamic theme switching capabilities
 */
@SysUISingleton
class DynamicThemeEngine @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "DynamicThemeEngine"
        
        // Theme Settings
        const val DYNAMIC_THEME_ENGINE = "dynamic_theme_engine"
        const val THEME_MODE = "theme_mode"
        const val ACCENT_COLOR = "accent_color"
        const val PRIMARY_COLOR = "primary_color"
        const val SECONDARY_COLOR = "secondary_color"
        const val BACKGROUND_COLOR = "background_color"
        const val TEXT_COLOR = "text_color"
        const val THEME_TRANSITION_SPEED = "theme_transition_speed"
        const val THEME_AUTO_SWITCH = "theme_auto_switch"
        const val THEME_SCHEDULE_START = "theme_schedule_start"
        const val THEME_SCHEDULE_END = "theme_schedule_end"
    }
    
    /**
     * Theme configuration data class
     */
    data class ThemeConfig(
        val mode: String,
        val accentColor: Int,
        val primaryColor: Int,
        val secondaryColor: Int,
        val backgroundColor: Int,
        val textColor: Int,
        val transitionSpeed: Long,
        val autoSwitch: Boolean,
        val scheduleStart: String,
        val scheduleEnd: String
    )
    
    /**
     * Theme modes
     */
    object ThemeMode {
        const val LIGHT = "light"
        const val DARK = "dark"
        const val AUTO = "auto"
        const val CUSTOM = "custom"
        const val DYNAMIC = "dynamic"
    }
    
    /**
     * Check if dynamic theme engine is enabled
     */
    fun isDynamicThemeEngineEnabled(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            DYNAMIC_THEME_ENGINE,
            0
        ) == 1
    }
    
    /**
     * Enable/Disable dynamic theme engine
     */
    fun setDynamicThemeEngineEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            DYNAMIC_THEME_ENGINE,
            if (enabled) 1 else 0
        )
        Log.d(TAG, "Dynamic theme engine ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Get current theme mode
     */
    fun getThemeMode(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            THEME_MODE
        ) ?: ThemeMode.AUTO
    }
    
    /**
     * Set theme mode
     */
    fun setThemeMode(mode: String) {
        Settings.Secure.putString(
            context.contentResolver,
            THEME_MODE,
            mode
        )
        Log.d(TAG, "Theme mode set to $mode")
    }
    
    /**
     * Get accent color
     */
    fun getAccentColor(): Int {
        return Settings.Secure.getInt(
            context.contentResolver,
            ACCENT_COLOR,
            0xFF2196F3.toInt() // Default Material Blue
        )
    }
    
    /**
     * Set accent color
     */
    fun setAccentColor(color: Int) {
        Settings.Secure.putInt(
            context.contentResolver,
            ACCENT_COLOR,
            color
        )
        Log.d(TAG, "Accent color set to ${String.format("#%08X", color)}")
    }
    
    /**
     * Get primary color
     */
    fun getPrimaryColor(): Int {
        return Settings.Secure.getInt(
            context.contentResolver,
            PRIMARY_COLOR,
            0xFF1976D2.toInt() // Default Material Blue Dark
        )
    }
    
    /**
     * Set primary color
     */
    fun setPrimaryColor(color: Int) {
        Settings.Secure.putInt(
            context.contentResolver,
            PRIMARY_COLOR,
            color
        )
        Log.d(TAG, "Primary color set to ${String.format("#%08X", color)}")
    }
    
    /**
     * Get secondary color
     */
    fun getSecondaryColor(): Int {
        return Settings.Secure.getInt(
            context.contentResolver,
            SECONDARY_COLOR,
            0xFF03DAC6.toInt() // Default Material Teal
        )
    }
    
    /**
     * Set secondary color
     */
    fun setSecondaryColor(color: Int) {
        Settings.Secure.putInt(
            context.contentResolver,
            SECONDARY_COLOR,
            color
        )
        Log.d(TAG, "Secondary color set to ${String.format("#%08X", color)}")
    }
    
    /**
     * Get background color
     */
    fun getBackgroundColor(): Int {
        return Settings.Secure.getInt(
            context.contentResolver,
            BACKGROUND_COLOR,
            0xFFF5F5F5.toInt() // Default Light Gray
        )
    }
    
    /**
     * Set background color
     */
    fun setBackgroundColor(color: Int) {
        Settings.Secure.putInt(
            context.contentResolver,
            BACKGROUND_COLOR,
            color
        )
        Log.d(TAG, "Background color set to ${String.format("#%08X", color)}")
    }
    
    /**
     * Get text color
     */
    fun getTextColor(): Int {
        return Settings.Secure.getInt(
            context.contentResolver,
            TEXT_COLOR,
            0xFF212121.toInt() // Default Dark Gray
        )
    }
    
    /**
     * Set text color
     */
    fun setTextColor(color: Int) {
        Settings.Secure.putInt(
            context.contentResolver,
            TEXT_COLOR,
            color
        )
        Log.d(TAG, "Text color set to ${String.format("#%08X", color)}")
    }
    
    /**
     * Get theme transition speed
     */
    fun getThemeTransitionSpeed(): Long {
        return Settings.Secure.getLong(
            context.contentResolver,
            THEME_TRANSITION_SPEED,
            300L // Default 300ms
        )
    }
    
    /**
     * Set theme transition speed
     */
    fun setThemeTransitionSpeed(speed: Long) {
        Settings.Secure.putLong(
            context.contentResolver,
            THEME_TRANSITION_SPEED,
            speed
        )
        Log.d(TAG, "Theme transition speed set to ${speed}ms")
    }
    
    /**
     * Check if theme auto switch is enabled
     */
    fun isThemeAutoSwitchEnabled(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            THEME_AUTO_SWITCH,
            0
        ) == 1
    }
    
    /**
     * Enable/Disable theme auto switch
     */
    fun setThemeAutoSwitchEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            THEME_AUTO_SWITCH,
            if (enabled) 1 else 0
        )
        Log.d(TAG, "Theme auto switch ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Get theme schedule start time
     */
    fun getThemeScheduleStart(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            THEME_SCHEDULE_START
        ) ?: "22:00"
    }
    
    /**
     * Set theme schedule start time
     */
    fun setThemeScheduleStart(time: String) {
        Settings.Secure.putString(
            context.contentResolver,
            THEME_SCHEDULE_START,
            time
        )
        Log.d(TAG, "Theme schedule start set to $time")
    }
    
    /**
     * Get theme schedule end time
     */
    fun getThemeScheduleEnd(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            THEME_SCHEDULE_END
        ) ?: "06:00"
    }
    
    /**
     * Set theme schedule end time
     */
    fun setThemeScheduleEnd(time: String) {
        Settings.Secure.putString(
            context.contentResolver,
            THEME_SCHEDULE_END,
            time
        )
        Log.d(TAG, "Theme schedule end set to $time")
    }
    
    /**
     * Get complete theme configuration
     */
    fun getThemeConfig(): ThemeConfig {
        return ThemeConfig(
            mode = getThemeMode(),
            accentColor = getAccentColor(),
            primaryColor = getPrimaryColor(),
            secondaryColor = getSecondaryColor(),
            backgroundColor = getBackgroundColor(),
            textColor = getTextColor(),
            transitionSpeed = getThemeTransitionSpeed(),
            autoSwitch = isThemeAutoSwitchEnabled(),
            scheduleStart = getThemeScheduleStart(),
            scheduleEnd = getThemeScheduleEnd()
        )
    }
    
    /**
     * Set complete theme configuration
     */
    fun setThemeConfig(config: ThemeConfig) {
        setThemeMode(config.mode)
        setAccentColor(config.accentColor)
        setPrimaryColor(config.primaryColor)
        setSecondaryColor(config.secondaryColor)
        setBackgroundColor(config.backgroundColor)
        setTextColor(config.textColor)
        setThemeTransitionSpeed(config.transitionSpeed)
        setThemeAutoSwitchEnabled(config.autoSwitch)
        setThemeScheduleStart(config.scheduleStart)
        setThemeScheduleEnd(config.scheduleEnd)
        
        Log.d(TAG, "Complete theme configuration applied")
    }
    
    /**
     * Apply predefined theme
     */
    fun applyPredefinedTheme(themeName: String) {
        when (themeName.lowercase()) {
            "material" -> {
                setAccentColor(0xFF2196F3.toInt())
                setPrimaryColor(0xFF1976D2.toInt())
                setSecondaryColor(0xFF03DAC6.toInt())
            }
            "dark" -> {
                setAccentColor(0xFFBB86FC.toInt())
                setPrimaryColor(0xFF121212.toInt())
                setSecondaryColor(0xFF03DAC6.toInt())
                setBackgroundColor(0xFF121212.toInt())
                setTextColor(0xFFFFFFFF.toInt())
            }
            "light" -> {
                setAccentColor(0xFF2196F3.toInt())
                setPrimaryColor(0xFF1976D2.toInt())
                setSecondaryColor(0xFF03DAC6.toInt())
                setBackgroundColor(0xFFF5F5F5.toInt())
                setTextColor(0xFF212121.toInt())
            }
            "amoled" -> {
                setAccentColor(0xFF00E676.toInt())
                setPrimaryColor(0xFF000000.toInt())
                setSecondaryColor(0xFF00E676.toInt())
                setBackgroundColor(0xFF000000.toInt())
                setTextColor(0xFFFFFFFF.toInt())
            }
        }
        Log.d(TAG, "Predefined theme '$themeName' applied")
    }
    
    /**
     * Reset theme to defaults
     */
    fun resetThemeToDefaults() {
        setThemeMode(ThemeMode.AUTO)
        setAccentColor(0xFF2196F3.toInt())
        setPrimaryColor(0xFF1976D2.toInt())
        setSecondaryColor(0xFF03DAC6.toInt())
        setBackgroundColor(0xFFF5F5F5.toInt())
        setTextColor(0xFF212121.toInt())
        setThemeTransitionSpeed(300L)
        setThemeAutoSwitchEnabled(false)
        setThemeScheduleStart("22:00")
        setThemeScheduleEnd("06:00")
        
        Log.d(TAG, "Theme reset to defaults")
    }
    
    /**
     * Get theme statistics
     */
    fun getThemeStatistics(): Map<String, Any> {
        val config = getThemeConfig()
        
        return mapOf(
            "mode" to config.mode,
            "accent_color" to String.format("#%08X", config.accentColor),
            "primary_color" to String.format("#%08X", config.primaryColor),
            "secondary_color" to String.format("#%08X", config.secondaryColor),
            "background_color" to String.format("#%08X", config.backgroundColor),
            "text_color" to String.format("#%08X", config.textColor),
            "transition_speed" to config.transitionSpeed,
            "auto_switch" to config.autoSwitch,
            "schedule_start" to config.scheduleStart,
            "schedule_end" to config.scheduleEnd
        )
    }
}
