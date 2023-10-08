package com.sevtinge.cemiuiler.module.hook.securitycenter.beauty

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Method

object BeautyFace : BaseHook() {
    var beautyFace: Method? = null
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("taoyao", "IN", "persist.vendor.vcb.ability")
                returnType = "boolean"
            }
        }.forEach {
            beautyFace = it.getMethodInstance(lpparam.classLoader)
        }

        beautyFace!!.createHook {
            returnConstant(true)
        }

        /*try {
            val result: List<DexMethodDescriptor> =
                Objects.requireNonNull<List<DexMethodDescriptor>>(
                    SecurityCenterDexKit.mSecurityCenterResultMap.get("BeautyFace")
                )
            for (descriptor in result) {
                beautyFace = descriptor.getMethodInstance(lpparam.classLoader)
                log("beautyFace method is " + beautyFace)
                if (beautyFace!!.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(beautyFace, XC_MethodReplacement.returnConstant(true))
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }*/
    }
}
