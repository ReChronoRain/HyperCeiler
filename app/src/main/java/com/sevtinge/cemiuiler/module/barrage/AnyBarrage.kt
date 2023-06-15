package com.sevtinge.cemiuiler.module.barrage


import android.service.notification.StatusBarNotification
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook


object AnyBarrage : BaseHook() {
    override fun init() {
        loadClass("com.xiaomi.barrage.service.NotificationMonitorService").methodFinder()
            .filterByName("filterNotification")
            .first().createHook {
                before { param ->
                    val statusBarNotification =
                        param.args[0] as StatusBarNotification
                    getObjectOrNullAs<ArrayList<String>>(
                        param.thisObject,
                        "mBarragePackageList"
                    )!!.let {
                        if (!it.contains(statusBarNotification.packageName)) {
                            it.add(statusBarNotification.packageName)
                        }
                    }
                }
            }
    }
}
