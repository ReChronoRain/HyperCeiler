package com.sevtinge.hyperceiler

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.sevtinge.hyperceiler.module.app.SystemFrameworkForCorePatch
import com.sevtinge.hyperceiler.module.base.BaseXposedInit
import com.sevtinge.hyperceiler.module.hook.home.title.EnableIconMonetColor
import com.sevtinge.hyperceiler.module.hook.securitycenter.SidebarLineCustom
import com.sevtinge.hyperceiler.module.hook.settings.VolumeSeparateControlForSettings
import com.sevtinge.hyperceiler.module.hook.systemframework.*
import com.sevtinge.hyperceiler.module.hook.systemui.navigation.HandleLineCustom
import com.sevtinge.hyperceiler.module.hook.tsmclient.AutoNfc
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

private const val TAG = "HyperCeiler"

class XposedInit : BaseXposedInit(), IXposedHookInitPackageResources {
    @Throws(Throwable::class)
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        super.initZygote(startupParam)
        if (mPrefsMap.getBoolean("system_framework_allow_uninstall")) AllowUninstall().initZygote(startupParam)
        if (mPrefsMap.getBoolean("system_framework_screen_all_rotations")) ScreenRotation.initRes()
        if (mPrefsMap.getBoolean("system_framework_clean_share_menu")) CleanShareMenu.initRes()
        if (mPrefsMap.getBoolean("system_framework_clean_open_menu")) CleanOpenMenu.initRes()
        if (mPrefsMap.getBoolean("system_framework_volume_separate_control")) VolumeSeparateControlForSettings.initRes()
        if (startupParam != null) {
            BackgroundBlurDrawable().initZygote(startupParam)
            SystemFrameworkForCorePatch().initZygote(startupParam)
        }
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Init EzXHelper
        EzXHelper.apply {
            initHandleLoadPackage(lpparam)
            setLogTag(TAG)
            setToastTag(TAG)
        }

        init(lpparam)
        //CrashRecord.init(EzXHelper.appContext)
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
