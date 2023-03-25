package com.sevtinge.cemiuiler.module.home.folder

import android.view.View
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.callMethod
import com.sevtinge.cemiuiler.utils.woobox.getBooleanField
import com.sevtinge.cemiuiler.utils.woobox.hookAfterMethod

object FolderAutoClose : BaseHook() {
    override fun init() {
        "com.miui.home.launcher.Launcher".hookAfterMethod(
            "launch", "com.miui.home.launcher.ShortcutInfo", View::class.java
        ) {
            val mHasLaunchedAppFromFolder = it.thisObject.getBooleanField("mHasLaunchedAppFromFolder")
            if (mHasLaunchedAppFromFolder) it.thisObject.callMethod("closeFolder")
        }
    }
}