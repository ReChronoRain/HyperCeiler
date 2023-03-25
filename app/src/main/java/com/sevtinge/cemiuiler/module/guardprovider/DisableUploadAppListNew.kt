package com.sevtinge.cemiuiler.module.guardprovider

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.replaceMethod
import io.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

class DisableUploadAppListNew : BaseHook() {

    @Throws(NoSuchMethodException::class)
    override fun init() {
        System.loadLibrary("dexkit")
        DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->

            val map = mapOf(
                "AntiDefraudAppManager" to setOf("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app"),
            )

            val resultMap = bridge.batchFindMethodsUsingStrings {
                queryMap(map)
            }

            val antiDefraudAppManager = resultMap["AntiDefraudAppManager"]!!
            assert(antiDefraudAppManager.size == 1)
            val antiDefraudAppManagerDescriptor = antiDefraudAppManager.first()
            val antiDefraudAppManagerMethod: Method = antiDefraudAppManagerDescriptor.getMethodInstance(lpparam.classLoader)
            antiDefraudAppManagerMethod.replaceMethod {
                return@replaceMethod null
            }
        }
    }

}
