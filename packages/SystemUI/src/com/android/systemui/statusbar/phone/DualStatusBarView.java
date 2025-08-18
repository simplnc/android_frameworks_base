/*
 * Copyright (C) 2025 Zeus-OS
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

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.android.systemui.Dependency;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;

/**
 * Dual Status Bar View that contains two status bars - primary and secondary
 * This allows for more information display and better organization of status bar elements
 */
public class DualStatusBarView extends LinearLayout implements Callbacks {
    private static final String TAG = "DualStatusBarView";
    
    private final CommandQueue mCommandQueue;
    private PhoneStatusBarView mPrimaryStatusBar;
    private PhoneStatusBarView mSecondaryStatusBar;
    
    public DualStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCommandQueue = Dependency.get(CommandQueue.class);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPrimaryStatusBar = findViewById(com.android.systemui.res.R.id.primary_status_bar);
        mSecondaryStatusBar = findViewById(com.android.systemui.res.R.id.secondary_status_bar);
        
        if (mPrimaryStatusBar != null && mSecondaryStatusBar != null) {
            // Register callbacks for both status bars
            mCommandQueue.addCallback(this);
        }
    }
    
    public PhoneStatusBarView getPrimaryStatusBar() {
        return mPrimaryStatusBar;
    }
    
    public PhoneStatusBarView getSecondaryStatusBar() {
        return mSecondaryStatusBar;
    }
    
    // Implement Callbacks interface methods
    @Override
    public void disable(int displayId, int state1, int state2, boolean animate) {
        if (mPrimaryStatusBar != null) {
            mPrimaryStatusBar.disable(displayId, state1, state2, animate);
        }
        if (mSecondaryStatusBar != null) {
            mSecondaryStatusBar.disable(displayId, state1, state2, animate);
        }
    }
    
    @Override
    public void setWindowState(int displayId, int window, int state) {
        if (mPrimaryStatusBar != null) {
            mPrimaryStatusBar.setWindowState(displayId, window, state);
        }
        if (mSecondaryStatusBar != null) {
            mSecondaryStatusBar.setWindowState(displayId, window, state);
        }
    }
    
    @Override
    public void setImeWindowStatus(int displayId, int vis, int backDisposition, boolean showImeSwitcher) {
        if (mPrimaryStatusBar != null) {
            mPrimaryStatusBar.setImeWindowStatus(displayId, vis, backDisposition, showImeSwitcher);
        }
        if (mSecondaryStatusBar != null) {
            mSecondaryStatusBar.setImeWindowStatus(displayId, vis, backDisposition, showImeSwitcher);
        }
    }
}
