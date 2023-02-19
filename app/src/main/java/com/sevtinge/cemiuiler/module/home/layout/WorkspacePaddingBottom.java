package com.sevtinge.cemiuiler.module.home.layout;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class WorkspacePaddingBottom extends BaseHook {

    Context mContext;
    Class<?> mDeviceConfig;

    @Override
    public void init() {

        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");

        findAndHookMethod(mDeviceConfig, "Init", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mContext = (Context) param.args[0];
            }
        });

        findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingBottom", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_layout_workspace_padding_bottom", 0)));
            }
        });

    }
}
