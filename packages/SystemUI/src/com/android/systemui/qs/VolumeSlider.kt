/*
 * Copyright (C) 2024 the risingOS Android Project
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
package com.android.systemui.qs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.lang.reflect.Field
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.android.systemui.res.R

class VolumeSlider(context: Context, attrs: AttributeSet? = null) : VerticalSlider(context, attrs) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var volumeIcon: ImageView? = null
    private val handler = Handler()
    private var isUserAdjusting = false
    private var currentVolumePercent = 0
    
    // Paint for percentage text
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = 24f
        color = Color.WHITE
    }
    private val volumeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.VOLUME_CHANGED_ACTION && !isUserAdjusting) {
                updateVolumeProgress()
            }
        }
    }
    
    init {
        setupUserInteractionListener()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        volumeIcon = findViewById(R.id.qs_controls_volume_slider_icon)
        volumeIcon?.bringToFront()
        updateProgressRect()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter(AudioManager.VOLUME_CHANGED_ACTION)
        context.registerReceiver(volumeChangeReceiver, filter)
        updateVolumeProgress()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(volumeChangeReceiver)
    }
    
    private fun setupUserInteractionListener() {
        addUserInteractionListener(object : UserInteractionListener {
            override fun onUserInteractionEnd() {
                handler.postDelayed({ isUserAdjusting = false }, 200)
            }
            override fun onLongPress() {
                toggleMute()
            }
            override fun onUserSwipe() {
                isUserAdjusting = true
                setVolumeFromProgress()
                val volHapticsIntensity = Settings.System.getIntForUser(context.getContentResolver(),
                        "volume_slider_haptics_intensity", 1, UserHandle.USER_CURRENT)
                performSliderHaptics(volHapticsIntensity)
            }
        })
    }
    
    private fun updateVolumeProgress() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newProgress = (currentVolume * 100 / maxVolume)
        currentVolumePercent = newProgress
        setSliderProgress(newProgress)
        updateVolumeColor(newProgress)
        invalidate() // Trigger redraw for percentage text
        progressRect.top = (1 - newProgress / 100f) * measuredHeight
        volumeIcon?.let { updateIconTint(it) }
        invalidate()
    }
    
    private fun updateVolumeColor(volumePercent: Int) {
        currentVolumePercent = volumePercent
        updateSliderPaint() // This will call our overridden method
        invalidate() // Force redraw with new color
    }
    
    override fun updateSliderPaint() {
        super.updateSliderPaint() // Call parent first
        
        // Override progress color based on volume level
        val volumeColor = when {
            currentVolumePercent == 0 -> Color.GRAY           // 🔇 0% (Muted)
            currentVolumePercent <= 29 -> Color.GREEN         // 🔉 1-29% (Low) 
            currentVolumePercent <= 69 -> Color.BLUE          // 🔊 30-69% (Medium)
            else -> Color.rgb(255, 165, 0)                   // 🔊 70-100% (High) - Orange
        }
        
        // Use reflection to access private progressPaint or create our own
        try {
            val field = javaClass.superclass.getDeclaredField("progressPaint")
            field.isAccessible = true
            val paint = field.get(this) as Paint
            paint.color = volumeColor
        } catch (e: Exception) {
            // Fallback: the parent updateSliderPaint will handle it
        }
        
        // Update volume icon tint
        volumeIcon?.let { updateIconTint(it) }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw percentage text in the center of the slider
        val centerX = width / 2f
        val centerY = height / 2f
        val percentText = "${currentVolumePercent}%"
        
        // Draw text with shadow for better visibility
        textPaint.color = Color.BLACK
        canvas.drawText(percentText, centerX + 2f, centerY + 2f, textPaint)
        textPaint.color = Color.WHITE
        canvas.drawText(percentText, centerX, centerY, textPaint)
    }
    
    private fun setVolumeFromProgress() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = progress * maxVolume / 100
        currentVolumePercent = progress
        updateVolumeColor(progress)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        invalidate() // Trigger redraw for updated percentage
        setSliderProgress((volume * 100 / maxVolume))
        updateProgressRect()
    }
    
    private fun toggleMute() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVolume == 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 4, 0)
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        }
        updateVolumeProgress()
    }

    override fun updateProgressRect() {
        super.updateProgressRect()
        volumeIcon?.let { updateIconTint(it) }
    }
}
