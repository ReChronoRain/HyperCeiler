package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;

public class AppsFreezerEnable extends BaseHook {
    @Override
    public void init() {
        Helpers.findAndHookMethod("com.android.settings.development.CachedAppsFreezerPreferenceController",
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
