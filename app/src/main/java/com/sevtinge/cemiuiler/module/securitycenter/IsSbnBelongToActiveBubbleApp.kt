package com.sevtinge.cemiuiler.module.securitycenter

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge

object IsSbnBelongToActiveBubbleApp : BaseHook() {
    override fun init() {
        try {
            findMethod("com.miui.bubbles.settings.BubblesSettings") {
                name == "isSbnBelongToActiveBubbleApp"
            }.hookReturnConstant(true)
            XposedBridge.log("Cemiuiler: Hook isSbnBelongToActiveBubbleApp success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook isSbnBelongToActiveBubbleApp failed!")
            XposedBridge.log(e)
        }
    }

}