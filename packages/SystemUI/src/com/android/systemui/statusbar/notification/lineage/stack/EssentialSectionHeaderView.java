/*
 * Copyright (C) 2024 The LineageOS Project
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

package com.android.systemui.statusbar.notification.lineage.stack;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.notification.stack.SectionHeaderView;

/**
 * Header view for the essential notifications section
 */
public class EssentialSectionHeaderView extends FrameLayout implements SectionHeaderView {

    private ImageView mClearAllButton;
    private View.OnClickListener mClearSectionClickListener;

    public EssentialSectionHeaderView(Context context) {
        this(context, null);
    }

    public EssentialSectionHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EssentialSectionHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EssentialSectionHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mClearAllButton = findViewById(R.id.btn_clear_all);
        if (mClearAllButton != null) {
            mClearAllButton.setOnClickListener(v -> {
                if (mClearSectionClickListener != null) {
                    mClearSectionClickListener.onClick(v);
                }
            });
        }
    }

    public void setOnClearSectionClickListener(View.OnClickListener listener) {
        mClearSectionClickListener = listener;
    }

    public void setClearSectionEnabled(boolean enabled) {
        if (mClearAllButton != null) {
            mClearAllButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean isHeaderVisible() {
        return getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean isHeaderDismissable() {
        return false; // Essential header should not be dismissable
    }
}