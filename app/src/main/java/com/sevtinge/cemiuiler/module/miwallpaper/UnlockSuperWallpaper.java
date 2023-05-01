package com.sevtinge.cemiuiler.module.miwallpaper;

import android.content.Context;
import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockSuperWallpaper extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.superwallpaper.SuperWallpaperUtils", "initEnableSuperWallpaper", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
