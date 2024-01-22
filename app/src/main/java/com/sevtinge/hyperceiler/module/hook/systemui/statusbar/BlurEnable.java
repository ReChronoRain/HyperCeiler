package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class BlurEnable extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.statusbar.BlurUtils",
            "supportsBlursOnWindows",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(true);
                }
            }
        );
    }
}
