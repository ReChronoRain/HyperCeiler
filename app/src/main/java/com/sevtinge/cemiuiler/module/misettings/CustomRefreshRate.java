package com.sevtinge.cemiuiler.module.misettings;

import android.os.Bundle;
import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class CustomRefreshRate extends BaseHook {

    Class<?> mNewRefreshRateFragment;
    Class<?> mRefreshRateActivity;

    @Override
    public void init() {
        mNewRefreshRateFragment = findClassIfExists("com.xiaomi.misettings.display.RefreshRate.NewRefreshRateFragment");
        mRefreshRateActivity = findClassIfExists("com.xiaomi.misettings.display.RefreshRate.RefreshRateActivity");

        findAndHookMethod(mNewRefreshRateFragment, "b", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });

        findAndHookMethod(mRefreshRateActivity, "onCreate", Bundle.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "a", true);
            }
        });
    }
}
