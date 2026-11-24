package com.sevtinge.hyperceiler.libhook.rules.mishare

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.BrowserLinks
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.lang.reflect.Method

object OpenLinksInDefaultBrowser : BaseHook() {
    private const val TAP_DATA = "com.miui.mishare.tap.TapData"
    private const val TAP_RECEIVE_DATA = "com.miui.mishare.tap.TapRecvData"
    private const val RECEIVE_CLICK = "com.miui.mishare.action.FOCUS_NOTIFICATION_RECEIVE_CLICK"

    private var buildReceiveIntent: Method? = null
    private var tapData: Class<*>? = null
    private var tapReceiveData: Class<*>? = null

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        buildReceiveIntent = optionalMember("buildReceiveIntent") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "android.content.Intent"
                    paramTypes(
                        "android.content.Context",
                        null,
                        TAP_RECEIVE_DATA,
                        "boolean",
                        "boolean"
                    )
                    addUsingString("IntentBuilder")
                    addUsingString("open one file!")
                }
            }.singleOrNull()
        }
        return true
    }

    override fun init() {
        tapData = findClassIfExists(TAP_DATA)
        tapReceiveData = findClassIfExists(TAP_RECEIVE_DATA)

        buildReceiveIntent?.createHook {
            after { param ->
                val context = param.args[0] as? Context ?: return@after
                val files = param.args[1] as? Array<*>
                val original = param.result as? Intent
                val url = (if (original?.action == Intent.ACTION_VIEW &&
                    (original.getPackage() == null || BrowserLinks.targetsXiaomiBrowser(original)) &&
                    !BrowserLinks.isBrowserDownload(original.data)
                ) {
                    BrowserLinks.webUri(original.data)
                } else if (files.isNullOrEmpty() && (original == null || BrowserLinks.isBrowserDownload(
                        original.data
                    ))
                ) {
                    receivedWebLink(param.args[2])
                } else {
                    null
                }) ?: return@after
                BrowserLinks.forDefaultBrowser(context, url, original?.flags ?: 0)
                    ?.let { param.result = it }
            }
        }

        tapData?.declaredMethods?.filter {
            it.name in setOf(
                "c",
                "d"
            ) && it.parameterCount == 0 && it.returnType == Drawable::class.java
        }?.forEach { getter ->
            getter.createHook {
                after { param ->
                    if (receivedWebLink(param.thisObject) == null) return@after
                    val context = EzXposed.appContextOrNull ?: return@after
                    val browser = BrowserLinks.defaultPackage(context) ?: return@after
                    runCatching { context.packageManager.getApplicationIcon(browser) }.getOrNull()
                        ?.let { param.result = it }
                }
            }
        }

        NotificationManager::class.java.getDeclaredMethod(
            "notify", String::class.java, Int::class.javaPrimitiveType, Notification::class.java
        ).createHook {
            before { param ->
                val notification = param.args[2] as? Notification ?: return@before
                val click = BrowserLinks.notificationIntent(notification) ?: return@before
                if (click.action != RECEIVE_CLICK) return@before
                val dataClass = tapReceiveData ?: return@before
                val data =
                    runCatching { click.getParcelableExtra("tap_recv_data", dataClass) }.getOrNull()
                if (receivedWebLink(data) == null) return@before
                val context = EzXposed.appContextOrNull ?: return@before
                val browser = BrowserLinks.defaultPackage(context) ?: return@before
                val icon = BrowserLinks.appIcon(context, browser) ?: return@before
                BrowserLinks.updateFocusIcon(notification, icon)
            }
        }
    }

    private fun receivedWebLink(data: Any?): Uri? {
        if (data == null || tapReceiveData?.isInstance(data) != true) return null
        return runCatching {
            val type = data.javaClass.getMethod("a").invoke(data) as? String
            val source = data.javaClass.getMethod("i").invoke(data) as? String
            if (type != "browser" && !BrowserLinks.isXiaomiBrowser(source)) return null
            val link = data.javaClass.getMethod("g").invoke(data) as? String
            val text = data.javaClass.getMethod("l").invoke(data) as? String
            BrowserLinks.webUri(link) ?: BrowserLinks.webUri(text)
        }.getOrNull()
    }
}
