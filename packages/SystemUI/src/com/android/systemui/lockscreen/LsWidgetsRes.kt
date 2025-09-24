package com.android.systemui.lockscreen

import com.android.systemui.res.R

object LsWidgetsRes {
    val WIDGETS_VIEW_IDS = intArrayOf(
        R.id.kg_item_placeholder1,
        R.id.kg_item_placeholder2,
        R.id.kg_item_placeholder3,
        R.id.kg_item_placeholder4
    )
    val BT_ACTIVE = R.drawable.qs_bluetooth_icon_on
    val BT_INACTIVE = R.drawable.qs_bluetooth_icon_off
    val RINGER_ACTIVE = R.drawable.ic_vibration_24
    val RINGER_INACTIVE = R.drawable.ic_ring_volume_24
    val TORCH_RES_ACTIVE = R.drawable.ic_flashlight_on
    val TORCH_RES_INACTIVE = R.drawable.ic_flashlight_off
    // Removed unused resources: DATA, WIFI, HOTSPOT
}
