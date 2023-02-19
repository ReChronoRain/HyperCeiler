package com.sevtinge.cemiuiler.module.mishare;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DisableMishareAutoOff extends BaseHook {

    @Override
    public void init() {
        hookAllMethods("com.miui.mishare.connectivity.MiShareService$d$g", "b", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                return;
            }
        });
    }
}


