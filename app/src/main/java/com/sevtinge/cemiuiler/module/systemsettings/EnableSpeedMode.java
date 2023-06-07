package com.sevtinge.cemiuiler.module.systemsettings;

import com.sevtinge.cemiuiler.module.base.BaseHook;

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

