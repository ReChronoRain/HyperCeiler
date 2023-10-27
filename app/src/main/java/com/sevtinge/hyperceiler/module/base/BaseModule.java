package com.sevtinge.hyperceiler.module.base;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.PrefsMap;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;

    public void init(LoadPackageParam lpparam) {
        mLoadPackageParam = lpparam;
        initZygote();
        handleLoadPackage();
    }

    @Override
    public void initZygote() {
    }

    public void initHook(BaseHook baseHook) {
        initHook(baseHook, true);
    }

    public void initHook(BaseHook baseHook, boolean isInit) {
        if (isInit) {
            baseHook.onCreate(mLoadPackageParam);
        }
    }
}
