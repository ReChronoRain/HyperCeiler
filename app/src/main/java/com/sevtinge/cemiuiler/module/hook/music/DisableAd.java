package com.sevtinge.cemiuiler.module.hook.music;

import static com.sevtinge.cemiuiler.utils.log.AndroidLogUtils.LogD;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DisableAd extends BaseHook {

    Class<?> mCloud;

    @Override
    // by @Yife Playte
    public void init() {
        try {
            findAndHookMethod("com.tencent.qqmusiclite.activity.SplashAdActivity", "onCreate", android.os.Bundle.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    try {
                        Class<?> clazz = findClassIfExists("android.app.Activity");
                        clazz.getMethod("finish").invoke(param.thisObject);
                    } catch (Throwable e) {
                        LogD(TAG, e);
                    }
                }
            });
        } catch (Throwable e) {
            LogD(TAG, e);
        }
    }
}


