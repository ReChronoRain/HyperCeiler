package com.sevtinge.cemiuiler.module.hook.guardprovider

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.hook.guardprovider.GuardProviderDexKit.mGuardProviderResultMethodsMap
import com.sevtinge.cemiuiler.utils.replaceMethod
import java.lang.reflect.Method

class DisableUploadAppListNew : BaseHook() {

    @Throws(NoSuchMethodException::class)
    override fun init() {
        val antiDefraudAppManager = mGuardProviderResultMethodsMap["AntiDefraudAppManager"]!!
        assert(antiDefraudAppManager.size == 1)
        val antiDefraudAppManagerDescriptor = antiDefraudAppManager.first()
        val antiDefraudAppManagerMethod: Method = antiDefraudAppManagerDescriptor.getMethodInstance(lpparam.classLoader)
        antiDefraudAppManagerMethod.replaceMethod {
            return@replaceMethod null
        }
    }
}
