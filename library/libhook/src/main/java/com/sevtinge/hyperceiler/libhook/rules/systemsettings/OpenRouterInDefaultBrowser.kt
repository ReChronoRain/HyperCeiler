package com.sevtinge.hyperceiler.libhook.rules.systemsettings

import android.content.Context
import android.content.Intent
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.BrowserLinks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object OpenRouterInDefaultBrowser : BaseHook() {
    private const val ROUTER_UTILS = "com.android.settings.wifi.XiaomiRouterUtils"
    private const val BROWSER_UTILS = "com.android.settings.wifi.openwifi.Utils"

    override fun init() {
        val utility = findClassIfExists(BROWSER_UTILS) ?: return
        val selectBrowser = runCatching {
            utility.getDeclaredMethod(
                "getDefaultBrowserPkgIntent",
                Context::class.java,
                Intent::class.java
            )
        }.getOrNull() ?: return
        selectBrowser.createHook {
            before { param ->
                val context = param.args[0] as? Context ?: return@before
                val intent = param.args[1] as? Intent ?: return@before
                if (intent.action != Intent.ACTION_VIEW) return@before
                val url = BrowserLinks.webUri(intent.data) ?: return@before
                if (Thread.currentThread().stackTrace.none {
                        it.className == ROUTER_UTILS && it.methodName == "showManageRouter"
                    }) return@before
                BrowserLinks.forDefaultBrowser(context, url, intent.flags)
                    ?.let { param.result = it }
            }
        }
    }
}
