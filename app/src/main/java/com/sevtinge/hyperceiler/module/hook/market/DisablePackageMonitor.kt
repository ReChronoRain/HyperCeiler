package com.sevtinge.hyperceiler.module.hook.market

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*

object DisablePackageMonitor : BaseHook() {

    override fun init() {
        // 使用root, adb, packageinstaller安装应用后, 应用商店有后台时会上传检查应用更新信息
        val initMethod = findClass("com.xiaomi.market.receiver.MyPackageMonitor").getMethod("init")
        initMethod.createHook {
            logD(TAG, lpparam.packageName, "FindAndHook 'init' method: $initMethod")
            replace { }
        }
    }

}