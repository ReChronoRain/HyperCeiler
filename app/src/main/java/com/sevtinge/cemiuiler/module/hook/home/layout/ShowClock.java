package com.sevtinge.cemiuiler.module.hook.home.layout;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class ShowClock extends BaseHook {

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.Workspace", "isScreenHasClockGadget", long.class, XC_MethodReplacement.returnConstant(false));
    }
}
