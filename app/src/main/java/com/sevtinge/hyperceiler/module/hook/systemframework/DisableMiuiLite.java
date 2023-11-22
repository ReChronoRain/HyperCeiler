package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableMiuiLite extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("miui.os.Build", "isMiuiLiteVersion", new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
