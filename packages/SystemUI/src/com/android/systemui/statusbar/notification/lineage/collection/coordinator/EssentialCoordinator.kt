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

package com.android.systemui.statusbar.notification.lineage.collection.coordinator

import com.android.systemui.statusbar.notification.collection.Coordinator
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.notification.collection.coordinator.CoordinatorParameter
import com.android.systemui.statusbar.notification.collection.listbuilder.pluggable.NotifSectioner
import com.android.systemui.statusbar.notification.collection.render.NodeController
import com.android.systemui.statusbar.notification.collection.render.NodeController
import com.android.systemui.statusbar.notification.lineage.collection.provider.EssentialProvider
import com.android.systemui.statusbar.notification.lineage.collection.render.EssentialSectionHeaderController
import com.android.systemui.statusbar.notification.stack.BUCKET_ESSENTIAL
import javax.inject.Inject

/**
 * Coordinator for essential notifications
 */
class EssentialCoordinator @Inject constructor(
    private val essentialProvider: EssentialProvider,
    private val essentialSectioner: EssentialSectioner
) : Coordinator {

    val essentialSectioner: NotifSectioner = essentialSectioner

    override fun attach(parameter: CoordinatorParameter) {
        // Attach to the notification pipeline
    }

    override fun getSectioner(): NotifSectioner? = essentialSectioner
}

/**
 * Sectioner for essential notifications
 */
class EssentialSectioner @Inject constructor(
    private val essentialProvider: EssentialProvider,
    private val essentialHeaderController: EssentialSectionHeaderController
) : NotifSectioner("EssentialSectioner", BUCKET_ESSENTIAL) {

    override fun isInSection(entry: NotificationEntry): Boolean {
        return essentialProvider.isEssentialNotification(entry)
    }

    override fun getHeaderNodeController(): NodeController {
        return essentialHeaderController
    }

    override fun getComparator(): Comparator<NotificationEntry>? {
        // Essential notifications use default ordering
        return null
    }
}