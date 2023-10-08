package com.sevtinge.cemiuiler.module.hook.guardprovider

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.replaceMethod

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
