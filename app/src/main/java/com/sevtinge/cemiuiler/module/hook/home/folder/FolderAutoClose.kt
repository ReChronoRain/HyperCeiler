package com.sevtinge.cemiuiler.module.hook.home.folder

import android.view.View
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod
import com.sevtinge.cemiuiler.utils.getBooleanField
import com.sevtinge.cemiuiler.utils.hookAfterMethod

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
