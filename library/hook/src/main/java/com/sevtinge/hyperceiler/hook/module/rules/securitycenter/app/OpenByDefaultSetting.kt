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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.rules.securitycenter.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.newInstance
import io.github.kyuubiran.ezxhelper.core.ClassLoaderProvider.classLoader
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext
import java.lang.reflect.Method


@SuppressLint("DiscouragedApi")
// from https://github.com/chsbuffer/MIUIQOL
class OpenByDefaultSetting : BaseHook() {
    private val domainVerificationManager: DomainVerificationManager by lazy(LazyThreadSafetyMode.NONE) {
        appContext.getSystemService(
            DomainVerificationManager::class.java
        )
    }
    private val moduleContext by lazy {
        OtherTool.getModuleRes(appContext)
    }

    private fun getOpenDefaultState(pkgName: String): String {
        val isLinkHandlingAllowed = domainVerificationManager.getDomainVerificationUserState(
            pkgName
        )?.isLinkHandlingAllowed ?: false
        val subTextId =
            if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never
        return moduleContext.getString(subTextId)
    }
    private fun getOpenDefaultTitle(): String = moduleContext.getString(R.string.open_by_default)

    private val appDetailsView by lazy(LazyThreadSafetyMode.NONE) {
        // getClassData 很便宜，不需要前置
        DexKit.initDexkitBridge().getClassData("com.miui.appmanager.fragment.ApplicationsDetailsFragment") ?:
        DexKit.initDexkitBridge().getClassData("com.miui.appmanager.ApplicationsDetailsActivity")!!
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
        DexKit.findMember("onLoadDataFinished") {
            appDetailsView.findMethod {
                matcher {
                    addEqString("enter_way")
                    returnType = "void"
                    paramTypes = listOf("", "")
                }
                findFirst = true
            }.single()
        }
    }

    companion object {
        @JvmStatic
        private fun OpenDefaultOnClick(activity: Activity) {
            val pkgName = activity.intent.getStringExtra("package_name")!!
            logD("OpenByDefaultSetting open default: $pkgName")
            val intent = Intent().apply {
                action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                addCategory(Intent.CATEGORY_DEFAULT)
                data = "package:${pkgName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            activity.startActivity(intent)
        }
    }

    /*private val idAmDetailDefault by lazy {
        appContext.resources.getIdentifier("am_detail_default", "id", lpparam.packageName)
    }
    private val idAmDetailDefaultTitle by lazy {
        appContext.resources.getIdentifier("am_detail_default_title", "id", lpparam.packageName)
    }
    private val drawableAmCardBgSelector by lazy {
        appContext.resources.getIdentifier("am_card_bg_selector", "drawable", lpparam.packageName)
    }
    private val dimenAmDetailsItemHeight by lazy {
        appContext.resources.getIdentifier("am_details_item_height", "dimen", lpparam.packageName)
    }
    private val dimenAmMainPageMarginSe by lazy {
        appContext.resources.getIdentifier("am_main_page_margin_se", "dimen", lpparam.packageName)
    }*/

    override fun init() {
        val appDetailsView = appDetailsView.getInstance(classLoader)

        if (Activity::class.java.isAssignableFrom(appDetailsView)) {
            // v1, v2
            XposedBridge.hookMethod(onLoadDataFinishMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    handleActivityOnLoadDataFinish(param.thisObject as Activity)
                }
            })
        } else {
            // v3
            XposedBridge.hookMethod(onLoadDataFinishMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    handleFragmentOnLoadDataFinish(param.thisObject)
                }
            })

            // injectClassLoader()
            XposedHelpers.findAndHookMethod(appDetailsView,
                "onPreferenceClick",
                "androidx.preference.Preference",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val pref = param.args[0]
                        if (callMethod(pref, "getKey") == "app_default_pref") {
                            val prefFrag = callMethod(param.thisObject, "requireActivity") as Activity
                            OpenDefaultOnClick(prefFrag)
                            param.result = true
                        }
                    }
                })
        }
    }

    // v1, v2
    fun handleActivityOnLoadDataFinish(activity: Activity) {
        var openDefaultView: View? = null
        val default_id = appContext.resources.getIdentifier(
            "am_detail_default", "id", appContext.packageName
        )
        if (default_id != 0) {
            openDefaultView = activity.findViewById(default_id)
        }
        // v2
        openDefaultView = openDefaultView ?: createOpenDefaultView(activity)
        openDefaultView.setOnClickListener { OpenDefaultOnClick(activity) }

        val pkgName = activity.intent.getStringExtra("package_name")!!
        // 加载完毕数据后，修改“清除默认操作”按钮标题和描述为“默认打开”
        setOpenDefaultViewText(openDefaultView, pkgName)
    }

    // v2
    private fun createOpenDefaultView(activity: Activity): View {
        val appmanagerTextBannerViewClass = XposedHelpers.findClass(
            "com.miui.appmanager.widget.AppDetailTextBannerView", classLoader
        )

        val anotherTextBannerId = appContext.resources.getIdentifier(
            "am_global_perm", "id", appContext.packageName
        )
        val anotherTextBanner = activity.findViewById<LinearLayout>(anotherTextBannerId)

        val attributeSet = null
        val defaultView =
            newInstance(appmanagerTextBannerViewClass, activity, attributeSet) as LinearLayout
        copyLinearLayoutStyle(defaultView, anotherTextBanner)

        val insertAfterViewId = appContext.resources.getIdentifier("am_full_screen", "id", appContext.packageName)
        val insertAfterView = activity.findViewById<View>(insertAfterViewId)
        val viewGroup = insertAfterView.parent as ViewGroup
        viewGroup.addView(defaultView, viewGroup.indexOfChild(insertAfterView) + 1)

        return defaultView
    }

    // v2
    private fun copyLinearLayoutStyle(thiz: LinearLayout, that: LinearLayout) {
        thiz.layoutParams = that.layoutParams
        thiz.minimumHeight = that.minimumHeight
        thiz.background = that.background

        thiz.setPadding(
            that.paddingLeft, that.paddingTop, that.paddingRight, that.paddingBottom
        )
        thiz.gravity = that.gravity
        thiz.orientation = that.orientation
    }

    // v1, v2
    private fun setOpenDefaultViewText(cleanDefaultView: View, pkgName: String) {
        // set title
        // 因为 AppDetailTextBannerView 没有 setTitle 方法，
        // 所以先将分别作为 Title 和 Summary 的两个 TextView 的文本都设为 "Open by default"
        // 之后再调用 setSummary 设置 Summary 的 TextView
        cleanDefaultView::class.java.declaredFields.forEach {
            val textView = getObjectField(cleanDefaultView, it.name)
            if (textView !is TextView) return@forEach

            callMethod(
                textView, "setText", arrayOf(CharSequence::class.java), getOpenDefaultTitle()
            )
        }

        // set summary
        callMethod(
            cleanDefaultView, "setSummary", getOpenDefaultState(pkgName)
        )
    }

    // v3
    fun handleFragmentOnLoadDataFinish(prefFrag: Any) {
        val activity = callMethod(prefFrag, "requireActivity") as Activity
        val pkgName = activity.intent.getStringExtra("package_name")!!
        val pref = callMethod(prefFrag, "findPreference", "app_default_pref"
        )
        callMethod(pref, "setTitle", getOpenDefaultTitle())
        callMethod(pref, "setSummary", getOpenDefaultState(pkgName))
    }

    // v3, 为了模块加载宿主 androidx 和 miuix
    @SuppressLint("DiscouragedPrivateApi")
    fun injectClassLoader() {
        val self = this::class.java.classLoader!!
        val loader = self.parent
        val host = classLoader
        val sBootClassLoader: ClassLoader = Context::class.java.classLoader!!

        val fParent = ClassLoader::class.java.getDeclaredField("parent")
        fParent.isAccessible = true
        fParent.set(self, object : ClassLoader(sBootClassLoader) {

            override fun findClass(name: String?): Class<*> {
                logD(TAG, lpparam.packageName, "OpenByDefaultSetting findClass $name")
                try {
                    return sBootClassLoader.loadClass(name)
                } catch (_: ClassNotFoundException) {
                }

                try {
                    return loader.loadClass(name)
                } catch (_: ClassNotFoundException) {
                }
                try {
                    return host.loadClass(name)
                } catch (_: ClassNotFoundException) {
                }

                throw ClassNotFoundException(name)
            }
        })
    }
}
