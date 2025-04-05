package com.sevtinge.hyperceiler.module.hook.soundrecorder

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import java.lang.reflect.Method

object UnlockRecordingScene : BaseHook() {
    private val unlockMethod by lazy<Method> {
        DexKit.findMember("recordScene") {
            it.findMethod {
                matcher {
                    usingEqStrings("support_record_param")
                    returnType = "boolean"
                }
            }.single()
        }
    }

    private val unlockMethod2 by lazy<Method> {
        DexKit.findMember("recordScene2") {
            it.findMethod {
                matcher {
                    usingEqStrings("support_hd_record_param")
                    returnType = "boolean"
                }
            }.single()
        }
    }

    override fun init() {
        unlockMethod.createHook {
            returnConstant(true)
        }

        unlockMethod2.createHook {
            returnConstant(true)
        }
    }
}
