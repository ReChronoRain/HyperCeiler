package com.sevtinge.hyperceiler.module.hook.home.title;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.List;

public class HideReportText extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.uninstall.BaseUninstallDialog", "init", Context.class, List.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod("com.miui.home.launcher.ShortcutInfo", "getInstallerPackageName", new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult("com.xiaomi.market");
                    }
                });
            }
        });
    }
}
