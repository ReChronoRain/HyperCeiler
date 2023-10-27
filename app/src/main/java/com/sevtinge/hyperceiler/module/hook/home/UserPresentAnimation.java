package com.sevtinge.hyperceiler.module.hook.home;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UserPresentAnimation extends BaseHook {

    Class<?> mUserPresentAnimationCompatV12Phone;

    @Override
    public void init() {
        mUserPresentAnimationCompatV12Phone = !isPad() ?
            findClassIfExists("com.miui.home.launcher.compat.UserPresentAnimationCompatV12Phone") :
        findClassIfExists("com.miui.home.launcher.compat.UserPresentAnimationCompatV12Spring");
        findAndHookMethod(mUserPresentAnimationCompatV12Phone, "getSpringAnimator", View.class, int.class, float.class, float.class, float.class, float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.args[4] = 0.5f;
                param.args[5] = 0.5f;
            }
        });
    }
}
