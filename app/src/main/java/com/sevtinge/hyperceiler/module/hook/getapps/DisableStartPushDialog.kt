package com.sevtinge.hyperceiler.module.hook.getapps

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*


object DisableStartPushDialog : BaseHook() {
    override fun init() {
        // 禁用开启推送弹窗
        loadClass("com.xiaomi.market.util.NotificationUtils").methodFinder()
            .filterByName("isNotificationEnabled")
            .filterByParamTypes(String::class.java)
            .first().createHook {
                returnConstant(true)
            }
        loadClass("com.xiaomi.market.util.SettingsUtils").methodFinder()
            .filterByName("shouldNotifyUpdate")
            .first().createHook {
                returnConstant(true)
            }
    }
}