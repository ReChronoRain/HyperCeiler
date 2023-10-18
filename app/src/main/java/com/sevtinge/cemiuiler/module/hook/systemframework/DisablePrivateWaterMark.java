package com.sevtinge.cemiuiler.module.hook.systemframework;

import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DisablePrivateWaterMark extends BaseHook {
    @Override
    public void init() {
        Class<?> clazz = findClass("com.miui.internal.cust.PrivateWaterMarkConfig", lpparam.classLoader);
        setStaticBooleanField(clazz, "IS_PRIVATE_WATER_MARK", false);
    }
}
