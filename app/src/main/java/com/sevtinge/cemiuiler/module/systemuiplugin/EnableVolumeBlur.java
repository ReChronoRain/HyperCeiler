package com.sevtinge.cemiuiler.module.systemuiplugin;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class EnableVolumeBlur extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.miui.volume.Util", "isSupportBlurS", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
