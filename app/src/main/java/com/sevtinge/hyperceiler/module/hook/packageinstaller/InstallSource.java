package com.sevtinge.hyperceiler.module.hook.packageinstaller;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class InstallSource extends BaseHook {

    String mInstallSourcePackageName;

    @Override
    public void init() {

        mInstallSourcePackageName = mPrefsMap.getString("miui_package_installer_install_source", "com.android.fileexplorer");

        findAndHookMethodSilently("com.miui.packageInstaller.InstallStart",
            "getCallingPackage",
            XC_MethodReplacement.returnConstant(mInstallSourcePackageName));
    }
}
