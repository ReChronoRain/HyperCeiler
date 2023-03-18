package com.voyager.star

import com.voyager.star.utils.EasyXposedInit
import com.sevtinge.cemiuiler.BuildConfig
import com.voyager.star.hooks.apps.*
import com.voyager.star.utils.AppRegister
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage

class VoyagerMainHook : EasyXposedInit() {
    private var prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, "config")

    override val registeredApp: List<AppRegister> = listOf(
        MiuiHome, // 桌面
        MediaEditor, // 相册编辑
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        XposedBridge.log("Voyager-Test: MainHook Hook success!")
        if (prefs.getBoolean("main_switch", true)) {
            super.handleLoadPackage(lpparam)
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        super.initZygote(startupParam)
    }

    override fun handleInitPackageResources(resparam: InitPackageResourcesParam?) {
        if (prefs.getBoolean("main_switch", true)) {
            super.handleInitPackageResources(resparam)
        }
    }
}