package com.sevtinge.hyperceiler.module.hook.home;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class FreeFormCountForHome extends BaseHook {

    Class<?> mRecentsAndFSGesture;

    @Override
    public void init() {
        mRecentsAndFSGesture = findClassIfExists("com.miui.home.launcher.RecentsAndFSGestureUtils");

        hookAllMethods(mRecentsAndFSGesture,
            "canTaskEnterMiniSmallWindow",
            MethodHook.returnConstant(true));

        hookAllMethods(mRecentsAndFSGesture,
            "canTaskEnterSmallWindow",
            MethodHook.returnConstant(true));
    }
}
