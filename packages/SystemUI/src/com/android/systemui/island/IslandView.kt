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

package com.android.systemui.island

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.android.systemui.R

/**
 * Minimal Dynamic Island notification view
 * Safe implementation that won't break builds
 */
class IslandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "IslandView"
    }
    
    private var iconView: ImageView? = null
    private var titleView: TextView? = null
    private var textView: TextView? = null
    
    init {
        initView()
    }
    
    private fun initView() {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        
        // Set background
        background = ContextCompat.getDrawable(context, R.drawable.island_background)
        
        // Set basic dimensions (using existing resources)
        minimumWidth = 48 // dp equivalent
        minimumHeight = 48 // dp equivalent
        
        // Add padding
        setPadding(12, 8, 12, 8)
        
        // Create icon view
        iconView = ImageView(context).apply {
            layoutParams = LayoutParams(48, 48).apply {
                marginEnd = 8
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            addView(this)
        }
        
        // Create text container
        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(this)
        }
        
        // Create title view
        titleView = TextView(context).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            textContainer.addView(this)
        }
        
        // Create text view
        textView = TextView(context).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.white).let { 
                (it and 0x00FFFFFF) or 0xCC000000 
            })
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            textContainer.addView(this)
        }
        
        // Initially hidden
        visibility = GONE
        alpha = 0f
    }
    
    /**
     * Show notification in the island
     */
    fun showNotification(title: String, text: String) {
        Log.d(TAG, "Showing notification: $title")
        
        titleView?.text = title
        textView?.text = text
        
        // Show with simple animation
        show()
    }
    
    /**
     * Hide the island
     */
    fun hide() {
        if (visibility == GONE) return
        
        Log.d(TAG, "Hiding island")
        visibility = GONE
        alpha = 0f
    }
    
    /**
     * Show the island with simple animation
     */
    private fun show() {
        visibility = VISIBLE
        animate().alpha(1f).setDuration(300).start()
    }
    
    /**
     * Check if island is currently visible
     */
    fun isVisible(): Boolean = visibility == VISIBLE
}
