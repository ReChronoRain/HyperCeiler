package com.sevtinge.hyperceiler.module.hook.home.title;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockBlurSupported extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.common.BlurUtilities",
            "isBlurSupported",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(true);
                }
            }
        );
    }
}
