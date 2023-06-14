package com.sevtinge.cemiuiler.module.systemui.statusbar.layout

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.XSPUtils.getInt

object StatusBarHeighten : BaseHook() {
    override fun init() {
        val opt = getInt("system_ui_statusbar_height", 19)
        val heightDpi = if (opt == 19) 27 else opt
        mResHook.setDensityReplacement("*", "dimen", "status_bar_height_default", heightDpi.toFloat())
        mResHook.setDensityReplacement("*", "dimen", "status_bar_height", heightDpi.toFloat())
        mResHook.setDensityReplacement("*", "dimen", "status_bar_height_portrait", heightDpi.toFloat())
        mResHook.setDensityReplacement("*", "dimen", "status_bar_height_landscape", heightDpi.toFloat())
    }
}
