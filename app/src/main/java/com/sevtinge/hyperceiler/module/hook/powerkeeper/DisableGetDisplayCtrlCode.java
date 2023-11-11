package com.sevtinge.hyperceiler.module.hook.powerkeeper;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableGetDisplayCtrlCode extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.powerkeeper.feedbackcontrol.ThermalManager", "getDisplayCtrlCode", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(0);
                }
            }
        );
    }
}
