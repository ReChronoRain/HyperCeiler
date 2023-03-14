package com.sevtinge.cemiuiler.module.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UseOriginalAnimation extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.server.wm.AppTransitionInjector", "createActivityOpenCloseTransition", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
