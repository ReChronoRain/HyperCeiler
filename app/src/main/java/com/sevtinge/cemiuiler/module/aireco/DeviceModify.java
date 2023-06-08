package com.sevtinge.cemiuiler.module.aireco;

import android.os.Build;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class DeviceModify extends BaseHook {

    @Override
    public void init() {
        hookAllMethods("com.xiaomi.aireco.utils.DeviceUtils", "getVoiceAssistUserAgent", new MethodHook() {
            // findAndHookConstructor("com.xiaomi.market.MarketApp", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticObjectField(Build.class, "DEVICE", "nuwa");
                XposedHelpers.setStaticObjectField(Build.class, "MODEL", "2210132C");
                XposedHelpers.setStaticObjectField(Build.class, "MANUFACTURER", "Xiaomi");
            }
        });
    }
}
