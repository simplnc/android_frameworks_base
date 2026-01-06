/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.clocks;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextClock;

import com.android.systemui.res.R;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;

public class ClockStyle extends RelativeLayout {

    private static final int[] CLOCK_LAYOUTS = {
            0,
            R.layout.keyguard_clock_oos,
            R.layout.keyguard_clock_center,
            R.layout.keyguard_clock_simple,
            R.layout.keyguard_clock_ide,
            R.layout.keyguard_clock_miui,
            R.layout.keyguard_clock_moto
    };

    private static final int DEFAULT_STYLE = 1; // OOS clock by default
    public static final String CLOCK_STYLE_KEY = "clock_style";

    private final Context mContext;
    private final KeyguardManager mKeyguardManager;

    private View currentClockView;
    private int mClockStyle;
    private int mClockPosition;

    private final ContentObserver mClockStyleObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateClockStyle();
        }
    };

    private final ContentObserver mClockPositionObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateClockPosition();
        }
    };    

    private static final long UPDATE_INTERVAL_MILLIS = 15 * 1000;
    private long lastUpdateTimeMillis = 0;

    private final StatusBarStateController mStatusBarStateController;

    private boolean mDozing;

    // Burn-in protection
    private static final int BURN_IN_PROTECTION_INTERVAL = 10000; // 10 seconds
    private static final int BURN_IN_PROTECTION_MAX_SHIFT = 4; // 4 pixels
    private final Handler mBurnInProtectionHandler = new Handler();
    private int mCurrentShiftX = 0;
    private int mCurrentShiftY = 0;

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mKeyguardManager != null 
                && mKeyguardManager.isKeyguardLocked()) {
                onTimeChanged();
            }
        }
    };

    private final Runnable mBurnInProtectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDozing) {
                mCurrentShiftX = (int) (Math.random() * BURN_IN_PROTECTION_MAX_SHIFT * 2) - BURN_IN_PROTECTION_MAX_SHIFT;
                mCurrentShiftY = (int) (Math.random() * BURN_IN_PROTECTION_MAX_SHIFT * 2) - BURN_IN_PROTECTION_MAX_SHIFT;
                if (currentClockView != null) {
                    currentClockView.setTranslationX(mCurrentShiftX);
                    currentClockView.setTranslationY(mCurrentShiftY);
                }
                invalidate();
                mBurnInProtectionHandler.postDelayed(this, BURN_IN_PROTECTION_INTERVAL);
            }
        }
    };

    private final StatusBarStateController.StateListener mStatusBarStateListener =
            new StatusBarStateController.StateListener() {
        @Override
        public void onStateChanged(int newState) {}

        @Override
        public void onDozingChanged(boolean dozing) {
            if (mDozing == dozing) {
                return;
            }
            mDozing = dozing;
            if (mDozing) {
                startBurnInProtection();
            } else {
                stopBurnInProtection();
            }
        }
    };

    public ClockStyle(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        mStatusBarStateController = Dependency.get(StatusBarStateController.class);
        mStatusBarStateController.addCallback(mStatusBarStateListener);
        mStatusBarStateListener.onDozingChanged(mStatusBarStateController.isDozing());

        // Register content observers for clock style and position changes
        mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(CLOCK_STYLE_KEY),
            false,
            mClockStyleObserver
        );
        mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor("clock_position"),
            false,
            mClockPositionObserver
        );

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction("com.android.systemui.doze.pulse");
        mContext.registerReceiver(mScreenReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        updateClockStyle();
        updateClockPosition();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mStatusBarStateController.removeCallback(mStatusBarStateListener);
        mContext.getContentResolver().unregisterContentObserver(mClockStyleObserver);
        mContext.getContentResolver().unregisterContentObserver(mClockPositionObserver);
        mBurnInProtectionHandler.removeCallbacks(mBurnInProtectionRunnable);
        mContext.unregisterReceiver(mScreenReceiver);
    }

    private void startBurnInProtection() {
        if (mClockStyle == 0) return;
        mBurnInProtectionHandler.post(mBurnInProtectionRunnable);
    }

    private void stopBurnInProtection() {
        if (mClockStyle == 0) return;
        mBurnInProtectionHandler.removeCallbacks(mBurnInProtectionRunnable);
        if (currentClockView != null) {
            currentClockView.setTranslationX(0);
            currentClockView.setTranslationY(0);
        }
    }

    private void updateTextClockViews(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childView = viewGroup.getChildAt(i);
                updateTextClockViews(childView);
                if (childView instanceof TextClock) {
                    ((TextClock) childView).refreshTime();
                }
            }
        }
    }

    public void onTimeChanged() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastUpdateTimeMillis >= UPDATE_INTERVAL_MILLIS) {
            if (currentClockView != null) {
                updateTextClockViews(currentClockView);
                lastUpdateTimeMillis = currentTimeMillis;
            }
        }
    }

    private void updateClockStyle() {
        mClockStyle = Settings.Global.getInt(mContext.getContentResolver(), CLOCK_STYLE_KEY, DEFAULT_STYLE);

        // Force set to OOS clock if invalid
        if (mClockStyle < 0 || mClockStyle >= CLOCK_LAYOUTS.length) {
            mClockStyle = DEFAULT_STYLE;
            Settings.Global.putInt(mContext.getContentResolver(), CLOCK_STYLE_KEY, DEFAULT_STYLE);
        }

        updateClockView();
    }

    private void updateClockPosition() {
        mClockPosition = Settings.Global.getInt(mContext.getContentResolver(), "clock_position", 1); // Default to center

        // Force valid position (0=left, 1=center, 2=right)
        if (mClockPosition < 0 || mClockPosition > 2) {
            mClockPosition = 1;
            Settings.Global.putInt(mContext.getContentResolver(), "clock_position", 1);
        }

        updateClockView();
    }

    private void updateClockView() {
        if (currentClockView != null) {
            ((ViewGroup) currentClockView.getParent()).removeView(currentClockView);
            currentClockView = null;
        }
        if (mClockStyle > 0 && mClockStyle < CLOCK_LAYOUTS.length) {
            ViewStub stub = findViewById(R.id.clock_view_stub);
            if (stub != null) {
                stub.setLayoutResource(CLOCK_LAYOUTS[mClockStyle]);
                currentClockView = stub.inflate();
                int gravity = getGravityForPosition(mClockPosition);
                if (currentClockView instanceof LinearLayout) {
                    ((LinearLayout) currentClockView).setGravity(gravity);
                }
            }
        }
        onTimeChanged();
        
        // Always show when custom clock is enabled
        if (mClockStyle > 0) {
            setVisibility(View.VISIBLE);
            if (currentClockView != null) {
                currentClockView.setVisibility(View.VISIBLE);
            }
        } else {
            setVisibility(View.GONE);
        }
    }


    private int getGravityForPosition(int position) {
        switch (position) {
            case 0: return Gravity.START;    // Left
            case 1: return Gravity.CENTER;   // Center
            case 2: return Gravity.END;      // Right
            default: return Gravity.CENTER;  // Default to center
        }
    }

    private int parseIntegerOrDefault(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
