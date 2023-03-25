package com.sevtinge.cemiuiler.module.wini.model


import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getBoolean
import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getInt

// 通知
data class NotificationBlurModel(
    var enable: Boolean = getBoolean("n_enable",true),
    var cornerRadius: Int = getInt("cornerRadius",48),
    var blurRadius: Int = getInt("blurRadius",99),
    var blurBackgroundAlpha: Int = getInt("blurBackgroundAlpha",100),
    var defaultBackgroundAlpha: Int = getInt("defaultBackgroundAlpha",200),
)