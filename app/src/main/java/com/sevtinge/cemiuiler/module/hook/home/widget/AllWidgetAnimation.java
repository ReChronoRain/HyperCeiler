package com.sevtinge.cemiuiler.module.hook.home.widget;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class AllWidgetAnimation extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.LauncherWidgetView", "isUseTransitionAnimation", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.miui.home.launcher.maml.MaMlWidgetView", "isUseTransitionAnimation", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
