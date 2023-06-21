package com.sevtinge.cemiuiler.module.home;

import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UserPresentAnimation extends BaseHook {

    Class<?> mUserPresentAnimationCompatV12Phone;

    @Override
    public void init() {
        mUserPresentAnimationCompatV12Phone = findClassIfExists("com.miui.home.launcher.compat.UserPresentAnimationCompatV12Phone");

        findAndHookMethod(mUserPresentAnimationCompatV12Phone, "getSpringAnimator", View.class, int.class, float.class, float.class, float.class, float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.args[4] = 0.5f;
                param.args[5] = 0.5f;
            }
        });
    }
}
