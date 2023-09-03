package com.sevtinge.cemiuiler.module.hook.home;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.DisplayUtils;

public class WidgetCornerRadius extends BaseHook {

    Context mContext;

    @Override
    public void init() {

        hookAllConstructors("com.miui.home.launcher.maml.MaMlHostView", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mContext = (Context) param.args[0];
            }
        });

        hookAllMethods("com.miui.home.launcher.maml.MaMlHostView", "computeRoundedCornerRadius", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult((float) DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_widget_corner_radius", 0)));
            }
        });


        hookAllConstructors("com.miui.home.launcher.LauncherAppWidgetHostView", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mContext = (Context) param.args[0];
            }
        });

        hookAllMethods("com.miui.home.launcher.LauncherAppWidgetHostView", "computeRoundedCornerRadius", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult((float) DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_widget_corner_radius", 0)));
            }
        });
    }
}
