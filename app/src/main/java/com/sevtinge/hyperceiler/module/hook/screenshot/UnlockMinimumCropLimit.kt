package com.sevtinge.hyperceiler.module.hook.screenshot

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Modifier

@SuppressLint("StaticFieldLeak")
object UnlockMinimumCropLimit : BaseHook() {
    private val mScreenCropViewMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                usingNumbers(0.5f, 200)
                returnType = "int"
                paramCount = 0
                modifiers = Modifier.PRIVATE
            }
        }.firstOrNull()?.getMethodInstance(EzXHelper.safeClassLoader)
    }


    override fun init() {
        mScreenCropViewMethod!!.createHook {
            returnConstant(0)
        }
    }
}
