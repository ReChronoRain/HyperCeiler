package com.sevtinge.hyperceiler.module.hook.phone;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableRemoveNetworkMode extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        hookAllMethods("com.android.phone.NetworkModeManager", "isRemoveNetworkModeSettings", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
