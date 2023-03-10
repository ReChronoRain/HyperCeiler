package com.sevtinge.cemiuiler

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.sevtinge.cemiuiler.module.base.BaseXposedInit
import com.sevtinge.cemiuiler.module.home.EnableIconMonetColor
import com.sevtinge.cemiuiler.module.settings.VolumeSeparateControlForSettings
import com.sevtinge.cemiuiler.module.systemframework.CleanShareMenu
import com.sevtinge.cemiuiler.module.systemframework.ScreenRotation
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForR
import com.sevtinge.cemiuiler.module.thememanager.ThemeCrack
import com.sevtinge.cemiuiler.module.tsmclient.AutoNfc
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

private const val TAG = "Cemiuiler"

class XposedInit : BaseXposedInit(), IXposedHookInitPackageResources {
    @Throws(Throwable::class)
    override fun initZygote(startupParam: StartupParam) {
        super.initZygote(startupParam)
        //CorePatchForR.initZygote()
        if (mPrefsMap.getBoolean("system_framework_screen_all_rotations")) ScreenRotation.initRes()
        if (mPrefsMap.getBoolean("system_framework_clean_share_menu")) CleanShareMenu.initRes()
        if (mPrefsMap.getBoolean("system_framework_volume_separate_control")) VolumeSeparateControlForSettings.initRes()
        //if (mPrefsMap.getBoolean("various_theme_crack")) ThemeCrack.initRes()
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Init EzXHelper
        EzXHelperInit.initHandleLoadPackage(lpparam)
        EzXHelperInit.setLogTag(TAG)
        EzXHelperInit.setToastTag(TAG)
        init(lpparam)
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        when (resparam.packageName) {
            "com.miui.tsmclient" -> if (mPrefsMap.getBoolean("tsmclient_auto_nfc")) {
                AutoNfc.initResource(resparam)
            }
            "com.miui.home" -> if (mPrefsMap.getBoolean("home_other_icon_monet_color")) {
                EnableIconMonetColor.initResource(resparam)
            }
        }
    }
}