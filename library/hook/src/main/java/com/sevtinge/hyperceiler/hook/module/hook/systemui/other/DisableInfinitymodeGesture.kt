package com.sevtinge.hyperceiler.hook.module.hook.systemui.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion


object DisableInfinitymodeGesture : BaseHook() {
    override fun init() {
        if (isMoreAndroidVersion(35)) {
            loadClass("com.android.wm.shell.multitasking.miuiinfinitymode.MiuiInfinityModeSizesPolicy", lpparam.classLoader)
        } else {
            loadClass("com.android.wm.shell.miuifreeform.MiuiInfinityModeSizesPolicy", lpparam.classLoader)
        }.methodFinder().filterByName("isForbiddenWindow").single().createHook {
            returnConstant(true)
        }
    }
}
