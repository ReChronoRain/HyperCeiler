package com.sevtinge.cemiuiler.module.hook.clock;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class EnableHourGlass extends BaseHook {
    @Override
    public void init() {
        int appVersionCode = getPackageVersionCode(lpparam);
        hookAllMethods("com.android.deskclock.util.Util", "isHourGlassEnable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                if (appVersionCode <= 130206400) {
                    param.setResult(true);
                } else {
                    log("Your clock versionCode is " + appVersionCode);
                    log("Please revert to a supported version yourself");
                }
            }
        });
    }
}
