package com.sevtinge.hyperceiler.module.hook.packageinstaller;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class SafeMode extends BaseHook {

    @Override
    public void init() {

        findAndHookMethod("com.android.packageinstaller.compat.MiuiSettingsCompat",
            "isSafeModelEnable",
            Context.class,
            XC_MethodReplacement.returnConstant(mPrefsMap.getBoolean("miui_package_installer_safe_mode")));
    }
}
