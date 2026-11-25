/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.aiengine

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.BrowserLinks
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import org.json.JSONObject
import java.lang.reflect.Method

object ChangeBrowserForAIEngine : BaseHook() {
    private const val COPY_WEBSITE_TYPE = 11
    private const val PREF_KEY_BROWSER = "aicr_browser"
    private const val NOTIFICATION_ID = 111
    private const val NOTIFICATION_CHANNEL = "phrase_channel_id"
    private const val COPY_WEBSITE_CUE_PREFIX =
        "cue.copy_text_jump_app#hce.copy_text_jump_app.jump_url#"
    private const val FOCUS_PARAM = "miui.focus.param"
    private val browserName =
        Regex("小米浏览器|小米瀏覽器|Mi Browser|Xiaomi Browser", RegexOption.IGNORE_CASE)

    private var websiteLaunchers: List<Method> = emptyList()
    private var dispatch: Method? = null
    private var canOpenWebsite: Method? = null
    private var getStartAppPackage: Method? = null
    private var createBubbleData: Method? = null
    private var parseCues: Method? = null

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        websiteLaunchers = optionalMemberList("websiteLaunchers") { bridge ->
            bridge.findMethod {
                matcher {
                    addUsingString("clipboard_open")
                    addUsingString("com.android.browser")
                    returnType = "void"
                    paramTypes(null, "java.lang.String")
                }
            }
        }

        val owner = websiteLaunchers.firstOrNull()?.declaringClass?.name
        if (owner != null) {
            dispatch = optionalMember("dispatchCopyDirect") { bridge ->
                bridge.findMethod {
                    matcher {
                        declaredClass = owner
                        returnType = "void"
                        paramTypes(null, "java.lang.String", "int")
                        addUsingString("androidamap://route?")
                    }
                }.singleOrNull()
            }
            canOpenWebsite = optionalMember("canOpenCopyDirectWebsite") { bridge ->
                val candidates = bridge.findMethod {
                    matcher {
                        declaredClass = owner
                        returnType = "boolean"
                        paramTypes("android.content.Context", "int")
                        addUsingString("com.android.browser")
                        addUsingString("com.taobao.taobao")
                    }
                }
                candidates.singleOrNull() ?: bridge.findMethod {
                    matcher {
                        declaredClass = owner
                        returnType = "boolean"
                        paramTypes("android.content.Context", "int")
                        addUsingString("isInstallForApp: ///////////////////")
                    }
                }.singleOrNull()
            }
            getStartAppPackage = optionalMember("getCopyDirectAppPackage") { bridge ->
                bridge.findMethod {
                    matcher {
                        declaredClass = owner
                        returnType = "java.lang.String"
                        paramTypes("android.content.Context", "int")
                        addUsingString("com.taobao.taobao")
                        addUsingString("com.eg.android.AlipayGphone")
                        addUsingString("com.android.browser")
                    }
                }.singleOrNull()
            }
        } else {
            dispatch = null
            canOpenWebsite = null
            getStartAppPackage = null
        }

        createBubbleData = optionalMember("createCopyDirectBubbleData") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "android.os.Bundle"
                    paramTypes(
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "android.content.Intent"
                    )
                    addUsingString("copyDirectData")
                    addUsingString("intentUri")
                    addUsingString("target_out")
                }
            }.singleOrNull()
        }
        parseCues = optionalMember("parseBubbleCues") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "java.util.List"
                    paramTypes("java.lang.String")
                    addUsingString("BubblePerceptionProcessor")
                    addUsingString("Parsed wrapper is null or has no cues")
                }
            }.singleOrNull()
        }
        return true
    }

    override fun init() {
        canOpenWebsite?.createHook {
            before { param ->
                if (param.args[1] != COPY_WEBSITE_TYPE) return@before
                val context = param.args[0] as? Context ?: return@before
                if (browserIntent(context, BrowserLinks.webProbe) != null) param.result = true
            }
        }
        getStartAppPackage?.createHook {
            before { param ->
                if (param.args[1] != COPY_WEBSITE_TYPE) return@before
                val context = param.args[0] as? Context ?: return@before
                browserPackage(context)?.let { param.result = it }
            }
        }
        dispatch?.createHook {
            before { param ->
                if (param.args[2] == COPY_WEBSITE_TYPE && openWebsite(
                        param.args[0],
                        param.args[1]
                    )
                ) {
                    param.result = null
                }
            }
        }
        websiteLaunchers.forEach { method ->
            method.createHook {
                before { param ->
                    if (openWebsite(param.args[0], param.args[1])) param.result = null
                }
            }
        }
        hookBubbleData()
        hookCueDisplay()
        hookNotification()
    }

    private fun selectedPackage(context: Context): String? {
        val packageName =
            PrefsBridge.getString(PREF_KEY_BROWSER, "")?.takeIf { it.isNotBlank() } ?: return null
        val probe = BrowserLinks.viewIntent(BrowserLinks.webProbe, packageName)
        return packageName.takeIf { BrowserLinks.canOpen(context, probe) }
    }

    private fun browserPackage(context: Context, url: Uri = BrowserLinks.webProbe) =
        browserIntent(context, url)?.getPackage()

    private fun browserLabel(context: Context, packageName: String): String? = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    private fun replaceBrowserName(text: String, label: String) =
        browserName.replace(text) { label }

    private fun browserIntent(context: Context, url: Uri, flags: Int = 0): Intent? {
        val selected = selectedPackage(context)
        if (selected != null) {
            val target = BrowserLinks.viewIntent(url, selected).setFlags(flags)
            if (BrowserLinks.canOpen(context, target)) return target
        }
        return BrowserLinks.forDefaultBrowser(context, url, flags)
    }

    private fun openWebsite(host: Any?, address: Any?): Boolean {
        val context = host as? Context ?: return false
        val url = BrowserLinks.webUri(address as? String, acceptBareAddress = true) ?: return false
        val target = browserIntent(context, url, Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        if (runCatching { context.startActivity(target) }.isSuccess) return true
        val fallback = BrowserLinks.forDefaultBrowser(context, url, Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return false
        return fallback.getPackage() != target.getPackage() && runCatching {
            context.startActivity(
                fallback
            )
        }.isSuccess
    }

    private fun hookBubbleData() {
        createBubbleData?.createHook {
            before { param ->
                val original = param.args[3] as? Intent ?: return@before
                if (!BrowserLinks.targetsXiaomiBrowser(original)) return@before
                val url = BrowserLinks.recoverWebUri(original) ?: return@before
                val context = EzXposed.appContextOrNull ?: return@before
                val target = browserIntent(context, url, original.flags) ?: return@before
                param.args[3] = target
                val browser = target.getPackage() ?: return@before
                val info = runCatching {
                    context.packageManager.getApplicationInfo(
                        browser,
                        0
                    )
                }.getOrNull()
                    ?: return@before
                if (info.icon != 0) param.args[2] = "android.resource://$browser/${info.icon}"
                val label = context.packageManager.getApplicationLabel(info).toString()
                for (index in 0..1) {
                    val text = param.args[index] as? String ?: continue
                    param.args[index] = replaceBrowserName(text, label)
                }
            }
        }
    }

    private fun hookCueDisplay() {
        val cueClass = findClassIfExists("com.xiaomi.ai.bubble.core.model.CueData") ?: return
        parseCues?.createHook {
            after { param ->
                val cues = param.result as? List<*> ?: return@after
                val context = EzXposed.appContextOrNull ?: return@after
                for (cue in cues) {
                    if (cue != null && cueClass.isInstance(cue)) rewriteCueDisplay(context, cue)
                }
            }
        }
        val bubbleClass = findClassIfExists("com.xiaomi.ai.bubble.core.bubbleview.view.BubbleView")
            ?: return
        val bindCue = bubbleClass.declaredMethods.singleOrNull {
            it.returnType == Void.TYPE && it.parameterTypes.contentEquals(arrayOf(cueClass))
        } ?: return
        bindCue.createHook {
            before { param ->
                val cue = param.args[0] ?: return@before
                val context = EzXposed.appContextOrNull ?: return@before
                rewriteCueDisplay(context, cue)
            }
        }
    }

    private fun rewriteCueDisplay(context: Context, cue: Any) {
        val category = getObjectField(cue, "category") as? String
        val cueId = getObjectField(cue, "cueId") as? String
        val isWebsiteCue = cueId?.startsWith(COPY_WEBSITE_CUE_PREFIX) == true
        if (!isWebsiteCue && category != "copy_jump") return

        val display = getObjectField(cue, "display") ?: return
        val originalPackage = getObjectField(display, "targetPackage") as? String
        if (!BrowserLinks.isXiaomiBrowser(originalPackage) &&
            !(isWebsiteCue && originalPackage.isNullOrBlank())
        ) return

        val browser = browserPackage(context) ?: return
        if (BrowserLinks.isXiaomiBrowser(browser)) return
        val label = browserLabel(context, browser) ?: return
        val card = getObjectField(display, "briefCard") ?: return
        val iconField = card.javaClass.getField("startIcon")
        val icon = iconField.type.getConstructor(String::class.java, String::class.java)
            .newInstance("application", browser)

        for (field in listOf("title", "description")) {
            val textModel = getObjectField(card, field) ?: continue
            val text = getObjectField(textModel, "text") as? String ?: continue
            val updatedText = replaceBrowserName(text, label)
            if (updatedText == text) continue
            setObjectField(textModel, "text", updatedText)
            if (updatedText.length != text.length) setObjectField(textModel, "highlights", null)
        }
        iconField.set(card, icon)
        setObjectField(display, "targetPackage", browser)
    }

    private fun hookNotification() {
        NotificationManager::class.java.getDeclaredMethod(
            "notify", String::class.java, Int::class.javaPrimitiveType, Notification::class.java
        ).createHook {
            before { param ->
                if (param.args[1] != NOTIFICATION_ID) return@before
                val notification = param.args[2] as? Notification ?: return@before
                if (notification.channelId != NOTIFICATION_CHANNEL) return@before
                val extras = notification.extras ?: return@before
                val focus = extras.getString(FOCUS_PARAM)?.let {
                    runCatching { JSONObject(it) }.getOrNull()
                }
                val focusParams = focus?.optJSONObject("param_v2")
                val action = focusParams?.optJSONArray("actions")?.optJSONObject(0)
                    ?.optString("actionIntent")?.takeIf(String::isNotEmpty)
                val click = action?.let {
                    runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull()
                } ?: BrowserLinks.notificationIntent(notification) ?: return@before
                if (click.getIntExtra("type", -1) != COPY_WEBSITE_TYPE) return@before
                val context = EzXposed.appContextOrNull ?: return@before
                val url =
                    BrowserLinks.webUri(extras.getString("copyText"), acceptBareAddress = true)
                        ?: BrowserLinks.webProbe
                val browser = browserPackage(context, url) ?: return@before
                if (BrowserLinks.isXiaomiBrowser(browser)) return@before
                BrowserLinks.appIcon(context, browser)?.let { icon ->
                    BrowserLinks.updateFocusIcon(notification, icon)
                    setObjectField(notification, "mSmallIcon", icon)
                }
                val label = browserLabel(context, browser) ?: return@before
                extras.putCharSequence(Notification.EXTRA_TITLE, label)
                extras.putCharSequence(Notification.EXTRA_TITLE_BIG, label)
                for (key in listOf(Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)) {
                    val text = extras.getCharSequence(key) ?: continue
                    val updatedText = replaceBrowserName(text.toString(), label)
                    if (updatedText != text.toString()) extras.putCharSequence(key, updatedText)
                }
                val iconText = focusParams?.optJSONObject("iconTextInfo") ?: return@before
                iconText.put("title", label)
                if (iconText.has("content")) {
                    iconText.put(
                        "content",
                        replaceBrowserName(iconText.optString("content"), label)
                    )
                }
                extras.putString(FOCUS_PARAM, focus.toString())
            }
        }
    }
}
