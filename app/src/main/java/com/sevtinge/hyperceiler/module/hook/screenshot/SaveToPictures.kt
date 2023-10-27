package com.sevtinge.hyperceiler.module.hook.screenshot

import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers

object SaveToPictures : BaseHook() {
    override fun init() {
        val clazz = XposedHelpers.findClass("android.os.Environment", lpparam.classLoader)
        XposedHelpers.setStaticObjectField(clazz, "DIRECTORY_DCIM", "Pictures")
    }
}
