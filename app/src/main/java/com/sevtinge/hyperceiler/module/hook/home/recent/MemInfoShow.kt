package com.sevtinge.hyperceiler.module.hook.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object MemInfoShow : BaseHook() {
    override fun init() {
        // hyperOS for Pad 修复方案来自 hyper helper
        try {
            // 此方法调用会将内存显示 hide，需拦截
            loadClass("com.miui.home.recents.views.RecentsDecorations").methodFinder().first {
                name == "hideTxtMemoryInfoView"
            }.createHook {
                returnConstant(null)
            }
        } catch (t: Throwable) {
            logE(TAG, "hideTxtMemoryInfoView method is null")
        }

        try {
            loadClass("com.miui.home.recents.views.RecentsDecorations").methodFinder().first {
                name == "isMemInfoShow"
            }
        } catch (t: Throwable) {
            loadClass("com.miui.home.recents.views.RecentsDecorations").methodFinder().first {
                name == "canTxtMemInfoShow"
            }
        }.createHook {
            returnConstant(true)
        }
    }
}
