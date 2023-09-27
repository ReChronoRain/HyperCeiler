package com.sevtinge.cemiuiler.module.hook.powerkeeper;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

public class PreventBatteryWitelist extends BaseHook {
    @Override
    public void init() {
        Helpers.hookAllMethods("com.miui.powerkeeper.utils.CommonAdapter", lpparam.classLoader, "addPowerSaveWhitelistApps", new MethodHook(20000) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
