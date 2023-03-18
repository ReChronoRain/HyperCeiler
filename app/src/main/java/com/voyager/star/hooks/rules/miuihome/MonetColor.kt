package com.voyager.star.hooks.rules.miuihome

import android.content.res.Resources
import android.graphics.Color
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.base.BaseXposedInit.mPrefsMap
import com.sevtinge.cemiuiler.utils.Settings.getInt
import com.voyager.star.utils.ResourcesHookRegister
import com.voyager.star.utils.XSPUtils
import com.voyager.star.utils.XSPUtils.getString
import com.voyager.star.utils.hasEnable

object MonetColor : ResourcesHookRegister() {
    override fun init() {
        if (!mPrefsMap.getBoolean("home_other_icon_monet_color"))
            return
        val monet = "system_accent1_100"
        val monoColorId = Resources.getSystem().getIdentifier(monet, "color", "android")
        var monoColor = Resources.getSystem().getColor(monoColorId)
        if (mPrefsMap.getBoolean("home_other_use_edit_color")) {
            monoColor = BaseHook.mPrefsMap.getInt("home_other_your_color_qwq", -1)
        }
        getInitPackageResourcesParam().res.setReplacement(
            "com.miui.home",
            "color",
            "monochrome_default",
            monoColor
        )
    }
}