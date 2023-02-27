package com.sevtinge.cemiuiler

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.sevtinge.cemiuiler.module.base.BaseXposedInit
import com.sevtinge.cemiuiler.module.settings.VolumeSeparateControlForSettings
import com.sevtinge.cemiuiler.module.systemframework.CleanShareMenu
import com.sevtinge.cemiuiler.module.systemframework.ScreenRotation
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForR
import com.sevtinge.cemiuiler.module.thememanager.ThemeCrack
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

private const val TAG = "Cemiuiler"

class XposedInit : BaseXposedInit() {
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
}