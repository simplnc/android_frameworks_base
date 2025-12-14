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

package com.android.systemui.shade

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.IdRes
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Insets
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.os.Trace
import android.os.Trace.TRACE_TAG_APP
import android.provider.AlarmClock
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.view.DisplayCutout
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.doOnLayout
import com.android.app.animation.Interpolators
import com.android.settingslib.Utils
import com.android.systemui.Dumpable
import com.android.systemui.animation.ShadeInterpolation
import com.android.systemui.battery.BatteryMeterView
import com.android.systemui.battery.BatteryMeterViewController
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.demomode.DemoMode
import com.android.systemui.demomode.DemoModeController
import com.android.systemui.dump.DumpManager
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.qs.ChipVisibilityListener
import com.android.systemui.qs.HeaderPrivacyIconsController
import com.android.systemui.res.R
import com.android.systemui.shade.ShadeHeaderController.Companion.HEADER_TRANSITION_ID
import com.android.systemui.shade.ShadeHeaderController.Companion.LARGE_SCREEN_HEADER_CONSTRAINT
import com.android.systemui.shade.ShadeHeaderController.Companion.LARGE_SCREEN_HEADER_TRANSITION_ID
import com.android.systemui.shade.ShadeHeaderController.Companion.QQS_HEADER_CONSTRAINT
import com.android.systemui.shade.ShadeHeaderController.Companion.QS_HEADER_CONSTRAINT
import com.android.systemui.shade.ShadeViewProviderModule.Companion.SHADE_HEADER
import com.android.systemui.shade.carrier.ShadeCarrierGroup
import com.android.systemui.shade.carrier.ShadeCarrierGroupController
import com.android.systemui.statusbar.data.repository.StatusBarContentInsetsProviderStore
import com.android.systemui.statusbar.phone.StatusBarLocation
import com.android.systemui.statusbar.phone.StatusIconContainer
import com.android.systemui.statusbar.phone.StatusOverlayHoverListenerFactory
import com.android.systemui.statusbar.phone.ui.StatusBarIconController
import com.android.systemui.statusbar.phone.ui.TintedIconManager
import com.android.systemui.statusbar.policy.Clock
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.NextAlarmController
import com.android.systemui.statusbar.policy.VariableDateView
import com.android.systemui.statusbar.policy.VariableDateViewController
import com.android.systemui.util.ViewController
import com.android.systemui.android.header.StatusBarHeaderMachine
import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Named

/**
 * Controller for QS header.
 *
 * [header] is a [MotionLayout] that has two transitions:
 * * [HEADER_TRANSITION_ID]: [QQS_HEADER_CONSTRAINT] <-> [QS_HEADER_CONSTRAINT] for portrait
 *   handheld device configuration.
 * * [LARGE_SCREEN_HEADER_TRANSITION_ID]: [LARGE_SCREEN_HEADER_CONSTRAINT] for all other
 *   configurations
 */
@SysUISingleton
class ShadeHeaderController
@Inject
constructor(
    @Named(SHADE_HEADER) private val header: MotionLayout,
    private val statusBarIconController: StatusBarIconController,
    private val tintedIconManagerFactory: TintedIconManager.Factory,
    private val privacyIconsController: HeaderPrivacyIconsController,
    private val insetsProviderStore: StatusBarContentInsetsProviderStore,
    @ShadeDisplayAware private val configurationController: ConfigurationController,
    private val context: Context,
    private val variableDateViewControllerFactory: VariableDateViewController.Factory,
    @Named(SHADE_HEADER) private val batteryMeterViewController: BatteryMeterViewController,
    private val dumpManager: DumpManager,
    private val shadeCarrierGroupControllerBuilder: ShadeCarrierGroupController.Builder,
    private val combinedShadeHeadersConstraintManager: CombinedShadeHeadersConstraintManager,
    private val demoModeController: DemoModeController,
    private val qsBatteryModeController: QsBatteryModeController,
    private val nextAlarmController: NextAlarmController,
    private val activityStarter: ActivityStarter,
    private val statusOverlayHoverListenerFactory: StatusOverlayHoverListenerFactory,
) : ViewController<View>(header), Dumpable {

    private val insetsProvider = insetsProviderStore.defaultDisplay

    companion object {
        /** IDs for transitions and constraints for the [MotionLayout]. */
        @VisibleForTesting internal val HEADER_TRANSITION_ID = R.id.header_transition
        @VisibleForTesting
        internal val LARGE_SCREEN_HEADER_TRANSITION_ID = R.id.large_screen_header_transition
        @VisibleForTesting internal val QQS_HEADER_CONSTRAINT = R.id.qqs_header_constraint
        @VisibleForTesting internal val QS_HEADER_CONSTRAINT = R.id.qs_header_constraint
        @VisibleForTesting
        internal val LARGE_SCREEN_HEADER_CONSTRAINT = R.id.large_screen_header_constraint

        @VisibleForTesting internal val DEFAULT_CLOCK_INTENT = Intent(AlarmClock.ACTION_SHOW_ALARMS)

      
        internal val QS_HEADER_CLOCK_STYLE =
            "system:" + "qs_header_clock_style"

        private fun Int.stateToString() =
            when (this) {
                QQS_HEADER_CONSTRAINT -> "QQS Header"
                QS_HEADER_CONSTRAINT -> "QS Header"
                LARGE_SCREEN_HEADER_CONSTRAINT -> "Large Screen Header"
                else -> "Unknown state $this"
            }
    }

    var shadeCollapseAction: Runnable? = null

   
    private var qsClockStyle = 0

    private lateinit var iconManager: TintedIconManager
    private lateinit var carrierIconSlots: List<String>
    private lateinit var mShadeCarrierGroupController: ShadeCarrierGroupController

    private val batteryIcon: BatteryMeterView = header.requireViewById(R.id.batteryRemainingIcon)
    private val clock: Clock = header.requireViewById(R.id.clock)
    private val date: TextView = header.requireViewById(R.id.date)
    private val iconContainer: StatusIconContainer = header.requireViewById(R.id.statusIcons)
    private val mShadeCarrierGroup: ShadeCarrierGroup = header.requireViewById(R.id.carrier_group)
    private val systemIconsHoverContainer: View =
        header.requireViewById(R.id.hover_system_icons_container)

    private var roundedCorners = 0
    private var cutout: DisplayCutout? = null
    private var lastInsets: WindowInsets? = null
    private var nextAlarmIntent: PendingIntent? = null
    private var textColorPrimary = Color.TRANSPARENT

    private var qsDisabled = false
    private var visible = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            updateListeners()
        }

    private var customizing = false

    // QS Header/Panel background image wiring
    private var qsHeaderImage: View? = null
    private var qsPanelBackgroundImage: View? = null
    private var headerMachine: StatusBarHeaderMachine? = null
    private var headerObserver: StatusBarHeaderMachine.IStatusBarHeaderMachineObserver? = null
    private var headerImageSettingsObserver: ContentObserver? = null
        set(value) {
            if (field != value) {
                field = value
                updateVisibility()
            }
        }

    /**
     * Whether the QQS/QS part of the shade is visible. This is particularly important in
     * Lockscreen, as the shade is visible but QS is not.
     */
    var qsVisible = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            onShadeExpandedChanged()
        }

    /**
     * Whether we are in a configuration with large screen width. In this case, the header is a
     * single line.
     */
    var largeScreenActive = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            onHeaderStateChanged()
        }

    /** Expansion fraction of the QQS/QS shade. This is not the expansion between QQS <-> QS. */
    var shadeExpandedFraction = -1f
        set(value) {
            if (qsVisible && field != value) {
                header.alpha = ShadeInterpolation.getContentAlpha(value)
                field = value
                updateIgnoredSlots()
            }
        }

    /** Expansion fraction of the QQS <-> QS animation. */
    var qsExpandedFraction = -1f
        set(value) {
            if (visible && field != value) {
                field = value
                iconContainer.setQsExpansionTransitioning(value > 0f && value < 1.0f)
                updatePosition()
                updateIgnoredSlots()
            }
        }

    /** Current scroll of QS. */
    var qsScrollY = 0
        set(value) {
            if (field != value) {
                field = value
                updateScrollY()
            }
        }

    private val insetListener =
        View.OnApplyWindowInsetsListener { view, insets ->
            updateConstraintsForInsets(view as MotionLayout, insets)
            lastInsets = WindowInsets(insets)

            view.onApplyWindowInsets(insets)
        }

    private var singleCarrier = false

    private val demoModeReceiver =
        object : DemoMode {
            override fun demoCommands() = listOf(DemoMode.COMMAND_CLOCK)

            override fun dispatchDemoCommand(command: String, args: Bundle) =
                clock.dispatchDemoCommand(command, args)

            override fun onDemoModeStarted() = clock.onDemoModeStarted()

            override fun onDemoModeFinished() = clock.onDemoModeFinished()
        }

    private val chipVisibilityListener: ChipVisibilityListener =
        object : ChipVisibilityListener {
            override fun onChipVisibilityRefreshed(visible: Boolean) {
                // If the privacy chip is visible, we hide the status icons and battery remaining
                // icon, only in QQS.
                val update =
                    combinedShadeHeadersConstraintManager.privacyChipVisibilityConstraints(visible)
                header.updateAllConstraints(update)
            }
        }

    private val configurationControllerListener =
        object : ConfigurationController.ConfigurationListener {
            override fun onConfigChanged(newConfig: Configuration?) {
                val left =
                    header.resources.getDimensionPixelSize(
                        R.dimen.large_screen_shade_header_left_padding
                    )
                header.setPadding(
                    left,
                    header.paddingTop,
                    header.paddingRight,
                    header.paddingBottom,
                )
                systemIconsHoverContainer.setPaddingRelative(
                    resources.getDimensionPixelSize(
                        R.dimen.hover_system_icons_container_padding_start
                    ),
                    resources.getDimensionPixelSize(
                        R.dimen.hover_system_icons_container_padding_top
                    ),
                    resources.getDimensionPixelSize(
                        R.dimen.hover_system_icons_container_padding_end
                    ),
                    resources.getDimensionPixelSize(
                        R.dimen.hover_system_icons_container_padding_bottom
                    ),
                )
            }

            override fun onDensityOrFontScaleChanged() {
                clock.setTextAppearance(R.style.TextAppearance_QS_Status)
                date.setTextAppearance(R.style.TextAppearance_QS_Status)
                updateQsHeaderClockDateVisibility()
                mShadeCarrierGroup.updateTextAppearance(R.style.TextAppearance_QS_Status_Carriers)
                loadConstraints()
                header.minHeight =
                    resources.getDimensionPixelSize(R.dimen.large_screen_shade_header_min_height)
                lastInsets?.let { updateConstraintsForInsets(header, it) }
                updateResources()
                updateCarrierGroupPadding()
                clock.onDensityOrFontScaleChanged()
            }

            override fun onUiModeChanged() {
                updateResources()
            }

            override fun onThemeChanged() {
                clock.setTextAppearance(R.style.TextAppearance_QS_Status)
                date.setTextAppearance(R.style.TextAppearance_QS_Status)
                updateQsHeaderClockDateVisibility()
                mShadeCarrierGroup.updateTextAppearance(R.style.TextAppearance_QS_Status_Carriers)
            }
        }

    private val nextAlarmCallback =
        NextAlarmController.NextAlarmChangeCallback { nextAlarm ->
            nextAlarmIntent = nextAlarm?.showIntent
        }


    fun updateQsHeaderClockDateVisibility() {
        // Check current custom clock style setting
        val currentClockStyle = Settings.System.getIntForUser(
            context.contentResolver, "qs_header_clock_style", 2, UserHandle.USER_CURRENT
        )

        val isCustomClockEnabled = currentClockStyle != 0

        // Hide default clock when custom clock is enabled
        clock.visibility = if (isCustomClockEnabled) View.GONE else View.VISIBLE

        // Hide the default date when custom clock is enabled, to prevent double-date
        date.visibility = if (isCustomClockEnabled) View.GONE else View.VISIBLE

        if (!isCustomClockEnabled) {
            val colorStateList = ColorStateList.valueOf(Color.WHITE)
            clock.setTextColor(colorStateList)
        }
    }

    private fun isOnKeyguard(): Boolean {
        // Check if we're on keyguard by looking for keyguard-specific views
        val root = header.rootView
        val keyguardStatus = root.findViewById<View?>(com.android.systemui.res.R.id.keyguard_status_view)
        val keyguardBottom = root.findViewById<View?>(com.android.systemui.res.R.id.keyguard_bottom_area)
        val qsPanel = root.findViewById<View?>(com.android.systemui.res.R.id.quick_settings_panel)
        
        // If keyguard views are visible OR QS panel is not visible, we're on keyguard
        return (keyguardStatus?.visibility == View.VISIBLE) || 
               (keyguardBottom?.visibility == View.VISIBLE) ||
               (qsPanel?.visibility != View.VISIBLE)
    }

    private fun isHeaderImageEnabled(): Boolean {
        return Settings.System.getIntForUser(
            context.contentResolver, Settings.System.STATUS_BAR_CUSTOM_HEADER, 0, UserHandle.USER_CURRENT
        ) == 1
    }

    /**
     * Map QS header settings to an alpha value for the full QS background image.
     *
     * - Shadow (0..3) controls how dark the image is (higher = darker / more transparent).
     * - Height (compact/default/tall/full) is repurposed as background "strength", giving
     *   a bit more punch when users pick taller headers.
     */
    private fun computeHeaderBackgroundAlpha(): Float {
        val resolver = context.contentResolver

        // Shadow: 0 (disabled) .. 3 (heavy)
        val shadow = Settings.System.getIntForUser(
            resolver,
            Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW,
            0,
            UserHandle.USER_CURRENT
        )

        // Base alpha from shadow (brighter to dimmer)
        val baseAlpha = when (shadow) {
            1 -> 0.55f
            2 -> 0.4f
            3 -> 0.25f
            else -> 0.7f
        }

        // Height: use as a soft strength factor; compact headers = softer background,
        // tall/full headers = stronger image
        val height = Settings.System.getIntForUser(
            resolver,
            Settings.System.STATUS_BAR_CUSTOM_HEADER_HEIGHT,
            142,
            UserHandle.USER_CURRENT
        ).coerceIn(80, 260)

        val strength = when {
            height <= 120 -> 0.8f
            height <= 160 -> 1.0f
            height <= 200 -> 1.1f
            else -> 1.2f
        }

        // When QS is fully expanded, use full opacity for full panel coverage
        val isQsFullyExpanded = isQsPanelVisible()
        val finalAlpha = if (isQsFullyExpanded) {
            // Fully expanded - use higher alpha for full visibility (0.8-1.0)
            (baseAlpha * strength).coerceIn(0.8f, 1.0f)
        } else {
            // Collapsed - use lower alpha for subtle background (0.15-0.9)
            (baseAlpha * strength).coerceIn(0.15f, 0.9f)
        }
        
        return finalAlpha
    }

    private fun updateHeaderImageVisibility() {
        // Hide small header strip on keyguard to avoid showing it on lockscreen
        val shouldShowHeaderStrip = isHeaderImageEnabled() && !isOnKeyguard()
        qsHeaderImage?.visibility = if (shouldShowHeaderStrip) View.VISIBLE else View.GONE
        
        // Panel background should be visible when header is enabled OR when it has an image
        // The panel background is the full QS background, not just the small header strip
        val panelIv = qsPanelBackgroundImage as? android.widget.ImageView
        if (panelIv != null) {
            val hasImage = panelIv.drawable != null
            val shouldShowPanel = isHeaderImageEnabled() || hasImage
            panelIv.visibility = if (shouldShowPanel) View.VISIBLE else View.GONE
            
            // If header is enabled but image isn't loaded yet, ensure it gets loaded
            if (isHeaderImageEnabled() && !hasImage) {
                android.util.Log.d("ShadeHeaderController", "Header enabled but no image, triggering load")
                // Trigger header machine to load image
                (headerMachine as? StatusBarHeaderMachine)?.updateEnablement()
            }
        } else {
            // View not found - try to find it again
            qsPanelBackgroundImage = header.rootView.findViewById(com.android.systemui.res.R.id.qs_panel_background_image)
            if (qsPanelBackgroundImage != null && isHeaderImageEnabled()) {
                (headerMachine as? StatusBarHeaderMachine)?.updateEnablement()
            }
        }
    }

    private fun isQsPanelVisible(): Boolean {
        val root = header.rootView
        val qsPanel = root.findViewById<View?>(com.android.systemui.res.R.id.quick_settings_panel)
        val qsContainer = root.findViewById<View?>(com.android.systemui.res.R.id.quick_settings_container)
        return (qsPanel?.visibility == View.VISIBLE) || (qsContainer?.visibility == View.VISIBLE)
    }

    override fun onInit() {
        variableDateViewControllerFactory.create(date as VariableDateView).init()
        batteryMeterViewController.init()

        // battery settings same as in QS icons
        batteryMeterViewController.ignoreTunerUpdates()

        val fgColor =
            Utils.getColorAttrDefaultColor(header.context, android.R.attr.textColorPrimary)
        val bgColor =
            Utils.getColorAttrDefaultColor(header.context, android.R.attr.textColorPrimaryInverse)

        iconManager = tintedIconManagerFactory.create(iconContainer, StatusBarLocation.QS)
        iconManager.setTint(fgColor, bgColor)

        batteryIcon.updateColors(
            fgColor /* foreground */,
            bgColor /* background */,
            fgColor, /* single tone (current default) */
        )

        carrierIconSlots =
            listOf(header.context.getString(com.android.internal.R.string.status_bar_mobile))
        mShadeCarrierGroupController =
            shadeCarrierGroupControllerBuilder.setShadeCarrierGroup(mShadeCarrierGroup).build()

        privacyIconsController.onParentVisible()

        // Initialize header image view and apply default visibility
        qsHeaderImage = header.findViewById<View>(com.android.systemui.res.R.id.qs_header_image_view)
        updateHeaderImageVisibility()

        // Initialize full QS panel background image (lives in qs_panel.xml)
        // Try to find it in root view, but it might not be inflated yet
        qsPanelBackgroundImage =
            header.rootView.findViewById(com.android.systemui.res.R.id.qs_panel_background_image)
        
        // If not found in root view, try finding it after a layout pass
        if (qsPanelBackgroundImage == null) {
            header.post {
                qsPanelBackgroundImage =
                    header.rootView.findViewById(com.android.systemui.res.R.id.qs_panel_background_image)
                if (qsPanelBackgroundImage != null && headerMachine != null) {
                    // If we found it now and header machine is ready, trigger update
                    headerObserver?.let { observer ->
                        val drawable = headerMachine?.getCurrent()
                        if (drawable != null) {
                            observer.updateHeader(drawable, true)
                        }
                    }
                }
            }
        }

        // Hook StatusBarHeaderMachine to dynamically supply headers
        headerMachine = StatusBarHeaderMachine(context)
        (header as? androidx.constraintlayout.widget.ConstraintLayout)?.let {
            // if needed in future
        }
    }

    override fun onViewAttached() {
        privacyIconsController.chipVisibilityListener = chipVisibilityListener
        updateVisibility()
        updateTransition()
        updateCarrierGroupPadding()

        header.setOnApplyWindowInsetsListener(insetListener)

        clock.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val newPivot = if (v.isLayoutRtl) v.width.toFloat() else 0f
            v.pivotX = newPivot
            v.pivotY = v.height.toFloat() / 2
        }
        clock.setOnClickListener { launchClockActivity() }
        batteryIcon.setOnClickListener {
            activityStarter.postStartActivityDismissingKeyguard(
                Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 0
            )
        }

        dumpManager.registerDumpable(this)
        configurationController.addCallback(configurationControllerListener)
        demoModeController.addCallback(demoModeReceiver)
        statusBarIconController.addIconGroup(iconManager)
        nextAlarmController.addCallback(nextAlarmCallback)
        updateResources()
        systemIconsHoverContainer.setOnHoverListener(
            statusOverlayHoverListenerFactory.createListener(systemIconsHoverContainer)
        )

        // tunerService.addTunable(this, QS_HEADER_CLOCK_STYLE) // Disabled - TunerService not available
        updateQsHeaderClockDateVisibility() // Update clock/date visibility based on custom clock setting

        // Subscribe header machine
        headerObserver = object: StatusBarHeaderMachine.IStatusBarHeaderMachineObserver {
            override fun updateHeader(headerImage: android.graphics.drawable.Drawable?, force: Boolean) {
                // Try to find panel background image if not found yet - try multiple times
                if (qsPanelBackgroundImage == null) {
                    qsPanelBackgroundImage =
                        header.rootView.findViewById(com.android.systemui.res.R.id.qs_panel_background_image)
                }
                
                // If still not found, try from different root
                if (qsPanelBackgroundImage == null) {
                    val container = header.rootView.findViewById<View?>(com.android.systemui.res.R.id.quick_settings_container)
                    qsPanelBackgroundImage = container?.findViewById(com.android.systemui.res.R.id.qs_panel_background_image)
                }
                
                val panelIv = qsPanelBackgroundImage as? android.widget.ImageView

                // Use header image as full QS background, with alpha controlled by
                // QS header settings (shadow + height).
                if (panelIv != null && headerImage != null) {
                    panelIv.setImageDrawable(headerImage)
                    panelIv.alpha = computeHeaderBackgroundAlpha()
                    // Always make visible when we have an image
                    android.util.Log.d("ShadeHeaderController", "Setting panel background image - drawable: $headerImage, alpha: ${panelIv.alpha}, scaleType: ${panelIv.scaleType}")
                    // #region agent log
                    try {
                        val fw = java.io.FileWriter("/media/linuxmain/lineageos/android/lineageos/.cursor/debug.log", true)
                        fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ShadeHeaderController.kt:584\",\"message\":\"Setting panel background image\",\"data\":{\"drawableType\":\"" + headerImage.javaClass.simpleName + "\",\"alpha\":" + panelIv.alpha + ",\"scaleType\":\"" + panelIv.scaleType + "\",\"imageViewSize\":\"" + panelIv.width + "x" + panelIv.height + "\"},\"sessionId\":\"debug-session\",\"runId\":\"qs-centering-test\",\"hypothesisId\":\"J\"}\n")
                        fw.close()
                    } catch (e: Exception) {
                        android.util.Log.d("ShadeHeaderController", "Log write failed", e)
                    }
                    // #endregion
                    panelIv.visibility = android.view.View.VISIBLE
                    android.util.Log.d("ShadeHeaderController", "Header image applied: ${headerImage.intrinsicWidth}x${headerImage.intrinsicHeight}, alpha=${panelIv.alpha}, visible=${panelIv.visibility}")
                } else {
                    android.util.Log.w("ShadeHeaderController", "Cannot apply header: panelIv=${panelIv != null}, headerImage=${headerImage != null}, qsPanelBackgroundImage=${qsPanelBackgroundImage != null}")
                    // If panel view exists but image is null, try to trigger reload
                    if (panelIv != null && headerImage == null && isHeaderImageEnabled()) {
                        android.util.Log.d("ShadeHeaderController", "Header image is null but header is enabled, triggering reload")
                        (headerMachine as? StatusBarHeaderMachine)?.updateEnablement()
                    } else if (qsPanelBackgroundImage == null) {
                        android.util.Log.e("ShadeHeaderController", "qs_panel_background_image view not found in root view!")
                    }
                }
                // Small header strip is handled separately; we don't touch it here.
            }
            override fun disableHeader() {
                // no-op
            }
            override fun refreshHeader() {
                // no-op
            }
        }
        // Find panel background image if not found yet (view might be inflated now)
        if (qsPanelBackgroundImage == null) {
            qsPanelBackgroundImage =
                header.rootView.findViewById(com.android.systemui.res.R.id.qs_panel_background_image)
        }
        
        (headerMachine as? StatusBarHeaderMachine)?.addObserver(headerObserver)
        // Force update enablement to ensure header loads immediately
        (headerMachine as? StatusBarHeaderMachine)?.updateEnablement()
        // Also trigger a manual update to ensure image is applied
        if (isHeaderImageEnabled()) {
            header.post {
                val drawable = (headerMachine as? StatusBarHeaderMachine)?.getCurrent()
                if (drawable != null) {
                    headerObserver?.updateHeader(drawable, true)
                }
            }
        }
        
        // Register ContentObserver to watch for header image setting changes
        headerImageSettingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateHeaderImageVisibility()
                // Also refresh header machine to reload image if needed
                (headerMachine as? StatusBarHeaderMachine)?.updateEnablement()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.STATUS_BAR_CUSTOM_HEADER),
            false,
            headerImageSettingsObserver,
            UserHandle.USER_CURRENT
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.STATUS_BAR_CUSTOM_HEADER_IMAGE),
            false,
            headerImageSettingsObserver,
            UserHandle.USER_CURRENT
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER),
            false,
            headerImageSettingsObserver,
            UserHandle.USER_CURRENT
        )
    }

    override fun onViewDetached() {
        clock.setOnClickListener(null)
        privacyIconsController.chipVisibilityListener = null
        dumpManager.unregisterDumpable(this::class.java.simpleName)
        configurationController.removeCallback(configurationControllerListener)
        demoModeController.removeCallback(demoModeReceiver)
        statusBarIconController.removeIconGroup(iconManager)
        nextAlarmController.removeCallback(nextAlarmCallback)
        systemIconsHoverContainer.setOnHoverListener(null)
        // tunerService.removeTunable(this) // Disabled - TunerService not available
        headerObserver?.let { (headerMachine as? StatusBarHeaderMachine)?.removeObserver(it) }
        headerObserver = null
        // Unregister ContentObserver
        headerImageSettingsObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
        }
        headerImageSettingsObserver = null
    }

    // Disabled TunerService functionality
    // override fun onTuningChanged(key: String?, value: String?) {
    //     when (key) {
    //         QS_HEADER_CLOCK_STYLE -> {
    //             qsClockStyle = TunerService.parseInteger(value, 0)
    //             updateQsHeaderClockDateVisibility()
    //         }
    //         else -> return
    //     }
    // }

    fun disable(state1: Int, state2: Int, animate: Boolean) {
        val disabled = state2 and StatusBarManager.DISABLE2_QUICK_SETTINGS != 0
        if (disabled == qsDisabled) return
        qsDisabled = disabled
        updateVisibility()
        // Re-evaluate background visibility when state changes
        updateHeaderImageVisibility()
    }

    fun startCustomizingAnimation(show: Boolean, duration: Long) {
        header
            .animate()
            .setDuration(duration)
            .alpha(if (show) 0f else 1f)
            .setInterpolator(if (show) Interpolators.ALPHA_OUT else Interpolators.ALPHA_IN)
            .setListener(CustomizerAnimationListener(show))
            .start()
    }

    @VisibleForTesting
    internal fun launchClockActivity() {
        if (nextAlarmIntent != null) {
            activityStarter.postStartActivityDismissingKeyguard(nextAlarmIntent)
        } else {
            activityStarter.postStartActivityDismissingKeyguard(DEFAULT_CLOCK_INTENT, 0 /*delay */)
        }
    }

    private fun loadConstraints() {
        // Use resources.getXml instead of passing the resource id due to bug b/205018300
        header
            .getConstraintSet(QQS_HEADER_CONSTRAINT)
            .load(context, resources.getXml(R.xml.qqs_header))
        header
            .getConstraintSet(QS_HEADER_CONSTRAINT)
            .load(context, resources.getXml(R.xml.qs_header))
        header
            .getConstraintSet(LARGE_SCREEN_HEADER_CONSTRAINT)
            .load(context, resources.getXml(R.xml.large_screen_shade_header))
    }

    private fun updateCarrierGroupPadding() {
        clock.doOnLayout {
            val maxClockWidth =
                (clock.width * resources.getFloat(R.dimen.qqs_expand_clock_scale)).toInt()
            mShadeCarrierGroup.setPaddingRelative(maxClockWidth, 0, 0, 0)
        }
    }

    private fun updateConstraintsForInsets(view: MotionLayout, insets: WindowInsets) {
        val cutout = insets.displayCutout.also { this.cutout = it }

        val sbInsets: Insets = insetsProvider.getStatusBarContentInsetsForCurrentRotation()
        val cutoutLeft = sbInsets.left
        val cutoutRight = sbInsets.right
        val hasCornerCutout: Boolean = insetsProvider.currentRotationHasCornerCutout()
        updateQQSPaddings()
        // Set these guides as the left/right limits for content that lives in the top row, using
        // cutoutLeft and cutoutRight
        var changes =
            combinedShadeHeadersConstraintManager.edgesGuidelinesConstraints(
                if (view.isLayoutRtl) cutoutRight else cutoutLeft,
                header.paddingStart,
                if (view.isLayoutRtl) cutoutLeft else cutoutRight,
                header.paddingEnd,
            )

        if (cutout != null) {
            val topCutout = cutout.boundingRectTop
            if (topCutout.isEmpty || hasCornerCutout) {
                changes += combinedShadeHeadersConstraintManager.emptyCutoutConstraints()
            } else {
                changes +=
                    combinedShadeHeadersConstraintManager.centerCutoutConstraints(
                        view.isLayoutRtl,
                        (view.width - view.paddingLeft - view.paddingRight - topCutout.width()) / 2,
                    )
            }
        } else {
            changes += combinedShadeHeadersConstraintManager.emptyCutoutConstraints()
        }

        view.setPadding(view.paddingLeft, sbInsets.top, view.paddingRight, view.paddingBottom)
        view.updateAllConstraints(changes)
        updateBatteryMode()
    }

    private fun updateBatteryMode() {
        qsBatteryModeController.getBatteryMode(cutout, qsExpandedFraction)?.let {
            batteryIcon.setPercentShowMode(it)
        }
    }

    private fun updateScrollY() {
        if (!largeScreenActive) {
            header.scrollY = qsScrollY
        }
    }

    private fun onShadeExpandedChanged() {
        if (qsVisible) {
            privacyIconsController.startListening()
        } else {
            privacyIconsController.stopListening()
        }
        updateVisibility()
        // Re-evaluate header image when shade visibility changes (e.g., entering/exiting keyguard)
        updateHeaderImageVisibility()
        updatePosition()
    }

    private fun onHeaderStateChanged() {
        updateTransition()
    }

    /**
     * If not using [combinedHeaders] this should only be visible on large screen. Else, it should
     * be visible any time the QQS/QS shade is open.
     */
    private fun updateVisibility() {
        val visibility =
            if (qsDisabled) {
                View.GONE
            } else if (qsVisible && !customizing) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        if (header.visibility != visibility) {
            header.visibility = visibility
            visible = visibility == View.VISIBLE
        }
        // Keep header image in sync with overall header visibility/keyguard state
        updateHeaderImageVisibility()
    }

    private fun updateTransition() {
        if (largeScreenActive) {
            logInstantEvent("Large screen constraints set")
            header.setTransition(LARGE_SCREEN_HEADER_TRANSITION_ID)
            systemIconsHoverContainer.isClickable = true
            systemIconsHoverContainer.setOnClickListener { shadeCollapseAction?.run() }
        } else {
            logInstantEvent("Small screen constraints set")
            header.setTransition(HEADER_TRANSITION_ID)
            systemIconsHoverContainer.setOnClickListener(null)
            systemIconsHoverContainer.isClickable = false
        }
        header.jumpToState(header.startState)
        updatePosition()
        updateScrollY()
    }

    private fun updatePosition() {
        if (!largeScreenActive && visible) {
            logInstantEvent("updatePosition: $qsExpandedFraction")
            header.progress = qsExpandedFraction
            updateBatteryMode()
        }
    }

    private fun logInstantEvent(message: String) {
        Trace.instantForTrack(TRACE_TAG_APP, "LargeScreenHeaderController", message)
    }

    private fun updateListeners() {
        mShadeCarrierGroupController.setListening(visible)
        if (visible) {
            singleCarrier = mShadeCarrierGroupController.isSingleCarrier
            updateIgnoredSlots()
            mShadeCarrierGroupController.setOnSingleCarrierChangedListener {
                singleCarrier = it
                updateIgnoredSlots()
            }
        } else {
            mShadeCarrierGroupController.setOnSingleCarrierChangedListener(null)
        }
    }

    private fun updateIgnoredSlots() {
        // switching from QQS to QS state halfway through the transition
        if (singleCarrier || (!largeScreenActive && qsExpandedFraction < 0.5)) {
            iconContainer.removeIgnoredSlots(carrierIconSlots)
        } else {
            iconContainer.addIgnoredSlots(carrierIconSlots)
        }
    }

    private fun updateResources() {
        roundedCorners = resources.getDimensionPixelSize(R.dimen.rounded_corner_content_padding)
        val padding = resources.getDimensionPixelSize(R.dimen.qs_panel_padding)
        header.setPadding(padding, header.paddingTop, padding, header.paddingBottom)
        updateQQSPaddings()
        qsBatteryModeController.updateResources()

        val textColor = Utils.getColorAttrDefaultColor(context, android.R.attr.textColorPrimary)
        val colorStateList = Utils.getColorAttr(context, android.R.attr.textColorPrimary)
        if (textColor != textColorPrimary) {
            val textColorSecondary = Utils.getColorAttrDefaultColor(context,
                    android.R.attr.textColorSecondary)
            textColorPrimary = textColor
            if (iconManager != null) {
                iconManager.setTint(
                    textColorPrimary,
                    Utils.getColorAttrDefaultColor(context, android.R.attr.textColorPrimaryInverse),
                )
            }
            clock.setTextColor(textColorPrimary)
            date.setTextColor(textColorPrimary)
            mShadeCarrierGroup.updateColors(textColorPrimary, colorStateList)
            batteryIcon.updateColors(textColorPrimary, textColorSecondary, textColorPrimary)
        }
    }

    private fun updateQQSPaddings() {
        val clockPaddingStart =
            resources.getDimensionPixelSize(R.dimen.status_bar_left_clock_starting_padding)
        val clockPaddingEnd =
            resources.getDimensionPixelSize(R.dimen.status_bar_left_clock_end_padding)
        clock.setPaddingRelative(
            clockPaddingStart,
            clock.paddingTop,
            clockPaddingEnd,
            clock.paddingBottom,
        )
    }

    override fun dump(pw: PrintWriter, args: Array<out String>) {
        pw.println("visible: $visible")
        pw.println("shadeExpanded: $qsVisible")
        pw.println("shadeExpandedFraction: $shadeExpandedFraction")
        pw.println("active: $largeScreenActive")
        pw.println("qsExpandedFraction: $qsExpandedFraction")
        pw.println("qsScrollY: $qsScrollY")
        pw.println("currentState: ${header.currentState.stateToString()}")
    }

    private fun MotionLayout.updateConstraints(@IdRes state: Int, update: ConstraintChange) {
        val constraints = getConstraintSet(state)
        constraints.update()
        updateState(state, constraints)
    }

    /**
     * Updates the [ConstraintSet] for the case of combined headers.
     *
     * Only non-`null` changes are applied to reduce the number of rebuilding in the [MotionLayout].
     */
    private fun MotionLayout.updateAllConstraints(updates: ConstraintsChanges) {
        if (updates.qqsConstraintsChanges != null) {
            updateConstraints(QQS_HEADER_CONSTRAINT, updates.qqsConstraintsChanges)
        }
        if (updates.qsConstraintsChanges != null) {
            updateConstraints(QS_HEADER_CONSTRAINT, updates.qsConstraintsChanges)
        }
        if (updates.largeScreenConstraintsChanges != null) {
            updateConstraints(LARGE_SCREEN_HEADER_CONSTRAINT, updates.largeScreenConstraintsChanges)
        }
    }

    @VisibleForTesting internal fun simulateViewDetached() = this.onViewDetached()

    inner class CustomizerAnimationListener(private val enteringCustomizing: Boolean) :
        AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            header.animate().setListener(null)
            if (enteringCustomizing) {
                customizing = true
            }
        }

        override fun onAnimationStart(animation: Animator) {
            super.onAnimationStart(animation)
            if (!enteringCustomizing) {
                customizing = false
            }
        }
    }
}
