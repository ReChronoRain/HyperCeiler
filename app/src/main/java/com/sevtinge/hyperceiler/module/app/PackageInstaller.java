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
package com.sevtinge.hyperceiler.module.app;

import android.text.TextUtils;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.AllAsSystemApp;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.DisableAd;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.DisableAppInfoUpload;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.DisableCountChecking;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.DisableInstallerFullSafeVersion;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.DisableSafeModelTip;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.DisplayMoreApkInfoNew;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.InstallRiskDisable;
import com.sevtinge.hyperceiler.module.hook.packageinstaller.InstallSource;

@HookBase(targetPackage = "com.miui.packageinstaller",  isPad = false)
public class PackageInstaller extends BaseModule {

    public void handleLoadPackage() {

        //
        /*initHook(new MiuiPackageInstallModify(), mPrefsMap.getBoolean("miui_package_installer_modify"));*/

        // 禁用广告
        initHook(new DisableAd(), mPrefsMap.getBoolean("miui_package_installer_disable_ad"));

        // 禁用风险检测
        initHook(InstallRiskDisable.INSTANCE, mPrefsMap.getBoolean("miui_package_installer_install_risk"));

        // 禁用安全守护提示
        initHook(DisableSafeModelTip.INSTANCE, mPrefsMap.getBoolean("miui_package_installer_safe_model_tip"));

        // 允许更新系统应用
        initHook(AllAsSystemApp.INSTANCE, mPrefsMap.getBoolean("miui_package_installer_update_system_app"));

        // 自定义安装来源
        initHook(new InstallSource(), !TextUtils.isEmpty(mPrefsMap.getString("miui_package_installer_install_source", "com.android.fileexplorer")));

        // 显示更多安装包信息
        // initHook(new DisplayMoreApkInfo(), mPrefsMap.getBoolean("miui_package_installer_apk_info"));
        initHook(DisplayMoreApkInfoNew.INSTANCE, mPrefsMap.getBoolean("miui_package_installer_apk_info"));
        initHook(new DisableInstallerFullSafeVersion(), mPrefsMap.getBoolean("miui_package_installer_apk_info"));

        // 禁用频繁安装应用检查
        initHook(DisableCountChecking.INSTANCE, mPrefsMap.getBoolean("miui_package_installer_count_checking"));

        // 禁用安装前后上传应用信息, 开启后会无法扫描病毒
        initHook(DisableAppInfoUpload.INSTANCE, mPrefsMap.getBoolean("miui_package_installer_upload_appinfo"));

    }
}
