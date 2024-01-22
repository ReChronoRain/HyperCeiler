package com.sevtinge.hyperceiler.module.base;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.callback.TAG;
import com.sevtinge.hyperceiler.utils.InitDexKit;
import com.sevtinge.hyperceiler.utils.PrefsMap;
import com.sevtinge.hyperceiler.utils.PropUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseModule implements IXposedHook {

    public LoadPackageParam mLoadPackageParam = null;
    public String TAG = getClass().getSimpleName();
    public static ILoadDexKit loadDexKit;
    public final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;
    private final String path = "/sdcard/Android/hy_crash/";
    private int count = 0;

    public interface ILoadDexKit {
        void createDexKit(LoadPackageParam lpparam, String TAG);
    }

    public static void setLoadDexKit(ILoadDexKit iLoadDexKit) {
        loadDexKit = iLoadDexKit;
    }

    public void init(LoadPackageParam lpparam) {
        if (needIntercept(lpparam.packageName)) {
            AndroidLogUtils.LogI(TAG.TAG, "进入安全模式: " + lpparam.packageName);
            return;
        }
        mLoadPackageParam = lpparam;
        initZygote();
        DexKitHelper helper = new DexKitHelper();
        InitDexKit kit = new InitDexKit();
        loadDexKit.createDexKit(mLoadPackageParam, TAG);
        handleLoadPackage();
        if (helper.useDexKit) {
            try {
                kit.closeHostDir();
                // XposedLogUtils.logE(TAG, "close dexkit s: " + lpparam.packageName);
            } catch (Exception e) {
                XposedLogUtils.logE(TAG, "close dexkit e: " + e);
            }
        }
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

    private static class DexKitHelper implements InitDexKit.IUseDexKit {
        public boolean useDexKit = false;

        public DexKitHelper() {
            InitDexKit.setUseDexKit(this);
        }

        @Override
        public void useDexKit(boolean use) {
            useDexKit = use;
        }
    }
}
