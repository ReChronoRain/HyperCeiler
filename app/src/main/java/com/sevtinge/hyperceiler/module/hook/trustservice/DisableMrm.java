package com.sevtinge.hyperceiler.module.hook.trustservice;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableMrm extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.xiaomi.trustservice.remoteservice.eventhandle.statusEventHandle", "initIMrmService" ,new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
