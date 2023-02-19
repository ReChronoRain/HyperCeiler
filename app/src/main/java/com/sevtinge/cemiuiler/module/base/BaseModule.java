package com.sevtinge.cemiuiler.module.base;

import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.utils.PrefsMap;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;

    public void init(LoadPackageParam lpparam) {
        mLoadPackageParam = lpparam;
        initZygote();
        handleLoadPackage();
    }

    @Override
    public void initZygote() {}

    public void initHook(BaseHook baseHook) {
        initHook(baseHook, true);
    }

    public void initHook(BaseHook baseHook, boolean isInit) {
        if (isInit) {
            baseHook.onCreate(mLoadPackageParam);
        }
    }
}
