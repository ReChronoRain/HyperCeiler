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
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Bundle
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.newInstance
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.text.Collator
import java.util.Locale

object ChangeBrowserForAIEngine : BaseHook() {

    private const val COPY_WEBSITE_TYPE = 11
    private const val COPY_WEBSITE_ENABLED_KEY = "copy_website_enabled"
    private const val NOTIFICATION_ID = 111

    private const val SMART_PASSWORD_UTILS_CLASS =
        "com.xiaomi.aicr.copydirect.util.SmartPasswordUtils"
    private const val NOTIFICATION_UTILS_CLASS =
        "com.xiaomi.aicr.copydirect.util.NotificationUtils"
    private const val COPY_WEBSITE_SETTINGS_FRAGMENT_CLASS =
        "com.xiaomi.aicr.copydirect.setting.CopyWebSiteSettingsFragment"
    private const val DROP_DOWN_PREFERENCE_CLASS = "miuix.preference.DropDownPreference"
    private const val APP_INSTALLATION_PREFERENCE_CLASS =
        "com.xiaomi.aicr.copydirect.setting.AppInstallationPreference"
    private const val TARGET_CONTEXT_UTIL_CLASS = "com.xiaomi.aicr.common.ContextUtil"
    private const val TARGET_PACKAGE_UTILS_CLASS = "com.xiaomi.aireco.utils.PackageUtils"

    private const val WEBSITE_SWITCH_KEY = "checkbox_copy_website"
    private const val ORIGINAL_BROWSER_PREF_KEY = "app_install_xiaomi_browser"
    private const val CUSTOM_BROWSER_PREF_KEY = "hyperceiler_copy_website_browser"
    private const val CUSTOM_BROWSER_PREF_PREFIX = "hyperceiler_copy_website_browser_"
    private const val DEFAULT_BROWSER_VALUE = "__default_browser__"

    private const val EXTRA_USE_DEFAULT_BROWSER =
        "hyperceiler_copy_website_use_default_browser"
    private const val EXTRA_BROWSER_PACKAGE =
        "hyperceiler_copy_website_browser_package"

    @Volatile
    private var appContext: Context? = null

    private data class BrowserApp(
        val packageName: String,
        val label: String,
        val icon: Drawable?
    )

    private data class BrowserChoice(
        val packageName: String?,
        val useDefault: Boolean
    )

    override fun init() {
        runOnApplicationAttach {
            appContext = it.applicationContext
        }

        hookCopyWebsiteSettings()
        hookSmartPasswordUtils()
        hookNotifications()
    }

    private fun hookCopyWebsiteSettings() {
        loadClass(COPY_WEBSITE_SETTINGS_FRAGMENT_CLASS).apply {
            methodFinder()
                .filterByName("onCreatePreferences")
                .first()
                .createHook {
                    after {
                        syncCopyWebsiteSettings(it.thisObject)
                    }
                }

            methodFinder()
                .filterByName("onResume")
                .first()
                .createHook {
                    after {
                        syncCopyWebsiteSettings(it.thisObject)
                    }
                }

            methodFinder()
                .filterByName("onPreferenceChange")
                .first()
                .createHook {
                    after {
                        val preference = it.args[0] ?: return@after
                        val key = preference.callMethod("getKey") as? String ?: return@after
                        if (key == WEBSITE_SWITCH_KEY) {
                            syncCopyWebsiteSettings(it.thisObject)
                        }
                    }
                }
        }
    }

    private fun hookSmartPasswordUtils() {
        loadClass(SMART_PASSWORD_UTILS_CLASS).apply {
            methodFinder()
                .filterByName("isSupportPackageName")
                .filterByParamTypes(Int::class.javaPrimitiveType!!, String::class.java)
                .first()
                .createHook {
                    before {
                        if ((it.args[0] as Int) != COPY_WEBSITE_TYPE) return@before

                        val context = getTargetAppContext() ?: return@before
                        val packageName = it.args[1] as? String ?: ""
                        it.result = findBrowserApp(context, packageName) != null
                    }
                }

            methodFinder()
                .filterByName("isInstallForApp")
                .filterByParamCount(3)
                .first()
                .createHook {
                    before {
                        if ((it.args[1] as Int) != COPY_WEBSITE_TYPE) return@before

                        val context = it.args[0] as? Context ?: return@before
                        it.result = canOpenSelectedBrowser(context)
                    }
                }

            methodFinder()
                .filterByName("getStartAppPackage")
                .filterByParamCount(2)
                .first()
                .createHook {
                    before {
                        if ((it.args[1] as Int) != COPY_WEBSITE_TYPE) return@before

                        val context = it.args[0] as? Context ?: return@before
                        it.result = getLaunchPackageName(
                            context,
                            resolveBrowserChoice(context, preferSnapshot = true)
                        )
                    }
                }

            methodFinder()
                .filterByName("startIntentToApp")
                .filterByParamCount(3)
                .first()
                .createHook {
                    before {
                        if ((it.args[2] as Int) != COPY_WEBSITE_TYPE) return@before

                        val context = it.args[0] as? Context ?: return@before
                        val url = it.args[1] as? String ?: return@before
                        openInSelectedBrowser(
                            context,
                            url,
                            resolveBrowserChoice(context, preferSnapshot = true)
                        )
                        it.result = null
                    }
                }
        }
    }

    private fun hookNotifications() {
        loadClass(NOTIFICATION_UTILS_CLASS).apply {
            methodFinder()
                .filterByName("getNotificationInfo")
                .first()
                .createHook {
                    after {
                        if ((it.args[1] as Int) != COPY_WEBSITE_TYPE) return@after

                        val context = it.args[0] as? Context ?: return@after
                        val choice = resolveBrowserChoice(context)
                        if (choice.useDefault) return@after

                        val title = resolveChosenBrowserApp(context, choice)?.label ?: return@after
                        it.result?.setObjectField("title", title)
                    }
                }

            methodFinder()
                .filterByName("getNotificationStartIntent")
                .first()
                .createHook {
                    after {
                        if ((it.args[2] as Int) != COPY_WEBSITE_TYPE) return@after

                        val context = it.args[0] as? Context ?: return@after
                        val intent = it.result as? Intent ?: return@after
                        writeBrowserSnapshot(intent, resolveBrowserChoice(context))
                    }
                }

            methodFinder()
                .filterByName("showNotification")
                .first()
                .createHook {
                    after {
                        if ((it.args[2] as Int) != COPY_WEBSITE_TYPE) return@after

                        val context = it.args[0] as? Context ?: return@after
                        val choice = resolveBrowserChoice(context)
                        refreshNotificationBrowserIcon(context, choice)
                    }
                }
        }
    }

    private fun syncCopyWebsiteSettings(fragment: Any) {
        val context = fragment.callMethod("getContext") as? Context ?: return
        val screen = fragment.callMethod("getPreferenceScreen") ?: return
        val browsers = getBrowserApps(context)
        val enabled = isCopyWebsiteEnabled(context)

        findSupportCategory(screen)?.let {
            it.callMethod("setOrder", 20)
            it.callMethod("setVisible", browsers.isNotEmpty())
        }

        fragment.callMethod("findPreference", ORIGINAL_BROWSER_PREF_KEY)?.let {
            it.callMethod("setVisible", false)
        }

        val dropDownPreference = ensureBrowserDropDownPreference(fragment, screen, context)
        configureBrowserDropDownPreference(dropDownPreference, context, browsers, enabled)
        rebuildBrowserSupportPreferences(screen, context, browsers)
    }

    private fun ensureBrowserDropDownPreference(fragment: Any, screen: Any, context: Context): Any {
        fragment.callMethod("findPreference", CUSTOM_BROWSER_PREF_KEY)?.let {
            return it
        }

        return loadClass(DROP_DOWN_PREFERENCE_CLASS).newInstance<Any>(context).also {
            it.callMethod("setKey", CUSTOM_BROWSER_PREF_KEY)
            it.callMethod("setOnPreferenceChangeListener", fragment)
            screen.callMethod("addPreference", it)
        }
    }

    private fun configureBrowserDropDownPreference(
        preference: Any,
        context: Context,
        browsers: List<BrowserApp>,
        enabled: Boolean
    ) {
        val selectedValue = sanitizeSelectedBrowserValue(readSelectedBrowserValue(context), browsers)

        preference.apply {
            callMethod("setTitle", preferredBrowserTitle(context))
            callMethod("setEntries", buildBrowserEntries(context, browsers).toTypedArray())
            callMethod("setEntryValues", buildBrowserValues(browsers).toTypedArray())
            callMethod("setValue", selectedValue)
            callMethod("setVisible", enabled && browsers.isNotEmpty())
            callMethod("setOrder", 10)
        }
    }

    private fun buildBrowserEntries(context: Context, browsers: List<BrowserApp>): List<CharSequence> {
        return buildList {
            add(defaultBrowserText(context))
            addAll(browsers.map { it.label })
        }
    }

    private fun buildBrowserValues(browsers: List<BrowserApp>): List<CharSequence> {
        return buildList {
            add(DEFAULT_BROWSER_VALUE)
            addAll(browsers.map { it.packageName })
        }
    }

    private fun rebuildBrowserSupportPreferences(
        screen: Any,
        context: Context,
        browsers: List<BrowserApp>
    ) {
        getScreenPreferences(screen)
            .filter { getPreferenceKey(it)?.startsWith(CUSTOM_BROWSER_PREF_PREFIX) == true }
            .forEach { screen.callMethod("removePreference", it) }

        browsers.forEachIndexed { index, browser ->
            loadClass(APP_INSTALLATION_PREFERENCE_CLASS).newInstance<Any>(context).also {
                it.callMethod("setKey", CUSTOM_BROWSER_PREF_PREFIX + browser.packageName)
                it.callMethod("setTitle", browser.label)
                resolveBrowserDrawable(context, browser)?.let { icon ->
                    it.callMethod("setIcon", icon)
                }
                it.callMethod("setClickable", false)
                it.callMethod("setTouchAnimationEnable", false)
                it.callMethod("setPackageName", browser.packageName)
                it.callMethod("setOrder", 21 + index)
                screen.callMethod("addPreference", it)
            }
        }
    }

    private fun getScreenPreferences(screen: Any): List<Any> {
        val count = screen.callMethod("getPreferenceCount") as? Int ?: return emptyList()
        return (0 until count).mapNotNull { index -> screen.callMethod("getPreference", index) }
    }

    private fun findSupportCategory(screen: Any): Any? {
        return getScreenPreferences(screen)
            .firstOrNull { it.javaClass.name == "androidx.preference.PreferenceCategory" }
    }

    private fun getPreferenceKey(preference: Any): String? {
        return preference.callMethod("getKey") as? String
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getBrowserApps(context: Context): List<BrowserApp> {
        val packageManager = context.packageManager
        val browsers = LinkedHashMap<String, BrowserApp>()

        queryBrowserCandidates(packageManager)
            .forEach { resolveInfo ->
                resolveInfo.toBrowserApp(packageManager)?.let { browser ->
                    browsers.putIfAbsent(browser.packageName, browser)
                }
            }

        resolveDefaultBrowser(context)?.let {
            browsers.putIfAbsent(it.packageName, it)
        }

        val collator = Collator.getInstance(Locale.getDefault())
        return browsers.values.sortedWith { left, right ->
            collator.compare(left.label, right.label)
        }
    }

    private fun resolveDefaultBrowser(context: Context): BrowserApp? {
        val packageManager = context.packageManager
        val resolveInfo = packageManager.resolveActivity(
            createBrowserViewIntent(),
            PackageManager.MATCH_DEFAULT_ONLY
        ) ?: return null
        return resolveInfo.toBrowserApp(packageManager, requireBrowserCapability = false)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun queryBrowserCandidates(packageManager: PackageManager): List<ResolveInfo> {
        return packageManager.queryIntentActivities(
            createBrowserViewIntent(),
            PackageManager.MATCH_ALL
        )
    }

    private fun resolveChosenBrowserApp(context: Context, choice: BrowserChoice): BrowserApp? {
        if (choice.useDefault) {
            return resolveDefaultBrowser(context)
        }

        return findBrowserApp(context, choice.packageName)
    }

    private fun findBrowserApp(context: Context, packageName: String?): BrowserApp? {
        if (packageName.isNullOrBlank()) return null
        val packageManager = context.packageManager
        return queryBrowserCandidates(packageManager)
            .firstOrNull { it.activityInfo?.packageName == packageName }
            ?.toBrowserApp(packageManager)
    }

    private fun canOpenSelectedBrowser(context: Context): Boolean {
        val choice = resolveBrowserChoice(context)
        return if (choice.useDefault) {
            getBrowserApps(context).isNotEmpty()
        } else {
            resolveChosenBrowserApp(context, choice) != null
        }
    }

    private fun resolveBrowserChoice(context: Context, preferSnapshot: Boolean = false): BrowserChoice {
        if (preferSnapshot) {
            getBrowserChoiceSnapshot(context)?.let {
                return sanitizeBrowserChoice(context, it)
            }
        }

        val selectedValue = readSelectedBrowserValue(context)

        return if (selectedValue == DEFAULT_BROWSER_VALUE) {
            BrowserChoice(packageName = null, useDefault = true)
        } else {
            sanitizeBrowserChoice(context, BrowserChoice(packageName = selectedValue, useDefault = false))
        }
    }

    private fun getBrowserChoiceSnapshot(context: Context): BrowserChoice? {
        val intent = (context as? Activity)?.intent ?: return null
        if (!intent.hasExtra(EXTRA_USE_DEFAULT_BROWSER) && !intent.hasExtra(EXTRA_BROWSER_PACKAGE)) {
            return null
        }

        val useDefault = intent.getBooleanExtra(EXTRA_USE_DEFAULT_BROWSER, true)
        return if (useDefault) {
            BrowserChoice(packageName = null, useDefault = true)
        } else {
            intent.getStringExtra(EXTRA_BROWSER_PACKAGE)
                ?.takeUnless { it.isBlank() }
                ?.let { BrowserChoice(packageName = it, useDefault = false) }
                ?: BrowserChoice(packageName = null, useDefault = true)
        }
    }

    private fun sanitizeBrowserChoice(context: Context, choice: BrowserChoice): BrowserChoice {
        if (choice.useDefault) return choice
        val packageName = choice.packageName ?: return BrowserChoice(null, true)
        return if (findBrowserApp(context, packageName) != null) choice else BrowserChoice(null, true)
    }

    private fun getLaunchPackageName(context: Context, choice: BrowserChoice): String {
        return resolveChosenBrowserApp(context, choice)?.packageName.orEmpty()
    }

    private fun openInSelectedBrowser(context: Context, url: String, choice: BrowserChoice) {
        val targetUrl = url.withHttpsIfMissing().toUri()

        if (choice.useDefault) {
            Intent(Intent.ACTION_VIEW, targetUrl).let {
                context.startActivity(it)
            }
            return
        }

        Intent(Intent.ACTION_VIEW, targetUrl).apply {
            setPackage(choice.packageName)
        }.let { intent ->
            runCatching {
                context.startActivity(intent)
            }.onFailure {
                context.startActivity(Intent(Intent.ACTION_VIEW, targetUrl))
            }
        }
    }

    private fun refreshNotificationBrowserIcon(
        context: Context,
        choice: BrowserChoice
    ) {
        val icon = resolveChosenBrowserIcon(context, choice) ?: return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = notificationManager.activeNotifications.firstOrNull {
            it.id == NOTIFICATION_ID &&
                it.packageName == context.packageName
        }?.notification ?: return

        val focusPics = notification.extras.getBundle("miui.focus.pics") ?: Bundle()
        focusPics.putParcelable("miui.focus.pic_image", icon)
        focusPics.putParcelable("miui.land.pic_image", icon)
        notification.extras.putBundle("miui.focus.pics", focusPics)
        notification.extras.putParcelable("miui.appIcon", icon)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun writeBrowserSnapshot(intent: Intent, choice: BrowserChoice) {
        intent.putExtra(EXTRA_USE_DEFAULT_BROWSER, choice.useDefault)
        if (choice.useDefault) {
            intent.removeExtra(EXTRA_BROWSER_PACKAGE)
        } else {
            intent.putExtra(EXTRA_BROWSER_PACKAGE, choice.packageName)
        }
    }

    private fun createBrowserViewIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, "http:".toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

    private fun readSelectedBrowserValue(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(CUSTOM_BROWSER_PREF_KEY, DEFAULT_BROWSER_VALUE)
            ?: DEFAULT_BROWSER_VALUE
    }

    private fun sanitizeSelectedBrowserValue(value: String, browsers: List<BrowserApp>): String {
        if (value == DEFAULT_BROWSER_VALUE) return value
        return if (browsers.any { it.packageName == value }) value else DEFAULT_BROWSER_VALUE
    }

    private fun isCopyWebsiteEnabled(context: Context): Boolean {
        return Settings.Secure.getInt(context.contentResolver, COPY_WEBSITE_ENABLED_KEY, 1) != 0
    }

    private fun preferredBrowserTitle(context: Context): String {
        return AppsTool.getModuleRes(context).getString(R.string.aicr_preferred_browser)
    }

    private fun resolveBrowserDrawable(context: Context, browser: BrowserApp): Drawable? {
        return runCatching {
            loadClass(TARGET_PACKAGE_UTILS_CLASS).callStaticMethod(
                "getDrawable",
                context,
                browser.packageName
            ) as? Drawable
        }.getOrNull() ?: browser.icon
    }

    private fun defaultBrowserText(context: Context): String {
        return AppsTool.getModuleRes(context).getString(R.string.aicr_default_browser)
    }

    private fun getTargetAppContext(): Context? {
        appContext?.let { return it }

        return runCatching {
            loadClass(TARGET_CONTEXT_UTIL_CLASS).callStaticMethod("getApplicationContext") as? Context
        }.getOrNull()?.also {
            appContext = it.applicationContext
        }
    }

    private fun resolveChosenBrowserIcon(context: Context, choice: BrowserChoice): Icon? {
        val browser = resolveChosenBrowserApp(context, choice) ?: return null
        val drawable = resolveBrowserDrawable(context, browser) ?: return null
        return drawable.toNotificationIcon(context)
    }

    private fun Drawable.toNotificationIcon(context: Context): Icon {
        if (this is BitmapDrawable && bitmap != null && !bitmap.isRecycled) {
            return Icon.createWithBitmap(bitmap)
        }

        val notificationIconWidth = runCatching {
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        }.getOrDefault(0)
        val notificationIconHeight = runCatching {
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
        }.getOrDefault(notificationIconWidth)
        val minimumSourceWidth = (notificationIconWidth.takeIf { it > 0 } ?: 1) * 2
        val minimumSourceHeight = (notificationIconHeight.takeIf { it > 0 } ?: minimumSourceWidth) * 2
        val width = maxOf(intrinsicWidth.takeIf { it > 0 } ?: 0, minimumSourceWidth)
        val height = maxOf(intrinsicHeight.takeIf { it > 0 } ?: 0, minimumSourceHeight)

        val bitmap = if (this is AdaptiveIconDrawable) {
            val size = maxOf(width, height)
            toBitmap(width = size, height = size)
        } else {
            toBitmap(width = width, height = height)
        }
        return Icon.createWithBitmap(bitmap)
    }

    private fun ResolveInfo.handlesAllWebDataUri(): Boolean {
        return runCatching {
            findField(javaClass, "handleAllWebDataURI")
                .get(this) as? Boolean
        }.getOrNull() == true
    }

    private fun ResolveInfo.toBrowserApp(
        packageManager: PackageManager,
        requireBrowserCapability: Boolean = true
    ): BrowserApp? {
        val packageName = activityInfo?.packageName ?: return null
        if (packageName == getPackageName() || packageName == "android") return null
        if (requireBrowserCapability && !handlesAllWebDataUri()) return null

        val label = loadLabel(packageManager).toString().takeIf { it.isNotBlank() } ?: packageName
        val icon = runCatching { loadIcon(packageManager) }.getOrNull()
        return BrowserApp(packageName, label, icon)
    }

    private fun String.withHttpsIfMissing(): String {
        return if (startsWith("http://", true) || startsWith("https://", true)) {
            this
        } else {
            "https://$this"
        }
    }
}
