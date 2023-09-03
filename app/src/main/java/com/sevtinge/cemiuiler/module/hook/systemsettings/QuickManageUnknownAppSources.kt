package com.sevtinge.cemiuiler.module.hook.systemsettings

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.sevtinge.cemiuiler.module.base.BaseHook

class QuickManageUnknownAppSources : BaseHook() {
    override fun init() {
        findAndHookMethod("com.android.settings.SettingsActivity",
            "redirectTabletActivity",
            Bundle::class.java,
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    val intent = (param.thisObject as Activity).intent
                    if (intent.action != Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES || intent.data == null || intent.data!!.scheme != "package") return@before
                    param.thisObject.objectHelper().setObjectUntilSuperclass(
                        "initialFragmentName",
                        "com.android.settings.applications.appinfo.ExternalSourcesDetails"
                    )
                }
            })
    }
}
