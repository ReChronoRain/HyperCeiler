package com.sevtinge.cemiuiler.module.hook.home;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class FreeFormCountForHome extends BaseHook {

    Class<?> mRecentsAndFSGesture;

    @Override
    public void init() {
        mRecentsAndFSGesture = findClassIfExists("com.miui.home.launcher.RecentsAndFSGestureUtils");

        hookAllMethods(mRecentsAndFSGesture,
            "canTaskEnterMiniSmallWindow",
            XC_MethodReplacement.returnConstant(true));

        hookAllMethods(mRecentsAndFSGesture,
            "canTaskEnterSmallWindow",
            XC_MethodReplacement.returnConstant(true));
    }
}
