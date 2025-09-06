/*
 * Copyright (C) 2024 LineageOS
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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Island notification view - iOS Dynamic Island inspired
 * Shows compact notification summaries with expandable details
 */
class IslandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val iconView: ImageView
    private val titleView: TextView
    private val contentView: TextView
    
    private var isExpanded = false
    private var notificationData: IslandNotificationData? = null
    
    data class IslandNotificationData(
        val icon: Drawable?,
        val title: String,
        val content: String,
        val packageName: String
    )
    
    init {
        // Create views programmatically instead of using layout inflation
        iconView = ImageView(context)
        titleView = TextView(context)
        contentView = TextView(context)
        
        // Set up layout parameters
        iconView.layoutParams = LayoutParams(24, 24)
        titleView.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        contentView.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        
        // Add views to this container
        addView(iconView)
        addView(titleView)
        addView(contentView)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        setOnClickListener {
            if (isExpanded) {
                collapse()
            } else {
                expand()
            }
        }
        
        setOnLongClickListener {
            // Show full notification details
            showFullDetails()
            true
        }
    }
    
    fun setNotificationData(data: IslandNotificationData) {
        notificationData = data
        
        iconView.setImageDrawable(data.icon)
        titleView.text = data.title
        contentView.text = data.content
        
        // Show brief summary initially
        showSummary()
    }
    
    private fun showSummary() {
        isExpanded = false
        contentView.visibility = View.GONE
        titleView.text = notificationData?.title ?: ""
    }
    
    private fun expand() {
        isExpanded = true
        contentView.visibility = View.VISIBLE
        contentView.text = notificationData?.content ?: ""
        
        // Animate expansion
        animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
    }
    
    private fun collapse() {
        isExpanded = false
        contentView.visibility = View.GONE
        
        // Animate collapse
        animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
    }
    
    private fun showFullDetails() {
        // TODO: Implement full notification details view
        // This would show the complete notification with actions
    }
    
    fun dismiss() {
        animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(300)
            .withEndAction {
                visibility = View.GONE
            }
            .start()
    }
}
