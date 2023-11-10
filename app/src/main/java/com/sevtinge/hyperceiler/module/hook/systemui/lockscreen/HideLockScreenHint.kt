package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

object HideLockScreenHint : BaseHook() {
    override fun init() {
        val hook: MethodHook = object : MethodHook() {
            @Throws(Throwable::class)
            override fun before(param: MethodHookParam) {
                XposedHelpers.setObjectField(param.thisObject, "mUpArrowIndication", null)
            }
        }

        findAndHookMethod(
            "com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController",
                lpparam.classLoader,
            "hasIndicationsExceptResting",
            XC_MethodReplacement.returnConstant(true)
        )
    }
}
