package com.sevtinge.cemiuiler.module.base;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public interface IXposedHook {

    void initZygote();

    void handleLoadPackage();
}
