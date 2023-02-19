package com.sevtinge.cemiuiler.module.packageinstaller

import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

class DisableSafeModelTip : BaseHook() {
    override fun init() {
        findAndHookMethod(
            "com.miui.packageInstaller.model.ApkInfo",
            "getSystemApp",
            XC_MethodReplacement.returnConstant(true)
        )
        hookAllMethods(
            "com.miui.packageInstaller.InstallProgressActivity",
            "g0",
            XC_MethodReplacement.returnConstant(false)
        )
        XposedHelpers.findAndHookMethod(
            "com.miui.packageInstaller.InstallProgressActivity",
            lpparam.classLoader,
            "Q1",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    param.result = ArrayList<Any>()
                }
            })

        //returnIntConstant(findClassIfExists("p6.a"), "d");
    }
}