package com.sevtinge.hyperceiler.module.hook.guardprovider

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.replaceMethod

object DisableUploadAppListNew : BaseHook() {

    @Throws(NoSuchMethodException::class)
    override fun init() {
        /*val antiDefraudAppManager = mGuardProviderResultMethodsMap["AntiDefraudAppManager"]!!
        assert(antiDefraudAppManager.size == 1)
        val antiDefraudAppManagerDescriptor = antiDefraudAppManager.first()
        val antiDefraudAppManagerMethod: Method = antiDefraudAppManagerDescriptor.getMethodInstance(lpparam.classLoader)
        antiDefraudAppManagerMethod.replaceMethod {
            return@replaceMethod null
        }*/
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app")
            }
        }.forEach {
            it.getMethodInstance(lpparam.classLoader).replaceMethod {
                return@replaceMethod null
            }
        }
    }
}
