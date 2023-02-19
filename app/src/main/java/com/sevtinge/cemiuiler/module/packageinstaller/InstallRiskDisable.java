package com.sevtinge.cemiuiler.module.packageinstaller;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class InstallRiskDisable extends BaseHook {


    @Override
    public void init() {

        findAndHookMethod("com.android.packageinstaller.compat.MiuiSettingsCompat",
                "isInstallRiskEnabled",
                Context.class,
                XC_MethodReplacement.returnConstant(false));

        findAndHookMethod("com.android.packageinstaller.compat.MiuiSettingsCompat",
                "isPersonalizedAdEnabled",
                XC_MethodReplacement.returnConstant(false));

        findAndHookMethod("m2.b",
                "s",
                XC_MethodReplacement.returnConstant(false));
    }
}
