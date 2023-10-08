package com.sevtinge.cemiuiler.module.hook.home.other

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object ShowAllHideApp : BaseHook() {
    override fun init() {
        dexKitBridge.findClass {
            matcher {
                addUsingStringsEquals("appInfo.packageName", "activityInfo")
            }
        }.forEach {
            it.getInstance(EzXHelper.classLoader).methodFinder().first {
                name == "isHideAppValid"
            }.createHook {
                returnConstant(true)
            }
        }
    }
}
