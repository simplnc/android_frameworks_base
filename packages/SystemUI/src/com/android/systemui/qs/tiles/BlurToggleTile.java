package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.Tile;

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

public class BlurToggleTile extends QSTileImpl<QSTile.BooleanState> {

    public static final String TILE_SPEC = "blur_toggle";

    @Inject
    public BlurToggleTile(
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
        boolean disabled = isBlurDisabled();
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.BLUR_EFFECTS_DISABLED, disabled ? 0 : 1);
        refreshState(null);
    }

    private boolean isBlurDisabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.BLUR_EFFECTS_DISABLED, 0) == 1;
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean disabled = isBlurDisabled();
        state.state = disabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        state.value = disabled;
        state.label = mContext.getString(R.string.quick_settings_blur_label);
        state.icon = maybeLoadResourceIcon(R.drawable.ic_qs_notes);
        state.contentDescription = state.label;
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_DISPLAY_SETTINGS);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_blur_label);
    }
}