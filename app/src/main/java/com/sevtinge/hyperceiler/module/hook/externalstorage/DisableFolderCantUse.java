package com.sevtinge.hyperceiler.module.hook.externalstorage;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableFolderCantUse extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.externalstorage.ExternalStorageProvider", "shouldBlockFromTree", String.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(false);
            }
        });
    }
}
