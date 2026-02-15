package com.sevtinge.hyperceiler.libhook.rules.health

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object UnlockFoucsAuth : BaseHook() {
    override fun init() {

        loadClass("com.xiaomi.fitness.notify.util.NotificationFilterHelper").methodFinder()
            .filterByName("isNotificationSpotlightAppInWhiteList")
            .single().createHook {
                // 允许全部应用转发到手表
                returnConstant(true)
            }
    }
}
