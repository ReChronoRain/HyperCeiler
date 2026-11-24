package com.sevtinge.hyperceiler.libhook.rules.voiceassist

import android.content.Context
import android.content.Intent
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.BrowserLinks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.lang.reflect.Method

object OpenRecognizedLinksInDefaultBrowser : BaseHook() {
    private var checks: List<Method> = emptyList()

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        checks = optionalMemberList<Method>("checkRecognizedIntent") { bridge ->
            bridge.findMethod {
                searchPackages("com.xiaomi.voiceassistant.utils")
                matcher {
                    name = "isIntentAvailable"
                    returnType = "boolean"
                    paramTypes("android.content.Intent", "android.content.Context")
                }
            }
        }.orEmpty()
        return true
    }

    override fun init() {
        checks.forEach { method ->
            method.createHook {
                before { param ->
                    val original = param.args[0] as? Intent ?: return@before
                    val context = param.args[1] as? Context ?: return@before
                    if (!BrowserLinks.targetsXiaomiBrowser(original)) return@before
                    val url = BrowserLinks.recoverWebUri(original) ?: return@before
                    val target = BrowserLinks.forDefaultBrowser(context, url, original.flags)
                        ?: return@before

                    // The caller reuses this same Intent after the availability check.
                    original.action = target.action
                    original.setDataAndType(target.data, null)
                    original.component = null
                    original.selector = null
                    original.setPackage(target.getPackage())
                    original.categories?.toList()?.forEach(original::removeCategory)
                    original.addCategory(Intent.CATEGORY_BROWSABLE)
                }
            }
        }
    }
}
