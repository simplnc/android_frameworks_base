package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;

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

import javax.inject.Inject;

public class AnimationsTile extends QSTileImpl<QSTile.BooleanState> {

    public static final String TILE_SPEC = "animations";

    @Inject
    public AnimationsTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        boolean enabled = isAnimationsEnabled();
        setAnimationsEnabled(!enabled);
        refreshState(null);
    }

    private boolean isAnimationsEnabled() {
        float w = Settings.Global.getFloat(mContext.getContentResolver(),
                Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f);
        float t = Settings.Global.getFloat(mContext.getContentResolver(),
                Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f);
        float a = Settings.Global.getFloat(mContext.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
        return (w > 0f) || (t > 0f) || (a > 0f);
    }

    private void setAnimationsEnabled(boolean enable) {
        float v = enable ? 1.0f : 0.0f;
        Settings.Global.putFloat(mContext.getContentResolver(),
                Settings.Global.WINDOW_ANIMATION_SCALE, v);
        Settings.Global.putFloat(mContext.getContentResolver(),
                Settings.Global.TRANSITION_ANIMATION_SCALE, v);
        Settings.Global.putFloat(mContext.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, v);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean enabled = isAnimationsEnabled();
        state.state = enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        state.value = enabled;
        state.label = mContext.getString(R.string.quick_settings_anim_scale_label);
        state.icon = maybeLoadResourceIcon(R.drawable.ic_qs_font_scaling);
        state.contentDescription = state.label;
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_anim_scale_label);
    }
}