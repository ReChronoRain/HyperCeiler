package com.sevtinge.cemiuiler.module.hook.systemui;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

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
