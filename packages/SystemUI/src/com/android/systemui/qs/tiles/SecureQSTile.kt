/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.systemui.qs.tiles

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.Nullable
import com.android.internal.logging.MetricsLogger
import com.android.internal.logging.UiEventLogger
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.plugins.FalsingManager
import com.android.systemui.plugins.qs.QSTile
import com.android.systemui.qs.QSHost
import com.android.systemui.qs.logging.QSLogger
import com.android.systemui.statusbar.policy.KeyguardStateController
import com.android.systemui.statusbar.policy.StatusBarStateController
import com.android.systemui.qs.tileimpl.QSTileImpl
import javax.inject.Inject

/**
 * Base class for QS tiles that require unlocking to use.
 * This prevents unauthorized access to sensitive system functions when the device is locked.
 */
abstract class SecureQSTile<T : QSTile.State> @Inject constructor(
    host: QSHost,
    uiEventLogger: UiEventLogger,
    @Background backgroundLooper: Looper,
    @Main mainHandler: Handler,
    falsingManager: FalsingManager,
    metricsLogger: MetricsLogger,
    statusBarStateController: StatusBarStateController,
    activityStarter: ActivityStarter,
    qsLogger: QSLogger,
    private val keyguardStateController: KeyguardStateController
) : QSTileImpl<T>(
    host,
    uiEventLogger,
    backgroundLooper,
    mainHandler,
    falsingManager,
    metricsLogger,
    statusBarStateController,
    activityStarter,
    qsLogger
) {

    /**
     * Checks if the device is locked and shows unlock prompt if needed.
     * @param expandable The expandable view for showing unlock prompt
     * @param keyguardShowing Whether the keyguard is currently showing
     * @return true if the action should be blocked due to keyguard, false otherwise
     */
    protected fun checkKeyguard(@Nullable expandable: QSTileImpl.Expandable?, keyguardShowing: Boolean): Boolean {
        if (keyguardShowing && keyguardStateController.isUnlocked.not()) {
            // Show unlock prompt
            activityStarter.postStartActivityDismissingKeyguard(
                Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS),
                0
            )
            return true
        }
        return false
    }

    /**
     * Override handleClick to include keyguard checking.
     * Subclasses should call this method instead of the original handleClick.
     */
    protected abstract fun handleClick(@Nullable expandable: QSTileImpl.Expandable?, keyguardShowing: Boolean)

    override fun handleClick(@Nullable expandable: QSTileImpl.Expandable?) {
        handleClick(expandable, keyguardStateController.isKeyguardShowing)
    }
}
