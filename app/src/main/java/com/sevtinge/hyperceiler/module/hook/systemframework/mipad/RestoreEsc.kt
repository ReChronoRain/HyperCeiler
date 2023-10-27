package com.sevtinge.hyperceiler.module.hook.systemframework.mipad

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object RestoreEsc : BaseHook() {
    override fun init() {
        loadClass("com.android.server.input.config.InputCommonConfig").methodFinder().first {
            name == "setPadMode"
        }.createHook {
            before {
                it.args[0] = false
            }
        }

        loadClass("com.android.server.input.InputManagerServiceStubImpl").methodFinder().first {
            name == "switchPadMode"
        }.createHook {
            before {
                it.args[0] = false
            }
        }
    }
}
