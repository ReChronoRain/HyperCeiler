package com.sevtinge.hyperceiler.module.hook.home.title;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class NewInstallIndicator extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.TitleTextView",
            "updateNewInstallIndicator",
            boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(null);
                }
            }
        );
    }
}
