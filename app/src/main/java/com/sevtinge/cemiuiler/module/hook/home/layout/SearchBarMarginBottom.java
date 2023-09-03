package com.sevtinge.cemiuiler.module.hook.home.layout;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.DisplayUtils;

public class SearchBarMarginBottom extends BaseHook {

    Class<?> mDeviceConfig;

    @Override
    public void init() {

        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");

        findAndHookMethod(mDeviceConfig, "calcSearchBarMarginBottom", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                param.setResult(DisplayUtils.dip2px(context, mPrefsMap.getInt("home_layout_searchbar_margin_bottom", 0)));
            }
        });
    }
}
