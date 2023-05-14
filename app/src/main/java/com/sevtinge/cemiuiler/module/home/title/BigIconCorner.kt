package com.sevtinge.cemiuiler.module.home.title

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectField

object BigIconCorner : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("big_icon_corner")) return

        loadClass("com.miui.home.launcher.bigicon.BigIconUtil").methodFinder().first {
            name == "getCroppedFromCorner" && parameterCount == 4
        }.createHook {
            before {
                it.args[0] = 2
                it.args[1] = 2
            }
        }

        loadClass("com.miui.home.launcher.maml.MaMlHostView").methodFinder().first {
            name == "getCornerRadius"
        }.createHook {
            before {
                it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
            }
        }

        loadClass("com.miui.home.launcher.maml.MaMlHostView").methodFinder().first {
            name == "computeRoundedCornerRadius" && parameterCount == 1
        }.createHook {
            before {
                it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
            }
        }

        loadClass("com.miui.home.launcher.LauncherAppWidgetHostView").methodFinder().first {
            name == "computeRoundedCornerRadius" && parameterCount == 1
        }.createHook {
            before {
                it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
            }
        }

    }
}