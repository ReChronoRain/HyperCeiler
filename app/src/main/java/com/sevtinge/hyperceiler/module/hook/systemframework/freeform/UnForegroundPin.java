package com.sevtinge.hyperceiler.module.hook.systemframework.freeform;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnForegroundPin extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
            "needForegroundPin",
            "com.android.server.wm.MiuiFreeFormActivityStack",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(true);
                }
            }
        );
    }
}
