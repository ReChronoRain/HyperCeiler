package com.sevtinge.cemiuiler.module.systemui.statusbar.layout

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.XSPUtils.getInt

object StatusBarIconSize : BaseHook() {
    override fun init() {
        val icons = getInt("system_ui_statusbar_icon_size", 12)
        if (icons > 12) {
            val iconSize = icons * 0.5f
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_icon_size", iconSize)
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_clock_size", iconSize + 0.4f)
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_icon_drawing_size", iconSize)
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_icon_drawing_size_dark", iconSize)
            val notifyPadding = 2.5f * iconSize / 13
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_notification_icon_padding", notifyPadding)
            val iconHeight = 20.5f * iconSize / 13
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_icon_height", iconHeight)
        }
    }
}
