package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public

import com.sevtinge.hyperceiler.module.base.BaseHook.*

object MobilePrefs {
    // 初始化开关
    val getLocation by lazy {
        // 单独显示-显示在信号左侧
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_left")
    }
    val showMobileType by lazy {
        // 网络类型单独显示
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable")
    }
    val bold by lazy {
        // 单独显示-加粗
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_bold")
    }
    val fontSize by lazy {
        // 单独显示-字体大小
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_font_size", 27)
    }
    val leftMargin by lazy {
        // 单独显示-左侧间距
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_left_margin", 0)
    }
    val rightMargin by lazy {
        // 单独显示-右侧间距
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_right_margin", 0)
    }
    val verticalOffset by lazy {
        // 单独显示-上下偏移量
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_vertical_offset", 8)
    }
    val mobileNetworkType by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_show_mobile_network_type", 0)
    }
    val hideIndicator by lazy {
        // 网络活动指示器
        mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator")
    }
    val hideRoaming by lazy {
        // 隐藏漫游图标
        mPrefsMap.getBoolean("system_ui_status_bar_mobile_hide_roaming_icon")
    }
    val isEnableDouble by lazy {
        // 双排信号
        mPrefsMap.getBoolean("system_ui_statusbar_network_icon_enable")
    }
    val card1 by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_1")
    }
    val card2 by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_2")
    }
}