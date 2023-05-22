package com.sevtinge.cemiuiler.module.home.other

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodReplacement

class OverlapMode : BaseHook(){
    override fun init() {
        //Fold2样式负一屏
        findMethod("com.miui.home.launcher.overlay.assistant.AssistantDeviceAdapter") {
            name == "inOverlapMode"
        }.hookReturnConstant(true)

        /*findAndHookMethod(
            "com.miui.home.launcher.overlay.assistant.AssistantDeviceAdapter",
            "inOverlapMode",
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any {
                    return true
                }
            }
        )*/
    }
}