package com.sevtinge.hyperceiler.module.hook.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.setObjectField

object DisableRecentViewWallpaperDarken : BaseHook() {
    override fun init() {

        // if (!mPrefsMap.getBoolean("home_disable_darken")) return
        loadClass("com.miui.home.recents.DimLayer").methodFinder().first {
            name == "dim" && parameterCount == 3
        }.createHook {
            before {
                it.args[0] = 0.0f
                it.thisObject.setObjectField("mCurrentAlpha", 0.0f)
            }
        }
    }
}
