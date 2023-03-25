package com.sevtinge.cemiuiler.module.wini.model

import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getBoolean
import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getInt
import com.sevtinge.cemiuiler.utils.woobox.XSPUtils.getString


// 智能助理
data class PersonalAssistantModel(
    var background: BaseBlurBackgroundModel = BaseBlurBackgroundModel(
        getBoolean("pa_enable", true),
        getInt("personalAssistant_blurRadius",80),
        getString("personalAssistant_color","#1E000000")!!,
    )
)