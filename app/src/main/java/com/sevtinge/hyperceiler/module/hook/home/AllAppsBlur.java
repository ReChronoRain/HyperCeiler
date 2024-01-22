package com.sevtinge.hyperceiler.module.hook.home;

import android.app.Activity;

import com.sevtinge.hyperceiler.module.base.BaseHook;

// what is it?
public class AllAppsBlur extends BaseHook {

    Class<?> mLauncher;
    Class<?> mBlurUtils;
    Class<?> mAllAppsTransitionController;
    Class<?> mActivityCls;
    Activity mActivity;

    @Override
    public void init() {
        mActivityCls = Activity.class;
        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mBlurUtils = findClassIfExists("com.miui.home.launcher.common.BlurUtils");
        mAllAppsTransitionController = findClassIfExists("com.miui.home.launcher.allapps.BaseAllAppsContainerView");

    }
}
