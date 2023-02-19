package com.sevtinge.cemiuiler.module.home;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class ShowAllHideApp extends BaseHook {

    Class<?> mAllHideAppActivity;

    @Override
    public void init() {
        mAllHideAppActivity = findClassIfExists("com.miui.home.settings.AllHideAppActivity");

        hookAllMethods(mAllHideAppActivity, "isHideAppValid", XC_MethodReplacement.returnConstant(true));
    }
}
