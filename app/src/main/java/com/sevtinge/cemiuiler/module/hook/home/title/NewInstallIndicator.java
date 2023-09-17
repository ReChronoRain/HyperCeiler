package com.sevtinge.cemiuiler.module.hook.home.title;

import com.sevtinge.cemiuiler.module.base.BaseHook;

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
