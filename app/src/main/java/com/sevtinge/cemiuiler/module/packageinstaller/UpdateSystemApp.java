package com.sevtinge.cemiuiler.module.packageinstaller;

import android.content.pm.ApplicationInfo;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class UpdateSystemApp extends BaseHook {

    @Override
    public void init() {
        findAndHookMethod("android.os.SystemProperties", "getBoolean", String.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if ("persist.sys.allow_sys_app_update".equals(param.args[0])) {
                    param.setResult(true);
                }
            }
        });

        /*findAndHookMethod("j2.e",
                "q",
                ApplicationInfo.class,
                XC_MethodReplacement.returnConstant(false));*/

        hookAllMethods("j2.f",
                "q",
                XC_MethodReplacement.returnConstant(false));
    }
}
