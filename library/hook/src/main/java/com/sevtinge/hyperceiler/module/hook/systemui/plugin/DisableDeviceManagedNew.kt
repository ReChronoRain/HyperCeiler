package com.sevtinge.hyperceiler.module.hook.systemui.plugin

import android.app.admin.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.utils.*

object DisableDeviceManagedNew {
    @JvmStatic
    fun initDisableDeviceManaged(classLoader: ClassLoader) {
        val securityController by lazy {
            loadClass("miui.systemui.controlcenter.policy.SecurityController", classLoader)
        }

        securityController.constructorFinder()
            .filterByParamCount(5)
            .first().createBeforeHook {
                it.thisObject.setObjectField("hasCACerts", null)
            }

        DevicePolicyManager::class.java.methodFinder()
            .filterByName("isDeviceManaged")
            .first().createHook {
                returnConstant(false)
            }

        securityController.methodFinder()
            .filterByName("isDeviceManaged")
            .first().createHook {
                returnConstant(false)
            }

        securityController.methodFinder()
            .filterByName("hasCACertInCurrentUser")
            .first().createHook {
                returnConstant(false)
            }

        securityController.methodFinder()
            .filterByName("hasCACertInWorkProfile")
            .first().createHook {
                returnConstant(false)
            }
    }
}
