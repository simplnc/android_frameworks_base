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

package com.android.systemui.statusbar.phone.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.DualStatusBarView;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.ui.StatusBarIconController;
import com.android.systemui.statusbar.phone.ui.DarkIconManager;

/**
 * Fragment that manages the dual status bar layout
 * This replaces the single status bar with two status bars for enhanced information display
 */
@SuppressLint("ValidFragment")
public class DualStatusBarFragment extends Fragment implements CommandQueue.Callbacks {
    private static final String TAG = "DualStatusBarFragment";
    
    private DualStatusBarView mDualStatusBar;
    private PhoneStatusBarView mPrimaryStatusBar;
    private PhoneStatusBarView mSecondaryStatusBar;
    private StatusBarIconController mStatusBarIconController;
    private DarkIconManager mDarkIconManager;
    private final CommandQueue mCommandQueue;
    
    public DualStatusBarFragment(CommandQueue commandQueue) {
        mCommandQueue = commandQueue;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDualStatusBar = (DualStatusBarView) inflater.inflate(
                com.android.systemui.res.R.layout.dual_status_bar, container, false);
        
        mPrimaryStatusBar = mDualStatusBar.getPrimaryStatusBar();
        mSecondaryStatusBar = mDualStatusBar.getSecondaryStatusBar();
        
        return mDualStatusBar;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize icon controllers for both status bars
        if (mPrimaryStatusBar != null) {
            // Initialize primary status bar icons
            initializeStatusBarIcons(mPrimaryStatusBar, true);
        }
        
        if (mSecondaryStatusBar != null) {
            // Initialize secondary status bar icons
            initializeStatusBarIcons(mSecondaryStatusBar, false);
        }
    }
    
    private void initializeStatusBarIcons(PhoneStatusBarView statusBar, boolean isPrimary) {
        // This would be initialized by the StatusBarIconController
        // For now, we'll just set up the basic structure
    }
    
    public DualStatusBarView getDualStatusBar() {
        return mDualStatusBar;
    }
    
    public PhoneStatusBarView getPrimaryStatusBar() {
        return mPrimaryStatusBar;
    }
    
    public PhoneStatusBarView getSecondaryStatusBar() {
        return mSecondaryStatusBar;
    }
    
    // Implement CommandQueue.Callbacks
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
