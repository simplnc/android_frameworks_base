package com.android.systemui.qs.tiles;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
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

public class ChatAiTile extends QSTileImpl<QSTile.State> {

    public static final String TILE_SPEC = "chat_ai";

    // Default packages to try, in order
    private static final String[] DEFAULT_CHAT_AI_PACKAGES = new String[] {
            "com.openai.chatgpt",           // ChatGPT
            "com.openai.chatgpt.beta",      // ChatGPT beta
            "com.anthropic.claude",         // Claude (example)
            "com.perplexity.ai"             // Perplexity (example)
    };

    @Inject
    public ChatAiTile(
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
    public State newTileState() {
        State state = new State();
        state.handlesLongClick = true;
        return state;
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        String pkg = resolveInstalledChatApp();
        if (pkg == null) {
            // Fallback to Play Store search
            Intent market = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=chatgpt"));
            market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mActivityStarter.startActivity(market, true /* dismissShade */);
            } catch (ActivityNotFoundException e) {
                Intent web = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/search?q=chatgpt"));
                web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivityStarter.startActivity(web, true /* dismissShade */);
            }
            return;
        }
        Intent launch = mContext.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivityStarter.startActivity(launch, true /* dismissShade */);
        }
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        state.label = mContext.getString(R.string.quick_settings_chat_ai_label);
        state.contentDescription = state.label;
        state.icon = maybeLoadResourceIcon(R.drawable.ic_qs_notes);
        state.state = Tile.STATE_INACTIVE;
    }

    @Override
    public Intent getLongClickIntent() {
        // Long press opens web ChatGPT as a fallback
        Intent web = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://chat.openai.com"));
        return web;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_chat_ai_label);
    }

    private String resolveInstalledChatApp() {
        PackageManager pm = mContext.getPackageManager();
        for (String candidate : DEFAULT_CHAT_AI_PACKAGES) {
            try {
                pm.getPackageInfo(candidate, 0);
                return candidate;
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        return null;
    }
}