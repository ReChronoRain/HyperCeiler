package com.sevtinge.hyperceiler.module.hook.securitycenter.beauty

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
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
    }
}
