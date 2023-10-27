package com.sevtinge.hyperceiler.module.hook.systemframework.mipad

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object SetGestureNeedFingerNum : BaseHook() {
    override fun init() {
        loadClass("com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture").methodFinder()
            .first {
                name == "getFunctionNeedFingerNum"
            }.createHook {
            returnConstant(4)
        }
    }
}
