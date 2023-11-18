package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class HideDelimiter extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController",
            "fireCarrierTextChanged", String.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    String mCurrentCarrier = (String) param.args[0];
                    param.args[0] = mCurrentCarrier.replace(" | ", "");
                }
            }
        );
    }
}
