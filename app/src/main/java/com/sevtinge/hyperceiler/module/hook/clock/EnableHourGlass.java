package com.sevtinge.hyperceiler.module.hook.clock;

import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionCode;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

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
                    XposedLogUtils.logI("Your clock versionCode is " + appVersionCode);
                    XposedLogUtils.logI("Please revert to a supported version yourself");
                }
            }
        });
    }
}
