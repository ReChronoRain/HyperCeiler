/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.verify.domain.DomainVerificationManager
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import io.github.lingqiqi5211.ezhooktool.core.argTypes
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findAllFields
import io.github.lingqiqi5211.ezhooktool.core.findConstructor
import io.github.lingqiqi5211.ezhooktool.core.findFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed.appContext
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull
import java.lang.reflect.Method

@SuppressLint("DiscouragedApi")
// from https://github.com/chsbuffer/MIUIQOL
class OpenByDefaultSetting : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        appDetailsViewClass
        onLoadDataFinishMethod
        return true
    }

    private val domainVerificationManager: DomainVerificationManager by lazy(LazyThreadSafetyMode.NONE) {
        appContext.getSystemService(DomainVerificationManager::class.java)
    }

    private val moduleContext by lazy(LazyThreadSafetyMode.NONE) {
        AppsTool.getModuleRes(appContext)
    }

    private val appDetailsViewClass by lazy<Class<*>>(LazyThreadSafetyMode.NONE) {
        requiredMember("appDetailsViewClass") { bridge ->
            bridge.getClassData(CLASS_DETAILS_FRAGMENT)
                ?: bridge.getClassData(CLASS_DETAILS_ACTIVITY)
                ?: throw IllegalStateException("app details view class not found")
        }
    }

    /** LiveData 读取后更新 View 的方法 */
    private val onLoadDataFinishMethod by lazy<Method>(LazyThreadSafetyMode.NONE) {
        //
        //  public void a(a.j.b.c<Boolean> cVar, Boolean bool) {                      // <- a
        //      ……
        //      if (this.k0) {
        //          appDetailTextBannerView = this.p;
        //          i2 = R.string.app_manager_default_open_summary;
        //      } else {
        //          appDetailTextBannerView = this.p;
        //          i2 = R.string.app_manager_default_close_summary;
        //      }
        requiredMember("onLoadDataFinished") { bridge ->
            val classData = bridge.getClassData(appDetailsViewClass)
                ?: throw IllegalStateException("app details view class data not found")
            classData.findMethod {
                matcher {
                    addEqString("enter_way")
                    returnType = "void"
                    paramCount = 2
                }
                findFirst = true
            }.single()
        }
    }

    override fun init() {
        val appDetailsView = appDetailsViewClass

        if (Activity::class.java.isAssignableFrom(appDetailsView)) {
            onLoadDataFinishMethod.createAfterHook { param ->
                (param.thisObject as? Activity)?.let(::handleActivityOnLoadDataFinish)
            }
            return
        }

        onLoadDataFinishMethod.createAfterHook { param ->
            handleFragmentOnLoadDataFinish(param.thisObject)
        }

        val clickMethod = runCatching {
            appDetailsView.findMethod {
                name("onPreferenceClick")
                paramCount(1)
                returnType(Boolean::class.javaPrimitiveType as Class<*>)
            }
        }.getOrNull()
        if (clickMethod == null) {
            XposedLog.w(TAG, packageName, "onPreferenceClick method not found")
            return
        }
        clickMethod.createBeforeHook { param ->
            val pref = param.args.firstOrNull() ?: return@createBeforeHook
            if (pref.callMethod("getKey") != KEY_OPEN_BY_DEFAULT) {
                return@createBeforeHook
            }
            val activity = param.thisObject.callMethod("requireActivity") as? Activity ?: return@createBeforeHook
            val pkgName = getTargetPackageName(activity, param.thisObject) ?: return@createBeforeHook
            openDefaultOnClick(activity, pkgName)
            param.result = true
        }
    }

    // v1, v2
    private fun handleActivityOnLoadDataFinish(activity: Activity) {
        val openDefaultView = findOpenDefaultView(activity) ?: createOpenDefaultView(activity) ?: return
        val pkgName = getTargetPackageName(activity, null) ?: return

        openDefaultView.setOnClickListener { openDefaultOnClick(activity, pkgName) }
        setOpenDefaultViewText(openDefaultView, pkgName)
    }

    // v3
    private fun handleFragmentOnLoadDataFinish(fragment: Any) {
        val activity = fragment.callMethod("requireActivity") as? Activity ?: return
        val pkgName = getTargetPackageName(activity, fragment) ?: return
        val pref = findOpenDefaultPreference(fragment) ?: run {
            XposedLog.w(TAG, packageName, "open by default preference not found")
            return
        }

        pref.callMethod("setTitle", getOpenDefaultTitle())
        pref.callMethod("setSummary", getOpenDefaultState(pkgName))
    }

    private fun openDefaultOnClick(activity: Activity, pkgName: String) {
        XposedLog.d(TAG, packageName, "Open default settings: $pkgName")
        val intent = Intent().apply {
            action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
            addCategory(Intent.CATEGORY_DEFAULT)
            data = "package:$pkgName".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        try {
            activity.startActivity(intent)
        } catch (t: Throwable) {
            XposedLog.w(TAG, packageName, "Failed to open default settings for $pkgName", t)
        }
    }

    private fun getOpenDefaultState(pkgName: String): String {
        val allowed = try {
            domainVerificationManager.getDomainVerificationUserState(pkgName)?.isLinkHandlingAllowed ?: false
        } catch (t: Throwable) {
            XposedLog.w(TAG, packageName, "Failed to read open default state for $pkgName", t)
            false
        }
        val subTextId = if (allowed) R.string.app_link_open_always else R.string.app_link_open_never
        return moduleContext.getString(subTextId)
    }

    private fun getOpenDefaultTitle(): String =
        moduleContext.getString(R.string.open_by_default)

    private fun findOpenDefaultView(activity: Activity): View? {
        val id = appContext.getIdByName("am_detail_default", "id", appContext.packageName)
        return if (id == 0) null else activity.findViewById(id)
    }

    // v2
    private fun createOpenDefaultView(activity: Activity): View? {
        val viewClass = findClassIfExists(CLASS_TEXT_BANNER_VIEW, classLoader) ?: run {
            XposedLog.w(TAG, packageName, "AppDetailTextBannerView class not found")
            return null
        }
        val sourceId = appContext.getIdByName("am_global_perm", "id", appContext.packageName)
        val sourceView = activity.findViewById<LinearLayout>(sourceId) ?: run {
            XposedLog.w(TAG, packageName, "template banner view not found")
            return null
        }
        val insertAfterId = appContext.getIdByName("am_full_screen", "id", appContext.packageName)
        val insertAfterView = activity.findViewById<View>(insertAfterId) ?: run {
            XposedLog.w(TAG, packageName, "insert anchor view not found")
            return null
        }
        val parent = insertAfterView.parent as? ViewGroup ?: return null
        val defaultView = viewClass.findConstructor {
            paramCount(2)
        }.newInstance(activity, null) as? LinearLayout ?: return null

        copyLinearLayoutStyle(defaultView, sourceView)
        parent.addView(defaultView, parent.indexOfChild(insertAfterView) + 1)
        return defaultView
    }

    // v2
    private fun copyLinearLayoutStyle(target: LinearLayout, source: LinearLayout) {
        target.layoutParams = source.layoutParams
        target.minimumHeight = source.minimumHeight
        target.background = source.background
        target.setPadding(source.paddingLeft, source.paddingTop, source.paddingRight, source.paddingBottom)
        target.gravity = source.gravity
        target.orientation = source.orientation
    }

    // v1, v2
    private fun setOpenDefaultViewText(openDefaultView: View, pkgName: String) {
        // set title
        // 因为 AppDetailTextBannerView 没有 setTitle 方法，
        // 所以先将分别作为 Title 和 Summary 的两个 TextView 的文本都设为 "Open by default"
        // 之后再调用 setSummary 设置 Summary 的 TextView
        openDefaultView.javaClass.findAllFields { argTypes(TextView::class.java) }.toList().forEach { field ->
            val textView = field.get(openDefaultView) as? TextView ?: return@forEach
            textView.callMethod("setText", getOpenDefaultTitle())
        }
        try {
            // set summary
            openDefaultView.callMethod("setSummary", getOpenDefaultState(pkgName))
        } catch (t: Throwable) {
            XposedLog.w(TAG, packageName, "Failed to update open default summary", t)
        }
    }

    private fun findOpenDefaultPreference(fragment: Any): Any? =
        try {
            fragment.callMethod("findPreference", KEY_OPEN_BY_DEFAULT)
        } catch (_: Throwable) {
            null
    }

    private fun getTargetPackageName(activity: Activity, owner: Any?): String? {
        val packageInfo = owner?.let {
            runCatching {
                it.javaClass.findFieldOrNull {
                    argTypes(PackageInfo::class.java)
                }?.get(it) as? PackageInfo
            }.getOrNull()
        }
        packageInfo?.packageName?.takeIf { it.isNotBlank() }?.let { return it }
        getPackageNameFromIntent(activity.intent)?.let { return it }
        FIELD_PACKAGE_NAMES.firstNotNullOfOrNull { name ->
            runCatching { owner?.getObjectFieldOrNull(name) as? String }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }?.let { return it }
        FIELD_PACKAGE_NAMES.firstNotNullOfOrNull { name ->
            runCatching { activity.getObjectFieldOrNull(name) as? String }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }?.let { return it }

        XposedLog.w(TAG, packageName, "target package name not found")
        return null
    }

    private fun getPackageNameFromIntent(intent: Intent): String? {
        PACKAGE_EXTRA_KEYS.forEach { key ->
            try {
                intent.getStringExtra(key)?.takeIf { it.isNotBlank() }?.let { return it }
            } catch (_: Throwable) {
            }
        }
        val data = intent.data
        return if (data?.scheme == "package") data.schemeSpecificPart?.takeIf { it.isNotBlank() } else null
    }

    private companion object {
        private const val KEY_OPEN_BY_DEFAULT = "app_default_pref"
        private const val CLASS_DETAILS_FRAGMENT = "com.miui.appmanager.fragment.ApplicationsDetailsFragment"
        private const val CLASS_DETAILS_ACTIVITY = "com.miui.appmanager.ApplicationsDetailsActivity"
        private const val CLASS_TEXT_BANNER_VIEW = "com.miui.appmanager.widget.AppDetailTextBannerView"

        private val PACKAGE_EXTRA_KEYS = arrayOf(
            "package_name",
            "pkg_name",
            "pkgName",
            "packageName",
            "app_package_name",
            "miui.intent.extra.PACKAGE_NAME"
        )

        private val FIELD_PACKAGE_NAMES = arrayOf(
            "mPackageName",
            "packageName",
            "mPkgName",
            "pkgName",
            "mPkg"
        )
    }
}
