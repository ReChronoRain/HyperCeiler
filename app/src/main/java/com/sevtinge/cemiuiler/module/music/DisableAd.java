package com.sevtinge.cemiuiler.module.music;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DisableAd extends BaseHook {

    Class<?> mCloud;

    @Override
    //by @Yife Playte
    public void init() {
        try {
            findAndHookMethod("com.tencent.qqmusiclite.activity.SplashAdActivity", "onCreate", android.os.Bundle.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    try {
                        Class<?> clazz = findClassIfExists("android.app.Activity");
                        clazz.getMethod("finish").invoke(param.thisObject);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}


