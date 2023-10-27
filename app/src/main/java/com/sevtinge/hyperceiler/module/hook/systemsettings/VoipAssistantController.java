package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class VoipAssistantController extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.lab.MiuiVoipAssistantController", "isNotSupported", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

    }
}

