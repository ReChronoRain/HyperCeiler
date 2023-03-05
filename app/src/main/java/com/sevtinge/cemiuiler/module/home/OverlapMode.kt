package com.sevtinge.cemiuiler.module.home

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

class OverlapMode : BaseHook(){
    override fun init() {
        //Fold2样式负一屏
        findMethod("com.miui.home.launcher.overlay.assistant.AssistantDeviceAdapter") {
            name == "inOverlapMode"
        }.hookReturnConstant(true)
    }
}