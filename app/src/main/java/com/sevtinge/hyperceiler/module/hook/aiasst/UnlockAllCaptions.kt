package com.sevtinge.hyperceiler.module.hook.aiasst

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*

object UnlockAllCaptions : BaseHook() {
    override fun init() {
        // by PedroZ
        loadClass("com.xiaomi.aiasst.vision.common.BuildConfigUtils").methodFinder()
            .filterByName("isSupplierOnline")
            .single().createHook {
                returnConstant(true)
            }
    }
}