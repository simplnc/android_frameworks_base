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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import javax.inject.Inject;

public class ThreeFingerGestureTile extends SecureQSTile<BooleanState> {
    private static final String TAG = "ThreeFingerGestureTile";
    public static final String TILE_SPEC = "three_finger_gesture";

    @Inject
    public ThreeFingerGestureTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            KeyguardStateController keyguardStateController
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger, keyguardStateController);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_SETTINGS);
    }

    @Override
    protected void handleClick(@Nullable View view, boolean keyguardShowing) {
        if (checkKeyguard(null, keyguardShowing)) {
            return;
        }
        
        if (getState().state == Tile.STATE_UNAVAILABLE) {
            return;
        }
        boolean enabled = !mState.value;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.THREE_FINGER_GESTURE, enabled ? 1 : 0);
        refreshState();
    }

    @Override
    protected void handleLongClick(@Nullable View view) {
        handleClick(view);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.THREE_FINGER_GESTURE, 0) != 0;
        state.label = mContext.getString(R.string.three_finger_gesture_title);
        state.secondaryLabel = mContext.getString(R.string.three_finger_gesture_summary);
        state.icon = ResourceIcon.get(state.value ? 
                R.drawable.ic_gesture_on : R.drawable.ic_gesture_off);
        state.contentDescription = mContext.getString(R.string.three_finger_gesture_title);
        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.three_finger_gesture_title);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_PANEL; // Use generic QS panel category
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_three_finger_gesture_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_three_finger_gesture_changed_off);
        }
    }
}
