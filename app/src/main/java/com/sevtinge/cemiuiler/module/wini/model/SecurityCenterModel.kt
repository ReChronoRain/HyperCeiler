package com.sevtinge.cemiuiler.module.wini.model

import com.sevtinge.cemiuiler.utils.XSPUtils.getBoolean
import com.sevtinge.cemiuiler.utils.XSPUtils.getInt
import com.sevtinge.cemiuiler.utils.XSPUtils.getString


// 手机管家
data class SecurityCenterModel(
    var dockBackground: BaseBlurBackgroundModel = BaseBlurBackgroundModel(
        getBoolean("se_enable",true),
        getInt("security_blurRadius",60),
        getString("security_color", "#3C000000")!!,
    )
)