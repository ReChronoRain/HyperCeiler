package com.sevtinge.cemiuiler.module.hook.screenrecorder

import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SaveToMovies : BaseHook() {
    override fun init() {
        val clazz = XposedHelpers.findClass("android.os.Environment", lpparam.classLoader)
        XposedHelpers.setStaticObjectField(clazz, "DIRECTORY_DCIM", "Movies")

        XposedHelpers.findAndHookMethod("android.content.ContentValues",
            lpparam.classLoader,
            "put",
            String::class.java,
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == "relative_path") {
                        param.args[1] = (param.args[1] as String).replace("DCIM", "Movies")
                    }
                }
            })
    }
}
