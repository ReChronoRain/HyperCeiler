package com.sevtinge.hyperceiler.module.hook.networkboost;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class LinkTurboToast extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.xiaomi.NetworkBoost.slaservice.SLAToast",
            "setLinkTurboStatus", boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.args[0] = false;
                }
            }
        );
    }
}
