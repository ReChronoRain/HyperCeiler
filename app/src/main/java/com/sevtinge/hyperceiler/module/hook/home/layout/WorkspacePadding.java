package com.sevtinge.hyperceiler.module.hook.home.layout;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DisplayUtils;

public class WorkspacePadding extends BaseHook {

    Context mContext;
    Class<?> mDeviceConfig;

    @Override
    public void init() {

        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");

        findAndHookMethod(mDeviceConfig, "Init", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                mContext = (Context) param.args[0];
            }
        });

        if (mPrefsMap.getBoolean("home_layout_workspace_padding_bottom_enable")) {
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingBottom", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_layout_workspace_padding_bottom", 0)));
                }
            });
        }

        if (mPrefsMap.getBoolean("home_layout_workspace_padding_top_enable")) {
            try {
                // 新版本桌面，先标记，后续再做进一步修改
                findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingTop", Context.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_layout_workspace_padding_top", 0)));
                    }
                });
            } catch (Throwable t) {
                findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingTop", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_layout_workspace_padding_top", 0)));
                    }
                });
            }
        }

        if (mPrefsMap.getBoolean("home_layout_workspace_padding_horizontal_enable")) {
            logE("===============home_layout_workspace_padding_horizontal: " + mPrefsMap.getInt("home_layout_workspace_padding_horizontal", 0));
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingSide", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_layout_workspace_padding_horizontal", 0)));
                }
            });
        }
    }
}
