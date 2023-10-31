package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AppsFreezerEnable extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.development.CachedAppsFreezerPreferenceController",
            lpparam.classLoader,
            "isAvailable",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    super.after(param);
                    param.setResult(true);
                }
            }
        );
    }
}
