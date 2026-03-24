package com.sevtinge.hyperceiler.libhook.rules.securitycenter

import android.content.Context
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object NewPrivacyThumbnailBlur : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        thumbnailBlur
        return true
    }

    private val thumbnailBlur by lazy<List<Method>> {
        requiredMemberList("newPtb") {
            it.findClass {
                matcher {
                    addUsingString("miui_recents_privacy_thumbnail_blur", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    paramTypes(Context::class.java, String::class.java, Boolean::class.java)
                }
            }
        }
    }

    override fun init() {
        thumbnailBlur.forEach { method ->
            method.createBeforeHook { param ->
                if (Thread.currentThread().stackTrace.none {
                    it.className == "PrivacyThumbnailBlurSettings"
                }) {
                    param.result = null
                }
            }
        }
    }
}
