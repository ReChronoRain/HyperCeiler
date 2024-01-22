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
       }.single().getMethodInstance(lpparam.classLoader).createHook {
           returnConstant(true)
       }
    }
}
