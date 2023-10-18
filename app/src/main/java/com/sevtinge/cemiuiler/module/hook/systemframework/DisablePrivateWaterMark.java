package com.sevtinge.cemiuiler.module.hook.systemframework;

import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DisablePrivateWaterMark extends BaseHook {
    @Override
    public void init() {
        Class<?> clazz = findClass("com.miui.internal.cust.PrivateWaterMarkerConfig", lpparam.classLoader);
        setStaticBooleanField(clazz, "IS_PRIVATE_WATER_MARKER", false);
    }
}
