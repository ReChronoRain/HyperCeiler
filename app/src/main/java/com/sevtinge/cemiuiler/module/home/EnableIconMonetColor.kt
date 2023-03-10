package com.sevtinge.cemiuiler.module.home

import android.content.res.Resources
import android.graphics.Color
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.ResourcesHookRegister
import de.robv.android.xposed.callbacks.XC_InitPackageResources

class EnableIconMonetColor : BaseHook() {
    private lateinit var resparam: XC_InitPackageResources.InitPackageResourcesParam
    override fun init() {
        val monet = "system_accent1_100"
        val monoColorId = Resources.getSystem().getIdentifier(monet, "color", "android")
        var monoColor = Resources.getSystem().getColor(monoColorId)
//        hasEnable("use_edit_color") {
//            monoColor = Color.parseColor("your_color")
//        }
        getInitPackageResourcesParam().res.setReplacement(
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

    protected fun getInitPackageResourcesParam(): XC_InitPackageResources.InitPackageResourcesParam {
        if (!this::resparam.isInitialized) {
            throw RuntimeException("resparam should be initialized")
        }
        return resparam
    }
}