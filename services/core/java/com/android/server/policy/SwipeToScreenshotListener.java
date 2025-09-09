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

package com.android.server.policy;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Listener for three-finger swipe to screenshot gesture
 * Ported from Oppo ColorOS implementation
 */
public class SwipeToScreenshotListener {
    private static final String TAG = "SwipeToScreenshotListener";
    
    // Gesture states
    private static final int THREE_GESTURE_STATE_NONE = 0;
    private static final int THREE_GESTURE_STATE_DETECTING = 1;
    private static final int THREE_GESTURE_STATE_DETECTED_TRUE = 2;
    private static final int THREE_GESTURE_STATE_DETECTED_FALSE = 3;
    private static final int THREE_GESTURE_STATE_NO_DETECT = 4;
    
    private Context mContext;
    private DisplayMetrics mDisplayMetrics;
    private Callbacks mCallbacks;
    
    // Gesture detection variables
    private int mThreeGestureState = THREE_GESTURE_STATE_NONE;
    private int[] mPointerIds = new int[3];
    private float[] mInitMotionY = new float[3];
    private int mThreeGestureThreshold;
    private int mThreshold;
    private boolean mBootCompleted;
    private boolean mDeviceProvisioned;
    
    public SwipeToScreenshotListener(Context context, DisplayMetrics displayMetrics, Callbacks callbacks) {
        mContext = context;
        mDisplayMetrics = displayMetrics;
        mCallbacks = callbacks;
        
        // Initialize thresholds
        mThreeGestureThreshold = (int) (mDisplayMetrics.density * 100.0f);
        mThreshold = (int) (mDisplayMetrics.density * 50.0f);
        
        // Check initial state
        mBootCompleted = SystemProperties.getBoolean("sys.boot_completed", false);
        mDeviceProvisioned = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) != 0;
    }
    
    public void onTouchEvent(MotionEvent event) {
        if (!mBootCompleted) {
            mBootCompleted = SystemProperties.getBoolean("sys.boot_completed", false);
            return;
        }
        if (!mDeviceProvisioned) {
            mDeviceProvisioned = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.DEVICE_PROVISIONED, 0) != 0;
            return;
        }
        
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            changeThreeGestureState(THREE_GESTURE_STATE_NONE);
        } else if (mThreeGestureState == THREE_GESTURE_STATE_NONE && event.getPointerCount() == 3) {
            if (checkIsStartThreeGesture(event)) {
                changeThreeGestureState(THREE_GESTURE_STATE_DETECTING);
                for (int i = 0; i < 3; i++) {
                    mPointerIds[i] = event.getPointerId(i);
                    mInitMotionY[i] = event.getY(i);
                }
            } else {
                changeThreeGestureState(THREE_GESTURE_STATE_NO_DETECT);
            }
        }
        
        if (mThreeGestureState == THREE_GESTURE_STATE_DETECTING) {
            if (event.getPointerCount() != 3) {
                changeThreeGestureState(THREE_GESTURE_STATE_DETECTED_FALSE);
                return;
            }
            
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float distance = 0.0f;
                int i = 0;
                while (i < 3) {
                    int index = event.findPointerIndex(mPointerIds[i]);
                    if (index < 0 || index >= 3) {
                        changeThreeGestureState(THREE_GESTURE_STATE_DETECTED_FALSE);
                        return;
                    } else {
                        distance += event.getY(index) - mInitMotionY[i];
                        i++;
                    }
                }
                
                if (distance >= ((float) mThreeGestureThreshold)) {
                    changeThreeGestureState(THREE_GESTURE_STATE_DETECTED_TRUE);
                    mCallbacks.onSwipeThreeFinger();
                }
            }
        }
    }
    
    private void changeThreeGestureState(int state) {
        if (mThreeGestureState != state) {
            mThreeGestureState = state;
            boolean shouldEnableProp = mThreeGestureState == THREE_GESTURE_STATE_DETECTED_TRUE ||
                    mThreeGestureState == THREE_GESTURE_STATE_DETECTING;
            try {
                SystemProperties.set("sys.android.screenshot", shouldEnableProp ? "true" : "false");
            } catch (Exception e) {
                Log.e(TAG, "Exception when setprop", e);
            }
        }
    }
    
    private boolean checkIsStartThreeGesture(MotionEvent event) {
        if (event.getEventTime() - event.getDownTime() > 500) {
            return false;
        }
        
        int height = mDisplayMetrics.heightPixels;
        int width = mDisplayMetrics.widthPixels;
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        
        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            if (y > ((float) (height - mThreshold))) {
                return false;
            }
            maxX = Math.max(maxX, x);
            minX = Math.min(minX, x);
            maxY = Math.max(maxY, y);
            minY = Math.min(minY, y);
        }
        
        if (maxY - minY <= mDisplayMetrics.density * 150.0f) {
            return maxX - minX <= ((float) Math.min(width, height));
        }
        return false;
    }
    
    interface Callbacks {
        void onSwipeThreeFinger();
    }
}
