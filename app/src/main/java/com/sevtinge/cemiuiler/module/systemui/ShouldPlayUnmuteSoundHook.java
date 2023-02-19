package com.sevtinge.cemiuiler.module.systemui;

import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;

public class ShouldPlayUnmuteSoundHook extends BaseHook {

    Class<?> mQuietModeTile = XposedHelpers.findClassIfExists("com.android.systemui.qs.tiles.QuietModeTile", lpparam.classLoader);
    Class<?> mZenModeController = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.policy.ZenModeController", lpparam.classLoader);

    @Override
    public void init() {
        Helpers.findAndHookMethod(mQuietModeTile, "handleClick", View.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(mZenModeController, "isZenModeOn", true);
            }
        });
    }
}
