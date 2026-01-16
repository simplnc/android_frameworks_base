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

package com.android.systemui.statusbar.notification.lineage.collection.render

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.res.R
import com.android.systemui.statusbar.notification.collection.render.NodeController
import com.android.systemui.statusbar.notification.collection.render.SectionHeaderController
import com.android.systemui.statusbar.notification.lineage.collection.provider.EssentialProvider
import com.android.systemui.statusbar.notification.lineage.stack.EssentialSectionHeaderView
import com.android.systemui.statusbar.notification.stack.SectionHeaderView
import javax.inject.Inject

/**
 * Controller for the essential notifications section header
 */
class EssentialSectionHeaderController @Inject constructor(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val activityStarter: ActivityStarter,
    private val essentialProvider: EssentialProvider
) : SectionHeaderController, NodeController {

    companion object {
        const val NODE_LABEL = "essential header"
    }

    override val nodeLabel: String = NODE_LABEL

    override val view: View
        get() = headerView ?: throw IllegalStateException("Header view not inflated yet")

    private var headerView: EssentialSectionHeaderView? = null
    private var clearSectionListener: View.OnClickListener? = null

    override fun reinflateView(parent: ViewGroup) {
        headerView?.let { parent.removeView(it) }

        val view = layoutInflater.inflate(
            R.layout.status_bar_notification_essential_section_header,
            parent,
            false
        ) as EssentialSectionHeaderView

        headerView = view
        parent.addView(view)

        // Set up clear all button listener - either use provided listener or default clear all
        val listener = clearSectionListener ?: View.OnClickListener {
            essentialProvider.clearAllEssential()
        }
        view.setOnClearSectionClickListener(listener)
    }

    override val headerView: SectionHeaderView?
        get() = this.headerView

    override fun setClearSectionEnabled(enabled: Boolean) {
        headerView?.setClearSectionEnabled(enabled)
    }

    override fun setOnClearSectionClickListener(listener: View.OnClickListener) {
        clearSectionListener = listener
        headerView?.setOnClearSectionClickListener(listener)
    }
}