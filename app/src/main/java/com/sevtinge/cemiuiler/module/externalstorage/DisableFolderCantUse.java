package com.sevtinge.cemiuiler.module.externalstorage;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DisableFolderCantUse extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.externalstorage.ExternalStorageProvider", "shouldBlockFromTree", String.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}