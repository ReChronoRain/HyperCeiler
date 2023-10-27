package com.sevtinge.hyperceiler.module.hook.thememanager;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class VersionCodeModify extends BaseHook {

    Class<?> mThemeApplication;

    @Override
    public void init() {

        mThemeApplication = findClassIfExists("com.android.thememanager.ThemeApplication");

        findAndHookMethod(mThemeApplication, "onCreate", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {

                findAndHookMethod("android.os.SystemProperties", "get", String.class, String.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if ("ro.miui.ui.version.code".equals(param.args[0])) {
                            param.setResult("14");
                        }
                    }
                });
            }
        });
    }
}
