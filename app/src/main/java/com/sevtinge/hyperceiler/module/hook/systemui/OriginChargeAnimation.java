package com.sevtinge.hyperceiler.module.hook.systemui;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;

public class OriginChargeAnimation extends BaseHook {

    Class<?> mOriginChargeAnimCls;

    @Override
    public void init() {

        mOriginChargeAnimCls = findClassIfExists("com.android.systemui.statusbar.FeatureFlags");

        findAndHookMethod(mOriginChargeAnimCls, "isChargingRippleEnabled", new Helpers.MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
