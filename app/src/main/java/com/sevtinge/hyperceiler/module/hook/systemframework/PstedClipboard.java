package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.util.ArraySet;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class PstedClipboard extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.server.clipboard.ClipboardService",
                "lambda$showAccessNotificationLocked$4",
                String.class, int.class, ArraySet.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
        );
    }
}
