package com.sevtinge.hyperceiler.module.base;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.callback.TAG;
import com.sevtinge.hyperceiler.utils.PrefsMap;
import com.sevtinge.hyperceiler.utils.PropUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;
    private final String path = "/sdcard/Android/hy_crash/";
    private int count = 0;

    public void init(LoadPackageParam lpparam) {
        if (needIntercept(lpparam.packageName)) {
            AndroidLogUtils.LogI(TAG.TAG, "进入安全模式: " + lpparam.packageName);
            return;
        }
        mLoadPackageParam = lpparam;
        initZygote();
        handleLoadPackage();
    }

    private boolean needIntercept(String pkg) {
        String data = PropUtils.getProp("persist.hyperceiler.crash.report", "[]");
        if (data.equals("[]") || data.equals("")) {
            return false;
        }
        return data.contains(pkg);
        // ArrayList<JSONObject> jsonObjects = CrashHook.CrashData.toArray(data);
        // for (JSONObject j : jsonObjects) {
        //     String mPkg = CrashHook.CrashData.getPkg(j);
        //     if (mPkg.equals(pkg)) {
        //         return CrashHook.CrashData.getCount(j);
        //     }
        // }
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
