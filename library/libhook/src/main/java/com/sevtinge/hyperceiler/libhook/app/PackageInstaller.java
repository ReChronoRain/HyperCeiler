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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.app;

import android.text.TextUtils;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.AllAsSystemApp;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.DisableAd;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.DisableAppInfoUpload;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.DisableCountChecking;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.DisableInstallerFullSafeVersion;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.DisableSafeModelTip;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.DisplayMoreApkInfoNew;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.InstallRiskDisable;
import com.sevtinge.hyperceiler.libhook.rules.packageinstaller.InstallSource;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.miui.packageinstaller")
public class PackageInstaller extends BaseLoad {

    public PackageInstaller() {
        super(true);
    }

    public void onPackageLoaded() {

        //
        /*initHook(new MiuiPackageInstallModify(), PrefsBridge.getBoolean("miui_package_installer_modify"));*/

        // 禁用广告
        initHook(new DisableAd(), PrefsBridge.getBoolean("miui_package_installer_disable_ad"));

        // 禁用风险检测
        initHook(InstallRiskDisable.INSTANCE, PrefsBridge.getBoolean("miui_package_installer_install_risk"));

        // 禁用安全守护提示
        initHook(DisableSafeModelTip.INSTANCE, PrefsBridge.getBoolean("miui_package_installer_safe_model_tip"));

        // 允许更新系统应用
        initHook(AllAsSystemApp.INSTANCE, PrefsBridge.getBoolean("miui_package_installer_update_system_app"));

        // 自定义安装来源
        initHook(new InstallSource(), !TextUtils.isEmpty(PrefsBridge.getString("miui_package_installer_install_source", "com.android.fileexplorer")));

        // 显示更多安装包信息
        // initHook(new DisplayMoreApkInfo(), PrefsBridge.getBoolean("miui_package_installer_apk_info"));
        initHook(DisplayMoreApkInfoNew.INSTANCE, PrefsBridge.getBoolean("miui_package_installer_apk_info"));
        initHook(new DisableInstallerFullSafeVersion(), PrefsBridge.getBoolean("miui_package_installer_apk_info"));

        // 禁用频繁安装应用检查
        initHook(DisableCountChecking.INSTANCE, PrefsBridge.getBoolean("miui_package_installer_count_checking"));

        // 禁用安装前后上传应用信息, 开启后会无法扫描病毒
        initHook(DisableAppInfoUpload.INSTANCE, PrefsBridge.getBoolean("miui_package_installer_upload_appinfo"));

    }
}
