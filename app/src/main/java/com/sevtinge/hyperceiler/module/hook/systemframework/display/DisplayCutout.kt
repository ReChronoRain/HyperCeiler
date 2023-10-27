package com.sevtinge.hyperceiler.module.hook.systemframework.display

import com.sevtinge.hyperceiler.module.base.BaseHook

object DisplayCutout : BaseHook() {
    override fun init() {
        hookAllMethods("android.view.DisplayCutout", "pathAndDisplayCutoutFromSpec",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.args[0] = "M 0,0 H 0 V 0 Z"
                    param.args[1] = ""
                }
            }
        )
    }
}
