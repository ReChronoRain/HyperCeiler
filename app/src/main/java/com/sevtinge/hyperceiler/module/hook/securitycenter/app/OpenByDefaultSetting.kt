package com.sevtinge.hyperceiler.module.hook.securitycenter.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.sevtinge.hyperceiler.R
import com.sevtinge.hyperceiler.module.base.BaseHook


object OpenByDefaultSetting : BaseHook() {
    @RequiresApi(Build.VERSION_CODES.S)
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
    }
}
