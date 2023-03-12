package com.sevtinge.cemiuiler.module.home.title

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectField

object BigIconCorner : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("big_icon_corner")) return
        findMethod("com.miui.home.launcher.bigicon.BigIconUtil") {
            name == "getCroppedFromCorner" && parameterCount == 4
        }.hookBefore {
            it.args[0] = 2
            it.args[1] = 2
        }

        findMethod(
            "com.miui.home.launcher.maml.MaMlHostView"
        ) {
            name == "getCornerRadius"
        }.hookBefore {
            it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
        }

        findMethod("com.miui.home.launcher.maml.MaMlHostView") {
            name == "computeRoundedCornerRadius" && parameterCount == 1
        }.hookBefore {
            it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
        }

        findMethod("com.miui.home.launcher.LauncherAppWidgetHostView") {
            name == "computeRoundedCornerRadius" && parameterCount == 1
        }.hookBefore {
            it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
        }

    }
}