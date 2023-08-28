package com.sevtinge.cemiuiler.module.securitycenter.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.EzXHelper.initAppContext
import com.github.kyuubiran.ezxhelper.EzXHelper.moduleRes
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.R
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import io.luckypray.dexkit.enums.MatchType

object OpenByDefaultSetting : BaseHook() {
    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("DiscouragedApi")
    override fun init() {
        val domainVerificationManager: DomainVerificationManager by lazy {
            appContext.getSystemService(
                DomainVerificationManager::class.java
            )
        }
        loadClass("com.miui.appmanager.ApplicationsDetailsActivity").methodFinder()
            .filterByName("onClick")
            .first().createHook {
                before { param ->
                    initAppContext(param.thisObject as Activity)
                    val clickedView = param.args[0]
                    val cleanOpenByDefaultView = (param.thisObject as Activity).findViewById<View>(
                        appContext.resources.getIdentifier(
                            "am_detail_default", "id", hostPackageName
                        )
                    )
                    val pkgName =
                        (param.thisObject as Activity).intent.getStringExtra("package_name")!!
                    if (clickedView == cleanOpenByDefaultView) {
                        val intent = Intent().apply {
                            action = android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                            addCategory(Intent.CATEGORY_DEFAULT)
                            data = android.net.Uri.parse("package:${pkgName}")
                            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }
                        invokeMethodBestMatch(param.thisObject, "startActivity", null, intent)
                        param.result = null
                    }
                }
            }

        initDexKit(lpparam)
        dexKitBridge.findMethodUsingString {
            usingString = "enter_way"
            matchType = MatchType.CONTAINS
            methodDeclareClass = "Lcom/miui/appmanager/ApplicationsDetailsActivity;"
            methodReturnType = "void"
            methodParamTypes = arrayOf("", "Ljava/lang/Boolean;")
        }.firstOrNull()?.getMethodInstance(safeClassLoader)?.createHook {
            after { param ->
                initAppContext(param.thisObject as Activity)
                val cleanOpenByDefaultView = (param.thisObject as Activity).findViewById<View>(
                    appContext.resources.getIdentifier(
                        "am_detail_default", "id", hostPackageName
                    )
                )
                val pkgName = (param.thisObject as Activity).intent.getStringExtra("package_name")!!
                val isLinkHandlingAllowed =
                    domainVerificationManager.getDomainVerificationUserState(
                        pkgName
                    )?.isLinkHandlingAllowed ?: false
                val subTextId =
                    if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never
                cleanOpenByDefaultView::class.java.declaredFields.forEach {
                    val view = getObjectOrNull(cleanOpenByDefaultView, it.name)
                    if (view !is TextView) return@forEach
                    invokeMethodBestMatch(
                        view,
                        "setText",
                        null,
                        moduleRes.getString(R.string.open_by_default)
                    )
                }
                invokeMethodBestMatch(
                    cleanOpenByDefaultView,
                    "setSummary",
                    null,
                    moduleRes.getString(subTextId)
                )
            }
        }
    }
}
