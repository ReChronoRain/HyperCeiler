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

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.core.net.toUri
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.BitmapUtils
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.lang.reflect.Method

object ChangeBrowserForAIEngine : BaseHook() {

    private const val COPY_WEBSITE_TYPE = 11
    private const val PREF_KEY_BROWSER = "aicr_browser"
    private const val NOTIFICATION_ID = 111

    private var isInstallForAppMethod: Method? = null
    private lateinit var getStartAppPackageMethod: Method
    private var startIntentToAppMethod: Method? = null
    private lateinit var jumpToXiaoMiBrowserMethod: Method
    private lateinit var showNotificationMethod: Method

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        // "clipboard_open" 全 dex 唯一，用来锁定 SmartPasswordUtils 所在类
        jumpToXiaoMiBrowserMethod = requiredMember("jumpToXiaoMiBrowser") { bridge ->
            bridge.findMethod {
                matcher {
                    addUsingString("clipboard_open")
                    returnType = "void"
                }
            }.single()
        }
        val ownerName = jumpToXiaoMiBrowserMethod.declaringClass.name

        // 3.17.x 是 (Context, int)；4.0.x 已删除该重载只剩 (Context, String)
        isInstallForAppMethod = optionalMember("isInstallForApp") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass = ownerName
                    returnType = "boolean"
                    paramTypes("android.content.Context", "int")
                    addUsingString("isInstallForApp: ///////////////////")
                }
            }.singleOrNull()
        }

        getStartAppPackageMethod = requiredMember("getStartAppPackage") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass = ownerName
                    returnType = "java.lang.String"
                    paramTypes("android.content.Context", "int")
                    addUsingString("com.taobao.taobao")
                    addUsingString("com.eg.android.AlipayGphone")
                }
            }.single()
        }

        startIntentToAppMethod = optionalMember("startIntentToApp") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass = ownerName
                    returnType = "void"
                    paramCount = 3
                    paramTypes(null, "java.lang.String", "int")
                    addUsingString("androidamap://route?")
                }
            }.singleOrNull()
        }

        showNotificationMethod = requiredMember("showNotification") { bridge ->
            bridge.findMethod {
                matcher { addUsingString("phrase_channel_id") }
            }.single()
        }
        return true
    }

    override fun init() {
        isInstallForAppMethod?.createHook {
            before {
                if ((it.args[1] as? Int) != COPY_WEBSITE_TYPE) return@before
                val ctx = it.args[0] as? Context ?: return@before
                val selected = resolveSelectedPackage(ctx)
                it.result = if (selected != null) {
                    canHandleBrowserIntent(ctx, selected)
                } else {
                    canOpenAnyBrowser(ctx)
                }
            }
        }

        getStartAppPackageMethod.createHook {
            before {
                if ((it.args[1] as? Int) != COPY_WEBSITE_TYPE) return@before
                val ctx = it.args[0] as? Context ?: return@before
                // 用户没选 → 不接管，宿主继续按原值走
                val selected = resolveSelectedPackage(ctx) ?: return@before
                it.result = selected
            }
        }

        if (startIntentToAppMethod != null) {
            // 3.17.x：拦截统一调度，type==11 走 openInSelectedBrowser
            startIntentToAppMethod!!.createHook {
                before {
                    if ((it.args[2] as? Int) != COPY_WEBSITE_TYPE) return@before
                    val ctx = it.args[0] as? Context ?: return@before
                    val url = it.args[1] as? String ?: return@before
                    openInSelectedBrowser(ctx, url)
                    it.result = null
                }
            }
        } else {
            // 4.0.x：宿主已经按 type 内联拆掉，直接接管 jumpToXiaoMiBrowser
            jumpToXiaoMiBrowserMethod.createHook {
                before {
                    val ctx = it.args[0] as? Context ?: return@before
                    val url = it.args[1] as? String ?: return@before
                    openInSelectedBrowser(ctx, url)
                    it.result = null
                }
            }
        }

        showNotificationMethod.createHook {
            after {
                if ((it.args[2] as? Int) != COPY_WEBSITE_TYPE) return@after
                val ctx = it.args[0] as? Context ?: return@after
                refreshNotificationBrowserIcon(ctx)
            }
        }
    }

    // ==================== 浏览器选择 ====================

    /** 用户显式选定的浏览器包名；null 表示跟随系统默认。 */
    private fun resolveSelectedPackage(context: Context): String? {
        val raw = PrefsBridge.getString(PREF_KEY_BROWSER, "")?.takeIf { it.isNotBlank() } ?: return null
        return raw.takeIf { canHandleBrowserIntent(context, it) }
    }

    /** 系统当前能否打开 http(s)。 */
    private fun canOpenAnyBrowser(context: Context): Boolean {
        return context.packageManager
            .resolveActivity(createBrowserViewIntent(), PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    /** PackageManager 解析的系统默认浏览器包名；不存在返回 null。 */
    private fun resolveSystemDefaultBrowser(context: Context): String? {
        val pkg = context.packageManager
            .resolveActivity(createBrowserViewIntent(), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        return pkg?.takeUnless { it.isBlank() || it == "android" || it == context.packageName }
    }

    private fun openInSelectedBrowser(context: Context, url: String) {
        val target = url.withHttpsIfMissing().toUri()
        val targetPackage = resolveSelectedPackage(context)
        val intent = Intent(Intent.ACTION_VIEW, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            targetPackage?.let { setPackage(it) }
        }
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, target).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun canHandleBrowserIntent(context: Context, packageName: String): Boolean {
        val intent = createBrowserViewIntent().apply { setPackage(packageName) }
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .isNotEmpty()
    }

    private fun createBrowserViewIntent(): Intent =
        Intent(Intent.ACTION_VIEW, "http:".toUri()).addCategory(Intent.CATEGORY_BROWSABLE)

    private fun String.withHttpsIfMissing(): String =
        if (startsWith("http://", true) || startsWith("https://", true)) this else "https://$this"

    // ==================== 通知图标替换 ====================

    private fun refreshNotificationBrowserIcon(context: Context) {
        // 用户选了具体浏览器 → 用它的图标；否则用系统默认浏览器的图标
        val targetPackage = resolveSelectedPackage(context) ?: resolveSystemDefaultBrowser(context) ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = nm.activeNotifications.firstOrNull {
            it.id == NOTIFICATION_ID && it.packageName == context.packageName
        }?.notification ?: return

        val pm = context.packageManager
        val info = runCatching { pm.getApplicationInfo(targetPackage, 0) }.getOrNull() ?: return
        val label = pm.getApplicationLabel(info)
        val drawable = runCatching { pm.getApplicationIcon(info) }.getOrNull() ?: return
        val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            runCatching { BitmapUtils.drawableToBitmap(drawable) }.getOrNull() ?: return
        }
        val icon = Icon.createWithBitmap(bitmap)

        val focusPics = notification.extras.getBundle("miui.focus.pics") ?: Bundle()
        focusPics.putParcelable("miui.focus.pic_image", icon)
        focusPics.putParcelable("miui.land.pic_image", icon)
        notification.extras.putBundle("miui.focus.pics", focusPics)
        notification.extras.putParcelable("miui.appIcon", icon)
        notification.extras.putCharSequence("android.title", label)
        notification.extras.putCharSequence("android.title.big", label)
        nm.notify(NOTIFICATION_ID, notification)
    }
}
