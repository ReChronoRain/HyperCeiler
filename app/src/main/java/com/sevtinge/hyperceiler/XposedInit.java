/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.sevtinge.hyperceiler.module.app.SystemFrameworkForCorePatch;
import com.sevtinge.hyperceiler.module.base.BaseXposedInit;
import com.sevtinge.hyperceiler.module.hook.home.other.AllowShareApk;
import com.sevtinge.hyperceiler.module.hook.home.title.EnableIconMonetColor;
import com.sevtinge.hyperceiler.module.hook.securitycenter.SidebarLineCustom;
import com.sevtinge.hyperceiler.module.hook.systemframework.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.module.hook.systemframework.AllowUninstall;
import com.sevtinge.hyperceiler.module.hook.systemframework.BackgroundBlurDrawable;
import com.sevtinge.hyperceiler.module.hook.systemframework.CleanOpenMenu;
import com.sevtinge.hyperceiler.module.hook.systemframework.CleanShareMenu;
import com.sevtinge.hyperceiler.module.hook.systemframework.ScreenRotation;
import com.sevtinge.hyperceiler.module.hook.systemsettings.VolumeSeparateControlForSettings;
import com.sevtinge.hyperceiler.module.hook.systemui.navigation.HandleLineCustom;
import com.sevtinge.hyperceiler.module.hook.tsmclient.AutoNfc;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit extends BaseXposedInit implements IXposedHookInitPackageResources, IXposedHookZygoteInit, IXposedHookLoadPackage {
    private final String TAG = "HyperCeiler";

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        super.initZygote(startupParam);
        EzXHelper.initZygote(startupParam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);
        if (mPrefsMap.getBoolean("system_framework_allow_uninstall"))
            new AllowUninstall().initZygote(startupParam);
        if (mPrefsMap.getBoolean("system_framework_screen_all_rotations")) ScreenRotation.initRes();
        if (mPrefsMap.getBoolean("system_framework_clean_share_menu")) CleanShareMenu.initRes();
        if (mPrefsMap.getBoolean("system_framework_clean_open_menu")) CleanOpenMenu.initRes();
        if (mPrefsMap.getBoolean("system_framework_volume_separate_control"))
            VolumeSeparateControlForSettings.initRes();
        if (mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"))
            new AllowManageAllNotifications().initZygote(startupParam);
        if (startupParam != null) {
            new BackgroundBlurDrawable().initZygote(startupParam);
            new SystemFrameworkForCorePatch().initZygote(startupParam);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        init(lpparam);
        new SystemFrameworkForCorePatch().handleLoadPackage(lpparam);
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        switch (resparam.packageName) {
            case "com.miui.tsmclient":
                if (mPrefsMap.getBoolean("tsmclient_auto_nfc")) {
                    AutoNfc.INSTANCE.initResource(resparam);
                }
                break;

            case "com.miui.home":
                if (mPrefsMap.getBoolean("home_other_icon_monet_color")) {
                    EnableIconMonetColor.INSTANCE.initResource(resparam);
                }
                if (mPrefsMap.getBoolean("home_other_allow_share_apk")) {
                    new AllowShareApk().initResource(resparam);
                }
                break;

            case "com.miui.securitycenter":
                if (mPrefsMap.getBoolean("security_center_sidebar_line_color")) {
                    SidebarLineCustom.INSTANCE.initResource(resparam);
                }
                break;

            case "com.android.systemui":
                if (mPrefsMap.getBoolean("system_ui_navigation_handle_custom")) {
                    HandleLineCustom.INSTANCE.initResource(resparam);
                }
                break;
        }
    }
}
