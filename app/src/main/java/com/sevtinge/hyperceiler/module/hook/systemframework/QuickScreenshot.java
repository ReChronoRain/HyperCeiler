package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class QuickScreenshot extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.server.policy.PhoneWindowManager", "getScreenshotChordLongPressDelay", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(0L);
            }
        });
    }
}
