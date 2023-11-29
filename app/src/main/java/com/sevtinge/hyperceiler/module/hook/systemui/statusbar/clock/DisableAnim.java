package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableAnim extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.statusbar.policy.FakeStatusBarClockController", "onPanelStretchChanged", float.class, boolean.class, new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
