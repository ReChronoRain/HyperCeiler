package com.sevtinge.cemiuiler.module.wini.model

import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getBoolean
import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getInt


// 快捷设置
data class QuickSettingModel(
    var hideMiPlayEntry:Boolean = getBoolean("hideMiPlayEntry",false),
    var controlDetailBackgroundAlpha: Int = getInt("controlDetailBackgroundAlpha",120),
)