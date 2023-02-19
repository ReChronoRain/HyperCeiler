package com.sevtinge.cemiuiler.module.home;

import android.content.Context;
import com.sevtinge.cemiuiler.module.base.BaseHook;

public class WidgetCrack extends BaseHook {


    @Override
    public void init() {


        findAndHookMethod("com.miui.maml.widget.edit.MamlutilKt", "themeManagerSupportPaidWidget", Context.class, new MethodHook() {
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

