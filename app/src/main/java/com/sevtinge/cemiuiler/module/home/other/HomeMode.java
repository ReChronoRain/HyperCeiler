package com.sevtinge.cemiuiler.module.home.other;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class HomeMode extends BaseHook {

    Class<?> mDeviceConfig;

    @Override
    public void init() {

        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");

        findAndHookMethod(mDeviceConfig, "isDarkMode", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int mHomeMode = mPrefsMap.getStringAsInt("home_other_home_mode",0);
                boolean isHomeMode = !(mHomeMode == 2);
                param.setResult(isHomeMode);
            }
        });
    }
}
