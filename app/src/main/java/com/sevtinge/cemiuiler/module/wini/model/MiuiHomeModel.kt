package com.sevtinge.cemiuiler.module.wini.model

import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getBoolean
import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getInt


// 基础模糊配置
data class MiuiHomeModel(
    var enableShortcutBackgroundBlur: Boolean = getBoolean("blur_when_show_shortcut_menu",false),
    var shortcutMenuBackgroundAlpha: Int = getInt("shortcutMenuBackgroundAlpha",120),
)