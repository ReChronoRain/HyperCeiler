package com.sevtinge.cemiuiler.module.misettings;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class CustomRefreshRate extends BaseHook {

    Class<?> mNewRefreshRateFragment;

    @Override
    public void init() {
        mNewRefreshRateFragment = findClassIfExists("com.xiaomi.misettings.display.RefreshRate.NewRefreshRateFragment");

        findAndHookMethod(mNewRefreshRateFragment, "b", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });
    }
}
