package com.sevtinge.hyperceiler.module.hook.securitycenter.beauty

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge


object BeautyPc : BaseHook() {
    override fun init() {
       dexKitBridge.findMethod {
          matcher {
              addUsingStringsEquals("persist.vendor.camera.facetracker.support")
              returnType = "boolean"
          }
       }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
           returnConstant(true)
       }

       /* try {
            val result: List<DexMethodDescriptor> =
                java.util.Objects.requireNonNull<List<DexMethodDescriptor>>(
                    SecurityCenterDexKit.mSecurityCenterResultMap.get("BeautyPc")
                )
            for (descriptor in result) {
                val beautyPc: java.lang.reflect.Method =
                    descriptor.getMethodInstance(lpparam.classLoader)
                log("beautyPc method is $beautyPc")
                if (beautyPc.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(beautyPc, XC_MethodReplacement.returnConstant(true))
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }*/
    }
}
