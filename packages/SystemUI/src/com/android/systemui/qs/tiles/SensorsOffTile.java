package com.android.systemui.qs.tiles;

import static android.hardware.SensorPrivacyManager.Sources.QS_TILE;

import android.hardware.SensorPrivacyManager;
import android.os.Handler;
import android.os.Looper;
import android.safetycenter.SafetyCenterManager;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.statusbar.policy.SensorPrivacyController;

import javax.inject.Inject;

public class SensorsOffTile extends QSTileImpl<QSTile.BooleanState> implements
        SensorPrivacyManager.OnAllSensorPrivacyChangedListener {

    public static final String TILE_SPEC = "sensorsoff";

    private final KeyguardStateController mKeyguard;
    private final SensorPrivacyManager mSensorPrivacyManager;
    private boolean mEnabled;

    @Inject
    public SensorsOffTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            KeyguardStateController keyguardStateController,
            SensorPrivacyManager sensorPrivacyManager) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mKeyguard = keyguardStateController;
        mSensorPrivacyManager = sensorPrivacyManager;
        mEnabled = mSensorPrivacyManager.isAllSensorPrivacyEnabled();
        mSensorPrivacyManager.addAllSensorPrivacyListener(this);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        boolean current = mSensorPrivacyManager.isAllSensorPrivacyEnabled();
        if (mKeyguard.isMethodSecure() && mKeyguard.isShowing()) {
            mActivityStarter.postQSRunnableDismissingKeyguard(() ->
                    mSensorPrivacyManager.setAllSensorPrivacy(!current));
            return;
        }
        mSensorPrivacyManager.setAllSensorPrivacy(!current);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean isEnabled = mSensorPrivacyManager.isAllSensorPrivacyEnabled();
        state.state = isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        state.value = isEnabled;
        state.label = mContext.getString(R.string.quick_settings_sensors_off_label);
        state.icon = maybeLoadResourceIcon(R.drawable.qs_mic_access_off);
        state.contentDescription = state.label;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_sensors_off_label);
    }

    @Override
    public void onAllSensorPrivacyChanged(boolean enabled) {
        mEnabled = enabled;
        refreshState(null);
    }
}