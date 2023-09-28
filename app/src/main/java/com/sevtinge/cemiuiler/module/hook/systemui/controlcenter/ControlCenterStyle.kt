package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object ControlCenterStyle : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl").declaredConstructors.createHooks {
            after {
                setObject(it.thisObject, "forceUseControlCenterPanel", false)
            }
        }
        loadClass("com.miui.systemui.SettingsObserver").methodFinder()
            .filterByName("setValue\$default").first()
            .createHook {
                before {
                    if (it.args[1] == "force_use_control_panel") {
                        it.args[2] = 0
                    }
                }
            }
    }
}
