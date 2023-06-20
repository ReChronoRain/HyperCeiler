package com.sevtinge.cemiuiler.module.systemui.lockscreen

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import com.sevtinge.cemiuiler.utils.devicesdk.isMoreAndroidVersion
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

        if (isMoreAndroidVersion(33)) {
            Helpers.findAndHookMethod(
                "com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController",
                lpparam.classLoader,
                "hasIndicationsExceptResting",
                XC_MethodReplacement.returnConstant(true)
            )
        } else {
            Helpers.findAndHookMethod(
                "com.android.systemui.statusbar.KeyguardIndicationController",
                lpparam.classLoader,
                "updateIndication",
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                hook
            )
        }
    }
}
