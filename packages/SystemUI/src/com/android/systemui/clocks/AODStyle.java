/*
 * Copyright (C) 2023-2024 the risingOS Android Project
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
package com.android.systemui.clocks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.settingslib.drawable.CircleFramedDrawable;

import com.android.systemui.res.R;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.tuner.TunerService;

public class AODStyle extends RelativeLayout implements TunerService.Tunable {

    private static final String TAG = "AODStyle";

    // Tuner keys
    private static final String CUSTOM_AOD_IMAGE_ENABLED_KEY = "custom_aod_image_enabled";
    private static final String CUSTOM_AOD_IMAGE_PATH_KEY = "custom_aod_image_path";
    private static final String CUSTOM_CLOCK_ENABLED_KEY = "custom_clock_enabled";

    private final Context mContext;
    private final TunerService mTunerService;
    private final StatusBarStateController mStatusBarStateController;

    private ImageView mAodImageView;
    private boolean mAodImageEnabled;
    private boolean mCustomClockEnabled;
    private String mCurrImagePath;
    private boolean mImageLoaded;
    private boolean mDozing;

    // Burn-in protection
    private static final int BURN_IN_PROTECTION_INTERVAL = 60000; // 60 seconds
    private static final int BURN_IN_PROTECTION_MAX_SHIFT = 2; // 2 pixels for AOD
    private final Handler mBurnInProtectionHandler = new Handler();
    private int mCurrentShiftX = 0;
    private int mCurrentShiftY = 0;

    private final Runnable mBurnInProtectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDozing && mAodImageEnabled) {
                mCurrentShiftX = (int) (Math.random() * BURN_IN_PROTECTION_MAX_SHIFT * 2) - BURN_IN_PROTECTION_MAX_SHIFT;
                mCurrentShiftY = (int) (Math.random() * BURN_IN_PROTECTION_MAX_SHIFT * 2) - BURN_IN_PROTECTION_MAX_SHIFT;
                if (mAodImageView != null) {
                    mAodImageView.setTranslationX(mCurrentShiftX);
                    mAodImageView.setTranslationY(mCurrentShiftY);
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

    public AODStyle(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mTunerService = Dependency.get(TunerService.class);
        mStatusBarStateController = Dependency.get(StatusBarStateController.class);

        mTunerService.addTunable(this,
                CUSTOM_AOD_IMAGE_ENABLED_KEY,
                CUSTOM_AOD_IMAGE_PATH_KEY,
                CUSTOM_CLOCK_ENABLED_KEY);

        mStatusBarStateController.addCallback(mStatusBarStateListener);
        mStatusBarStateListener.onDozingChanged(mStatusBarStateController.isDozing());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAodImageView = findViewById(R.id.custom_aod_image_view);
        // Ensure AOD image doesn't interfere with accessibility
        if (mAodImageView != null) {
            mAodImageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        updateAodImageView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mStatusBarStateController.removeCallback(mStatusBarStateListener);
        mTunerService.removeTunable(this);
        stopBurnInProtection();
    }

    private void startBurnInProtection() {
        if (!mAodImageEnabled) return;
        mBurnInProtectionHandler.post(mBurnInProtectionRunnable);
    }

    private void stopBurnInProtection() {
        mBurnInProtectionHandler.removeCallbacks(mBurnInProtectionRunnable);
        if (mAodImageView != null) {
            mAodImageView.setTranslationX(0);
            mAodImageView.setTranslationY(0);
        }
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case CUSTOM_AOD_IMAGE_ENABLED_KEY:
                mAodImageEnabled = TunerService.parseIntegerSwitch(newValue, false) && mCustomClockEnabled;
                break;
            case CUSTOM_AOD_IMAGE_PATH_KEY:
                mCurrImagePath = newValue;
                mImageLoaded = false;
                break;
            case CUSTOM_CLOCK_ENABLED_KEY:
                mCustomClockEnabled = TunerService.parseIntegerSwitch(newValue, false);
                mAodImageEnabled = mAodImageEnabled && mCustomClockEnabled;
                break;
        }
        updateAodImageView();
    }

    private void updateAodImageView() {
        if (mAodImageView == null || !mAodImageEnabled) {
            if (mAodImageView != null) mAodImageView.setVisibility(View.GONE);
            return;
        }
        loadAodImage();
        if (mDozing) {
            mAodImageView.setVisibility(View.VISIBLE);
            mAodImageView.setScaleX(0f);
            mAodImageView.setScaleY(0f);
            mAodImageView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .withEndAction(this::startBurnInProtection)
                    .start();
        } else {
            mAodImageView.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        mAodImageView.setVisibility(View.GONE);
                        stopBurnInProtection();
                    })
                    .start();
        }
    }

    private void loadAodImage() {
        if (mAodImageView == null || mCurrImagePath == null
                || mCurrImagePath.isEmpty() || mImageLoaded) return;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(mCurrImagePath);
            if (bitmap != null) {
                int targetSize = (int) mContext.getResources().getDimension(R.dimen.custom_aod_image_size);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true);
                try (java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream()) {
                    scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 90, stream);
                    byte[] byteArray = stream.toByteArray();
                    Bitmap compressedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                    Drawable roundedImg = new CircleFramedDrawable(compressedBitmap, targetSize);
                    mAodImageView.setImageDrawable(roundedImg);
                    scaledBitmap.recycle();
                    compressedBitmap.recycle();
                    mImageLoaded = true;
                }
            } else {
                mImageLoaded = false;
                mAodImageView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            mImageLoaded = false;
            mAodImageView.setVisibility(View.GONE);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }
}
