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

package com.android.systemui.statusbar.notification.lineage.stack

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import com.android.systemui.R
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView

/**
 * Delegate for drawing essential notification backgrounds with special styling
 */
class EssentialSectionBackgroundDelegate(
    private val context: Context
) {

    private val cornerRadius = context.resources.getDimensionPixelSize(
        R.dimen.essential_section_corner_radius
    ).toFloat()

    private val sectionPadding = context.resources.getDimensionPixelSize(
        R.dimen.essential_section_padding
    ).toFloat()

    private val path = Path()
    private val rect = RectF()

    /**
     * Draw the essential notification background
     */
    fun drawBackground(canvas: Canvas, view: ActivatableNotificationView) {
        val width = view.width.toFloat()
        val height = view.height.toFloat()

        // Create rounded rectangle path
        rect.set(0f, 0f, width, height)
        path.reset()
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)

        // Clip to the rounded rectangle
        canvas.save()
        canvas.clipPath(path)

        // Draw background color (this would be handled by the view's normal background drawing)

        canvas.restore()
    }

    /**
     * Get the padding for essential notifications
     */
    fun getPadding(): Float = sectionPadding
}