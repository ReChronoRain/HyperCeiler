package com.sevtinge.hyperceiler.module.base;

import static com.sevtinge.hyperceiler.utils.log.LogManager.logLevel;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.ResourcesHook;
import com.sevtinge.hyperceiler.utils.hook.HookUtils;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseHook extends HookUtils {
    public String TAG = getClass().getSimpleName();

    public static final ResourcesHook mResHook = XposedInit.mResHook;
    public static final String ACTION_PREFIX = "com.sevtinge.hyperceiler.module.action.";

    public abstract void init() throws NoSuchMethodException;

    public void onCreate(LoadPackageParam lpparam) {
        try {
            setLoadPackageParam(lpparam);
            init();
            if (logLevel >= 3) {
                logI(TAG, lpparam.packageName, "Hook Success.");
            }
        } catch (Throwable t) {
            if (logLevel >= 1) logE(TAG, lpparam.packageName, "Hook Failed", t);
        }
    }

    @Override
    public void setLoadPackageParam(LoadPackageParam param) {
        lpparam = param;
    }
}
