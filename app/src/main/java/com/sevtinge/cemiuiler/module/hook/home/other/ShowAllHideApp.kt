package com.sevtinge.cemiuiler.module.hook.home.other

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.hook.home.HomeDexKit

object ShowAllHideApp : BaseHook() {
    override fun init() {
        val result =
            HomeDexKit.mHomeResultClassMap!!["HideAllApp"]

        result!!.map {
            it.getClassInstance(EzXHelper.classLoader).methodFinder().first {
                name == "isHideAppValid"
            }.createHook {
                returnConstant(true)
            }
        }
    }
}
