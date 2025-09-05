/*
 * Copyright (C) 2014 The NamelessRom Project
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

package com.android.systemui.epic.onthego;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import com.android.systemui.res.R;

import com.android.internal.util.epic.OnTheGoUtils;

public class OnTheGoDialog extends Dialog {

    protected final Context mContext;
    protected final Handler mHandler = new Handler();

    private final int mOnTheGoDialogLongTimeout;
    private final int mOnTheGoDialogShortTimeout;

    private final Runnable mDismissDialogRunnable = new Runnable() {
        public void run() {
            if (OnTheGoDialog.this.isShowing()) {
                OnTheGoDialog.this.dismiss();
            }
        }
    };

    public OnTheGoDialog(Context ctx) {
        super(ctx);
        mContext = ctx;
        final Resources r = mContext.getResources();
        mOnTheGoDialogLongTimeout =
                r.getInteger(R.integer.quick_settings_onthego_dialog_long_timeout);
        mOnTheGoDialogShortTimeout =
                r.getInteger(R.integer.quick_settings_onthego_dialog_short_timeout);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY);
        window.getAttributes().privateFlags |=
                WindowManager.LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS;
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.requestFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.quick_settings_onthego_dialog);
        setCanceledOnTouchOutside(true);

        final ContentResolver resolver = mContext.getContentResolver();

        final SeekBar mSlider = (SeekBar) findViewById(R.id.alpha_slider);
        final float value = Settings.System.getFloat(resolver,
                Settings.System.ON_THE_GO_ALPHA,
                0.48f);
        // Convert alpha (0.0-1.0) to slider progress (0-90)
        // Map 0.1-1.0 to 0-90 range for better user experience
        final int progress = Math.round((value - 0.1f) * 100f);
        mSlider.setProgress(Math.max(0, Math.min(90, progress)));
        mSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // Convert slider progress (0-90) back to alpha (0.1-1.0)
                final float alpha = 0.1f + (i / 100f);
                sendAlphaBroadcast(String.valueOf(alpha));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                removeAllOnTheGoDialogCallbacks();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                dismissOnTheGoDialog(mOnTheGoDialogShortTimeout);
            }
        });

        if (!OnTheGoUtils.hasFrontCamera(getContext())) {
            findViewById(R.id.onthego_category_1).setVisibility(View.GONE);
        } else {
            final Switch mServiceToggle = (Switch) findViewById(R.id.onthego_service_toggle);
            final boolean restartService = Settings.System.getInt(resolver,
                    Settings.System.ON_THE_GO_SERVICE_RESTART, 0) == 1;
            mServiceToggle.setChecked(restartService);
            mServiceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Settings.System.putInt(resolver,
                            Settings.System.ON_THE_GO_SERVICE_RESTART,
                            (b ? 1 : 0));
                    dismissOnTheGoDialog(mOnTheGoDialogShortTimeout);
                }
            });

            final Switch mCamSwitch = (Switch) findViewById(R.id.onthego_camera_toggle);
            final boolean useFrontCam = (Settings.System.getInt(resolver,
                    Settings.System.ON_THE_GO_CAMERA,
                    0) == 1);
            mCamSwitch.setChecked(useFrontCam);
            mCamSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Settings.System.putInt(resolver,
                            Settings.System.ON_THE_GO_CAMERA,
                            (b ? 1 : 0));
                    sendCameraBroadcast();
                    dismissOnTheGoDialog(mOnTheGoDialogShortTimeout);
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        dismissOnTheGoDialog(mOnTheGoDialogLongTimeout);
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeAllOnTheGoDialogCallbacks();
    }

    private void dismissOnTheGoDialog(int timeout) {
        removeAllOnTheGoDialogCallbacks();
        mHandler.postDelayed(mDismissDialogRunnable, timeout);
    }

    private void removeAllOnTheGoDialogCallbacks() {
        mHandler.removeCallbacks(mDismissDialogRunnable);
    }

    private void sendAlphaBroadcast(String alphaValue) {
        final float value = Float.parseFloat(alphaValue);
        android.util.Log.d("OnTheGoDialog", "Sending alpha broadcast: " + value);
        final Intent alphaBroadcast = new Intent();
        alphaBroadcast.setAction(OnTheGoService.ACTION_TOGGLE_ALPHA);
        alphaBroadcast.putExtra(OnTheGoService.EXTRA_ALPHA, value);
        mContext.sendBroadcast(alphaBroadcast);
    }

    private void sendCameraBroadcast() {
        final Intent cameraBroadcast = new Intent();
        cameraBroadcast.setAction(OnTheGoService.ACTION_TOGGLE_CAMERA);
        mContext.sendBroadcast(cameraBroadcast);
    }

}
