package com.sevtinge.hyperceiler.module.base;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.PrefsMap;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public String TAG = getClass().getSimpleName();
    public final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;

    public void init(LoadPackageParam lpparam) {
        boolean loadDexKit = false;
        mLoadPackageParam = lpparam;
        initZygote();
        try {
            new LoadHostDir(lpparam, TAG);
            loadDexKit = true;
        } catch (Exception e) {
            XposedLogUtils.logE(TAG, "load dexkit e: " + e);
        }
        handleLoadPackage();
        try {
            if (loadDexKit) new CloseHostDir(lpparam, TAG);
        } catch (Exception f) {
            XposedLogUtils.logE(TAG, "close dexkit e: " + f);
        }
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
