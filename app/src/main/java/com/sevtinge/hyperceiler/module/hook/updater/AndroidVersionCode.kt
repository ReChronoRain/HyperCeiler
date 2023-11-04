package com.sevtinge.hyperceiler.module.hook.updater

import android.os.Build
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers

object AndroidVersionCode : BaseHook() {
    private val mAndroidVersionCode =
        mPrefsMap.getString("various_updater_android_version", "14")

    override fun init() {
        XposedHelpers.setStaticObjectField(Build.VERSION::class.java, "RELEASE", mAndroidVersionCode)
    }
}
