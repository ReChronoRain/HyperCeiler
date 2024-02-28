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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.securitycenter.app

/*import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext*/
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.initAppContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.R
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField
import de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField


@SuppressLint("DiscouragedApi")
object OpenByDefaultSetting : BaseHook() {
    private val domainVerificationManager: DomainVerificationManager by lazy {
        appContext.getSystemService(
            DomainVerificationManager::class.java
        )
    }
    private val idAmDetailDefault by lazy {
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
    }

    override fun init() {
        val clazzApplicationsDetailsActivity =
            loadClass("com.miui.appmanager.ApplicationsDetailsActivity")
        clazzApplicationsDetailsActivity.methodFinder().filterByName("initView").first()
            .createHook {
                after { param ->
                    val activity = param.thisObject as Activity
                    initAppContext(activity, true)
                    var cleanOpenByDefaultView: View? = activity.findViewById(idAmDetailDefault)
                    if (cleanOpenByDefaultView == null) {
                        val viewAmDetailDefaultTitle =
                            activity.findViewById<View>(idAmDetailDefaultTitle)
                        val linearLayout = viewAmDetailDefaultTitle.parent as LinearLayout
                        cleanOpenByDefaultView =
                            (findClass("com.miui.appmanager.widget.AppDetailBannerItemView").constructorFinder()
                                .filterByParamCount(2).first()
                                .newInstance(activity, null) as LinearLayout).apply {
                                gravity = Gravity.CENTER_VERTICAL
                                orientation = LinearLayout.HORIZONTAL
                                setBackgroundResource(drawableAmCardBgSelector)
                                isClickable = true
                                minimumHeight = activity.resources.getDimensionPixelSize(
                                    dimenAmDetailsItemHeight
                                )
                                val dimensionPixelSize =
                                    activity.resources.getDimensionPixelSize(dimenAmMainPageMarginSe)
                                setPadding(dimensionPixelSize, 0, dimensionPixelSize, 0)
                            }
                        cleanOpenByDefaultView.setOnClickListener {
                            startActionAppOpenByDefaultSettings(activity)
                        }
                        linearLayout.addView(cleanOpenByDefaultView)
                    }
                    setAdditionalInstanceField(
                        activity, "cleanOpenByDefaultView", cleanOpenByDefaultView
                    )
                    val pkgName = activity.intent.getStringExtra("package_name")!!
                    val isLinkHandlingAllowed =
                        domainVerificationManager.getDomainVerificationUserState(
                            pkgName
                        )?.isLinkHandlingAllowed ?: false
                    invokeMethodBestMatch(
                        cleanOpenByDefaultView, "setTitle", null, R.string.open_by_default
                    )
                    invokeMethodBestMatch(
                        cleanOpenByDefaultView,
                        "setSummary",
                        null,
                        if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never
                    )
                }
            }
        clazzApplicationsDetailsActivity.methodFinder().filterByName("onClick").first().createHook {
            before { param ->
                val activity = param.thisObject as Activity
                initAppContext(activity, true)
                val clickedView = param.args[0]
                val cleanOpenByDefaultView =
                    getAdditionalInstanceField(activity, "cleanOpenByDefaultView")
                if (clickedView == cleanOpenByDefaultView) {
                    startActionAppOpenByDefaultSettings(activity)
                    param.result = null
                }
            }
        }
    }

    private fun startActionAppOpenByDefaultSettings(activity: Activity) {
        val pkgName = activity.intent.getStringExtra("package_name")!!
        val intent = Intent().apply {
            action = android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
            addCategory(Intent.CATEGORY_DEFAULT)
            data = android.net.Uri.parse("package:${pkgName}")
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        invokeMethodBestMatch(activity, "startActivity", null, intent)
    }
    /*@RequiresApi(Build.VERSION_CODES.S)
    override fun init() {
        val domainVerificationManager: DomainVerificationManager by lazy {
            appContext.getSystemService(
                DomainVerificationManager::class.java
            )
        }

        val defaultViewId = intArrayOf(-1)
        findAndHookMethod(
            "com.miui.appmanager.ApplicationsDetailsActivity",
            lpparam.classLoader,
            "initView",
            object : MethodHook() {
                @SuppressLint("DiscouragedApi")
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    if (defaultViewId[0] == -1) {
                        val act = param.thisObject as Activity
                        val pkgName =
                            (param.thisObject as Activity).intent.getStringExtra("package_name")!!
                        val isLinkHandlingAllowed =
                            domainVerificationManager.getDomainVerificationUserState(
                                pkgName
                            )?.isLinkHandlingAllowed ?: false
                        val subTextId =
                            if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never

                        defaultViewId[0] = act.resources.getIdentifier(
                            "am_detail_default",
                            "id",
                            "com.miui.securitycenter"
                        )
                        mResHook.setResReplacement(
                            "com.miui.securitycenter",
                            "string",
                            "app_manager_default_open_title",
                            R.string.open_by_default
                        )

                        mResHook.setResReplacement(
                            "com.miui.securitycenter",
                            "string",
                            "app_manager_default_close_summary",
                            subTextId
                        )
                        mResHook.setResReplacement(
                            "com.miui.securitycenter",
                            "string",
                            "app_manager_default_open_summary",
                            subTextId
                        )
                    }
                }
            })

        findAndHookMethod(
            "com.miui.appmanager.ApplicationsDetailsActivity",
            lpparam.classLoader,
            "onClick",
            View::class.java,
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    val view = param.args[0] as View
                    if (view.id == defaultViewId[0] && defaultViewId[0] != -1) {
                        val act = param.thisObject as Activity
                        val intent = Intent("android.settings.APP_OPEN_BY_DEFAULT_SETTINGS")
                        val pkgName = act.intent.getStringExtra("package_name")
                        intent.setData(Uri.parse("package:$pkgName"))
                        act.startActivity(intent)
                        param.setResult(null)
                    }
                }
            })
    }*/
}
