package com.sevtinge.hyperceiler.module.hook.mtb

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object IsUserBuild : BaseHook() {
    override fun init() {
        loadClass("com.xiaomi.mtb.MtbUtils").methodFinder().first {
            name == "IsUserBuild"
        }.createHook {
            returnConstant(false)
        }
    }
}
