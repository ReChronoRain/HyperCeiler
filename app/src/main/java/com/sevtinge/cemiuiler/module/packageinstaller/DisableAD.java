package com.sevtinge.cemiuiler.module.packageinstaller;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class DisableAD extends BaseHook {

    @Override
    public void init() {

        findAndHookMethod("m2.b",
                "q",
                XC_MethodReplacement.returnConstant(false));

        findAndHookMethod("m2.b",
                "r",
                XC_MethodReplacement.returnConstant(false));

        findAndHookMethod("m2.b",
                "t",
                XC_MethodReplacement.returnConstant(false));
    }
}
