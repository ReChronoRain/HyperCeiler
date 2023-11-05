package com.sevtinge.hyperceiler.module.hook.systemui;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class ShouldPlayUnmuteSoundHook extends BaseHook {

    Class<?> mQuietModeTile = XposedHelpers.findClassIfExists("com.android.systemui.qs.tiles.QuietModeTile", lpparam.classLoader);
    Class<?> mZenModeController = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.policy.ZenModeController", lpparam.classLoader);

    @Override
    public void init() {
        findAndHookMethod(mQuietModeTile, "handleClick", View.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(mZenModeController, "isZenModeOn", true);
            }
        });
    }
}
