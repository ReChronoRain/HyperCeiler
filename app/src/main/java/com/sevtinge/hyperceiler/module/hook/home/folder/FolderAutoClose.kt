package com.sevtinge.hyperceiler.module.hook.home.folder

import android.view.View
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.getBooleanField
import com.sevtinge.hyperceiler.utils.hookAfterMethod

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
