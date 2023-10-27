package com.sevtinge.hyperceiler.module.hook.home.layout;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DisplayUtils;

public class HotSeatsMarginBottom extends BaseHook {

    Class<?> mDeviceConfig;

    @Override
    public void init() {

        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");

        findAndHookMethod(mDeviceConfig, "calcHotSeatsMarginBottom", Context.class, boolean.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                param.setResult(DisplayUtils.dip2px(context, mPrefsMap.getInt("home_layout_hotseats_margin_bottom", 60)));
            }
        });
    }
}
