package com.sevtinge.cemiuiler.module.home.title

import android.annotation.SuppressLint
import android.content.res.Resources
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.base.BaseXposedInit
import de.robv.android.xposed.callbacks.XC_InitPackageResources

object EnableIconMonetColor : BaseHook() {

    override fun init() {}

    @SuppressLint("DiscouragedApi")
    fun initResource(resParam: XC_InitPackageResources.InitPackageResourcesParam) {
        val monet = "system_accent1_100"
        val monoColorId = Resources.getSystem().getIdentifier(monet, "color", "android")
        var monoColor = Resources.getSystem().getColor(monoColorId)
        if (BaseXposedInit.mPrefsMap.getBoolean("home_other_use_edit_color")) {
            monoColor = mPrefsMap.getInt("home_other_your_color_qwq", -1)
        }
        resParam.res.setReplacement(
            "com.miui.home",
            "color",
            "monochrome_default",
            monoColor
        )
//        val ColorEntriesId = Resources.getSystem().getStringArray()
//        val ColorEntries = Resources.getSystem().getStringArray(ColorEntriesId)
//        getInitPackageResourcesParam().res.setReplacement(
//            "com.miui.home",
//            "string",
//            ColorEntries.toString(),
//            "Monet"
//        )
    }
}
