package com.sevtinge.hyperceiler.module.hook.systemui;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class MiuiGxzwSize extends BaseHook {

    @Override
    public void init() {

        Class<?> mMiuiGxzwUtils = findClassIfExists("com.android.keyguard.fod.MiuiGxzwUtils");

        /*hookAllMethods(mMiuiGxzwUtils,"caculateGxzwIconSize", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticIntField(mMiuiGxzwUtils,"GXZW_ANIM_HEIGHT", 1028);
                XposedHelpers.setStaticIntField(mMiuiGxzwUtils,"GXZW_ANIM_WIDTH", 1028);
            }
        });*/
    }
}
