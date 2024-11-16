package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public

import com.sevtinge.hyperceiler.module.base.BaseHook.*

object MobilePrefs {
    // 初始化开关
    val getLocation by lazy {
        // 显示在信号左侧
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_left")
    }
    val bold by lazy {
        // 加粗
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_bold")
    }
    val fontSize by lazy {
        // 字体大小
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_font_size", 27)
    }
    val leftMargin by lazy {
        // 左侧间距
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_left_margin", 0)
    }
    val rightMargin by lazy {
        // 右侧间距
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_right_margin", 0)
    }
    val verticalOffset by lazy {
        // 上下偏移量
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_vertical_offset", 8)
    }
}