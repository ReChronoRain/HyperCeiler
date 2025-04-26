package com.sevtinge.hyperceiler.hook.module.hook.camera

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockSuperHighQuality : BaseHook() {
    private val unlockMethod by lazy<Method> {
        DexKit.findMember("SuperHighQuality") {
            it.findMethod {
                matcher {
                    addCaller {
                        declaredClass {
                            usingEqStrings("pref_camera_jpegquality_key")
                        }
                        modifiers = Modifier.STATIC or Modifier.PUBLIC
                        addInvoke("Landroid/content/res/Resources;->getString(I)Ljava/lang/String;")
                    }

                    paramCount = 0
                    returnType = "boolean"
                }
            }.single()
        }
    }

    override fun init() {
        unlockMethod.createHook {
            returnConstant(true)
        }
    }
}