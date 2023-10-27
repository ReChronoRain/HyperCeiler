package com.sevtinge.hyperceiler.module.hook.screenshot

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Modifier

object UnlockMinimumCropLimit : BaseHook() {
    private val mScreenCropViewMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("fixImageBounds %f,%f")
                }
                usingNumbers(0.5f, 200)
                returnType = "int"
                modifiers = Modifier.PRIVATE
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.first()
    }

    override fun init() {
        mScreenCropViewMethod.createHook {
            returnConstant(0)
        }

        /*val mScreenCropView =
            findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView\$h")
        returnIntConstant(mScreenCropView, "a")*/
    }

    /*private fun returnIntConstant(cls: Class<*>?, methodName: String) {
        findAndHookMethod(cls, methodName, XC_MethodReplacement.returnConstant(0))
    }*/
}
