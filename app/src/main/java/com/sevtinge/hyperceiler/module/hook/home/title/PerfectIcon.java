package com.sevtinge.hyperceiler.module.hook.home.title;

import android.content.pm.LauncherActivityInfo;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class PerfectIcon extends BaseHook {

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.library.compat.LauncherActivityInfoCompat", "getIconResource", LauncherActivityInfo.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(0);
            }
        });
    }
}
