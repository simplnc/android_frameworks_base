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

package com.android.systemui.dynamicisland

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.systemui.R
import com.android.systemui.dagger.SysUISingleton
import javax.inject.Inject

/**
 * Dynamic Island notification overlay that displays notifications in a floating pill-shaped UI
 * similar to Apple's Dynamic Island.
 */
@SysUISingleton
class DynamicIslandNotificationView @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "DynamicIslandView"
        private const val DISPLAY_DURATION_MS = 3000L
        private const val ANIMATION_DURATION_MS = 300L
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val inflater = LayoutInflater.from(context)
    
    private var overlayView: View? = null
    private var isVisible = false
    private var hideRunnable: Runnable? = null

    /**
     * Show a notification in the Dynamic Island
     */
    fun showNotification(
        icon: android.graphics.drawable.Drawable?,
        text: String,
        onTap: (() -> Unit)? = null
    ) {
        Log.d(TAG, "Showing notification: $text")
        
        // Remove any existing overlay
        hideNotification()
        
        // Create new overlay
        overlayView = inflater.inflate(R.layout.dynamic_island_notification, null)
        
        // Setup content
        val iconView = overlayView?.findViewById<ImageView>(R.id.dynamic_island_icon)
        val textView = overlayView?.findViewById<TextView>(R.id.dynamic_island_text)
        
        iconView?.setImageDrawable(icon)
        textView?.text = text
        
        // Setup tap listener
        overlayView?.setOnClickListener {
            onTap?.invoke()
            hideNotification()
        }
        
        // Setup window parameters
        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            y = 100 // Position below status bar
        }
        
        // Add to window manager
        try {
            windowManager.addView(overlayView, params)
            isVisible = true
            
            // Animate in
            overlayView?.alpha = 0f
            overlayView?.scaleX = 0.8f
            overlayView?.scaleY = 0.8f
            overlayView?.animate()
                ?.alpha(1f)
                ?.scaleX(1f)
                ?.scaleY(1f)
                ?.setDuration(ANIMATION_DURATION_MS)
                ?.start()
            
            // Auto-hide after duration
            hideRunnable = Runnable { hideNotification() }
            handler.postDelayed(hideRunnable!!, DISPLAY_DURATION_MS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show Dynamic Island notification", e)
        }
    }
    
    /**
     * Hide the current notification
     */
    fun hideNotification() {
        if (!isVisible || overlayView == null) return
        
        Log.d(TAG, "Hiding notification")
        
        // Cancel auto-hide
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = null
        
        // Animate out
        overlayView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(ANIMATION_DURATION_MS)
            ?.withEndAction {
                try {
                    windowManager.removeView(overlayView)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove Dynamic Island view", e)
                }
                overlayView = null
                isVisible = false
            }
            ?.start()
    }
    
    /**
     * Check if Dynamic Island is currently visible
     */
    fun isVisible(): Boolean = isVisible
    
    /**
     * Cleanup resources
     */
    fun destroy() {
        hideNotification()
        handler.removeCallbacksAndMessages(null)
    }
}
