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

package com.android.systemui.statusbar.policy

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.android.systemui.R
import com.android.systemui.statusbar.policy.Clock

class CustomClockViewStub @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mTimeView: TextView? = null
    private var mDateView: TextView? = null
    private var mClockStyle: Int = 0
    private var mSystemClock: Clock? = null

    fun setSystemClock(clock: Clock) {
        mSystemClock = clock
        syncWithSystemClock()
    }

    fun syncWithSystemClock() {
        mSystemClock?.let { systemClock ->
            mTimeView?.text = systemClock.text
            mDateView?.text = systemClock.contentDescription
        }
    }

    fun setClockStyle(style: Int) {
        if (mClockStyle == style) return
        mClockStyle = style
        updateClockLayout()
    }

    fun setTimeText(text: String) {
        mTimeView?.text = text
    }

    fun setDateText(text: String) {
        mDateView?.text = text
    }

    private fun updateClockLayout() {
        removeAllViews()
        
        val layoutRes = when (mClockStyle) {
            1 -> R.layout.qs_header_clock_simple
            2 -> R.layout.qs_header_clock_chip
            3 -> R.layout.qs_header_clock_analog
            4 -> R.layout.qs_header_clock_oos
            else -> return // Default style - hide custom clock
        }

        val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
        addView(view)

        mTimeView = findViewById(R.id.time)
        mDateView = findViewById(R.id.date)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        updateClockLayout()
    }
}
