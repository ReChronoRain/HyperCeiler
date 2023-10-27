package com.sevtinge.hyperceiler.module.hook.home.mipad

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.getObjectField

object EnableMoreSetting : BaseHook() {
    override fun init() {
        loadClass("com.miui.home.settings.MiuiHomeSettings").methodFinder().first{
            name == "checkDevice"
        }.createHook{
            returnConstant(true)
        }

        loadClass("com.miui.home.launcher.DeviceConfig").methodFinder().first{
            name == "needShowCellsEntry"
        }.createHook{
            returnConstant(true)
        }

        loadClass("com.miui.home.launcher.LauncherMenu").methodFinder().first{
            name == "onShow"
        }.createHook{
            after{
                val mDefaultScreenPreview = it.thisObject.getObjectField("mDefaultScreenPreview") as View
                mDefaultScreenPreview.visibility = View.VISIBLE
            }
        }
    }
}
