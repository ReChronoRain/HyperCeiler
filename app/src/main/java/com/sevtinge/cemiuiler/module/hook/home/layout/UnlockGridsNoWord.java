package com.sevtinge.cemiuiler.module.hook.home.layout;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class UnlockGridsNoWord extends BaseHook {

    Class<?> mDeviceConfig;

    @Override
    public void init() {
        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");

        findAndHookMethod(mDeviceConfig, "isThemeCoverCellConfig", XC_MethodReplacement.returnConstant(true));
    }
}
