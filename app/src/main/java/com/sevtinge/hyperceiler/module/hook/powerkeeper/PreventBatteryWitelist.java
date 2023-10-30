package com.sevtinge.hyperceiler.module.hook.powerkeeper;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class PreventBatteryWitelist extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.miui.powerkeeper.utils.CommonAdapter", lpparam.classLoader, "addPowerSaveWhitelistApps", new MethodHook(20000) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
