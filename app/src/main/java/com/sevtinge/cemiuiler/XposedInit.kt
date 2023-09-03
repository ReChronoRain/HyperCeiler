package com.sevtinge.cemiuiler

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.sevtinge.cemiuiler.module.app.SystemFrameworkForCorePatch
import com.sevtinge.cemiuiler.module.base.BaseXposedInit
import com.sevtinge.cemiuiler.module.hook.home.title.EnableIconMonetColor
import com.sevtinge.cemiuiler.module.hook.securitycenter.SidebarLineCustom
import com.sevtinge.cemiuiler.module.hook.settings.VolumeSeparateControlForSettings
import com.sevtinge.cemiuiler.module.hook.systemframework.*
import com.sevtinge.cemiuiler.module.hook.systemui.navigation.HandleLineCustom
import com.sevtinge.cemiuiler.module.hook.tsmclient.AutoNfc
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

private const val TAG = "Cemiuiler"

class XposedInit : BaseXposedInit(), IXposedHookInitPackageResources {
    @Throws(Throwable::class)
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        super.initZygote(startupParam)
        if (mPrefsMap.getBoolean("system_framework_allow_uninstall")) AllowUninstall().initZygote(startupParam)
        if (mPrefsMap.getBoolean("system_framework_screen_all_rotations")) ScreenRotation.initRes()
        if (mPrefsMap.getBoolean("system_framework_clean_share_menu")) CleanShareMenu.initRes()
        if (mPrefsMap.getBoolean("system_framework_clean_open_menu")) CleanOpenMenu.initRes()
        if (mPrefsMap.getBoolean("system_framework_volume_separate_control")) VolumeSeparateControlForSettings.initRes()
        // if (mPrefsMap.getBoolean("various_theme_crack")) ThemeCrack.initRes()
        if (startupParam != null) {
            BackgroundBlurDrawable().initZygote(startupParam)
            SystemFrameworkForCorePatch().initZygote(startupParam)
        }
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Init EzXHelper
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag(TAG)
        EzXHelper.setToastTag(TAG)

        init(lpparam)
        // CrashRecord.init(appContext) 暂停使用，误触率高，等优化
        SystemFrameworkForCorePatch().handleLoadPackage(lpparam)
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        when (resparam.packageName) {
            "com.miui.tsmclient" ->
                if (mPrefsMap.getBoolean("tsmclient_auto_nfc")) {
                    AutoNfc.initResource(resparam)
                }

            "com.miui.home" ->
                if (mPrefsMap.getBoolean("home_other_icon_monet_color")) {
                    EnableIconMonetColor.initResource(resparam)
                }

            "com.miui.securitycenter" ->
                if (mPrefsMap.getBoolean("security_center_sidebar_line_color")) {
                    SidebarLineCustom.initResource(resparam)
                }

            "com.android.systemui" ->
                if (mPrefsMap.getBoolean("system_ui_navigation_handle_custom")) {
                    HandleLineCustom.initResource(resparam)
                }

        }
    }
}
