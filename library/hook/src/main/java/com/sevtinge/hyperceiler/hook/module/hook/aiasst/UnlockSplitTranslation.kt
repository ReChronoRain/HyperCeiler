package com.sevtinge.hyperceiler.hook.module.hook.aiasst

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockSplitTranslation : BaseHook() {

    private val hook by lazy {
        DexKit.findMember("unlockSplitTranslation") { dexKitBridge ->
            dexKitBridge.findClass {
                matcher {
                    addUsingString("SupportAiSubtitlesUtils", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    addInvoke {
                        paramTypes("java.lang.String[]")
                    }
                    modifiers = Modifier.PUBLIC or Modifier.STATIC
                    paramCount = 0
                    returnType = "boolean"
                }
            }.single()
        } as Method
    }

    override fun init() {
        hook.createHook {
            returnConstant(true)
        }
    }
}
