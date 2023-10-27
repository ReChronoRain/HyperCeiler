package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class EnableSpeedMode extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.development.SpeedModeToolsPreferenceController", "getAvailabilityStatus", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(0);
            }
        });

    }
}

