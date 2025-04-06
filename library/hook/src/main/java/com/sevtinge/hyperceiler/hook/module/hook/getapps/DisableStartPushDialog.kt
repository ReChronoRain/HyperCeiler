package com.sevtinge.hyperceiler.hook.module.hook.getapps

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook


object DisableStartPushDialog : BaseHook() {
    override fun init() {
        // 禁用开启推送弹窗
        loadClass("com.xiaomi.market.ui.UpdateListFragment").methodFinder()
            .filterByName("tryShowDialog")
            .first().createHook {
                interrupt()
            }
        loadClass("com.xiaomi.market.ui.update.UpdatePushDialogManager").methodFinder()
            .filterByName("tryShowDialog")
            .first().createHook {
                interrupt()
            }
    }
}
