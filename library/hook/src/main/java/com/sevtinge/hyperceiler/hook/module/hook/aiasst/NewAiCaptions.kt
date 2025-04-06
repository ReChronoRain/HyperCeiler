package com.sevtinge.hyperceiler.hook.module.hook.aiasst

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import java.lang.reflect.Method

object NewAiCaptions: BaseHook() {
    private val mSupportAiSubtitlesUtils by lazy {
        findClassIfExists("com.xiaomi.aiasst.vision.utils.SupportAiSubtitlesUtils")
    }

    private val getMethod by lazy<Method> {
        DexKit.findMember("AiCaptionsModel") {
           it.findMethod {
               matcher {
                   // SupportAiSubtitlesUtils
                   addEqString("SupportAiSubtitlesUtils")
                   addUsingField("Landroid/os/Build;->DEVICE:Ljava/lang/String;")

                   paramCount = 0
               }
           }.single()
        }
    }



    override fun init() {
        if (mSupportAiSubtitlesUtils == null) {
            getMethod.createHook {
                returnConstant(true)
            }
        } else {
            runCatching {
                mSupportAiSubtitlesUtils.methodFinder()
                    .filterByName("isSupportAiSubtitles")
                    .single().createHook {
                        returnConstant(true)
                    }

                mSupportAiSubtitlesUtils.methodFinder()
                    .filterByName("isSupportOfflineAiSubtitles")
                    .single().createHook {
                        returnConstant(true)
                    }

            }
        }
    }
}
