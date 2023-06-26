package com.sevtinge.cemiuiler.module.powerkeeper;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class CustomRefreshRate extends BaseHook {

    Class<?> mDisplayFrameSetting;

    @Override
    public void init() {
        mDisplayFrameSetting = findClassIfExists("com.miui.powerkeeper.statemachine.DisplayFrameSetting");

        findAndHookMethod(mDisplayFrameSetting, "parseCustomModeSwitchFromDb", String.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "mIsCustomFpsSwitch", "true");
                XposedHelpers.setObjectField(param.thisObject, "fucSwitch", "true");
            }
        });
    }
}
