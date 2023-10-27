package com.sevtinge.hyperceiler.module.hook.home.title

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.getObjectField

object BigIconCorner : BaseHook() {
    override fun init() {
        val maMlHostViewClass = loadClass("com.miui.home.launcher.maml.MaMlHostView")

        // if (!mPrefsMap.getBoolean("big_icon_corner")) return
        loadClass("com.miui.home.launcher.bigicon.BigIconUtil").methodFinder().filter {
            name == "getCroppedFromCorner" && parameterCount == 4
        }.toList().createHooks {
            before {
                it.args[0] = 2
                it.args[1] = 2
            }
        }

        maMlHostViewClass.methodFinder().first {
            name == "getCornerRadius"
        }.createHook {
            before {
                it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
            }
        }

        maMlHostViewClass.methodFinder().first {
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
