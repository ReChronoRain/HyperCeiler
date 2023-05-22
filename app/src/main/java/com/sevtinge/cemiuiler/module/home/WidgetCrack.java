package com.sevtinge.cemiuiler.module.home;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class WidgetCrack extends BaseHook {


    @Override
    public void init() {


        hookAllMethods("com.miui.maml.widget.edit.MamlutilKt", "themeManagerSupportPaidWidget", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        findAndHookMethod("com.miui.home.launcher.gadget.MaMlPendingHostView", "isCanAutoStartDownload", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}

