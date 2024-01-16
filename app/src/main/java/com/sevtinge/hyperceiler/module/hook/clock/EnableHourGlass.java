package com.sevtinge.hyperceiler.module.hook.clock;

import com.sevtinge.hyperceiler.module.base.BaseHook;


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
                    logI(TAG, EnableHourGlass.this.lpparam.packageName, "Your clock versionCode is " + appVersionCode);
                    logI(TAG, EnableHourGlass.this.lpparam.packageName, "Please revert to a supported version yourself");
                }
            }
        });
    }
}
