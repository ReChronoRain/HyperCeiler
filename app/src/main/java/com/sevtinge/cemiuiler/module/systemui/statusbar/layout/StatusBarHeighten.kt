package com.sevtinge.cemiuiler.module.systemui.statusbar.layout

import com.sevtinge.cemiuiler.module.base.BaseHook

object StatusBarHeighten : BaseHook() {
    override fun init() {
        val opt = mPrefsMap.getInt("system_ui_statusbar_heighten", 19)
        val heightDpi = if (opt == 19) 27.0f else opt.toFloat()
        try {
            mResHook.setDensityReplacement("*", "dimen", "status_bar_height_default", heightDpi)
            mResHook.setDensityReplacement("*", "dimen", "status_bar_height", heightDpi)
            mResHook.setDensityReplacement("*", "dimen", "status_bar_height_portrait", heightDpi)
            mResHook.setDensityReplacement("*", "dimen", "status_bar_height_landscape", heightDpi)
        } catch (_: Throwable) {
        }
    }
}
