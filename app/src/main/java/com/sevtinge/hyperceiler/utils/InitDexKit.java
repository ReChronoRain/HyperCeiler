package com.sevtinge.hyperceiler.utils;

import com.sevtinge.hyperceiler.module.base.BaseModule;

import org.luckypray.dexkit.DexKitBridge;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class InitDexKit implements BaseModule.ILoadDexKit {
    public static XC_LoadPackage.LoadPackageParam mLpparam;
    public static IUseDexKit iUseDexKit;
    public static String mTAG;
    private static String hostDir = null;

    public InitDexKit() {
        BaseModule.setLoadDexKit(this);
    }

    public interface IUseDexKit {
        void useDexKit(boolean use);
    }

    public static void setUseDexKit(IUseDexKit useDexKit) {
        iUseDexKit = useDexKit;
    }

    public static DexKitBridge init() throws Exception {
        if (hostDir == null) {
            if (mLpparam == null) {
                throw new Exception(mTAG != null ? mTAG : "InitDexKit" + ": lpparam is null");
            }
            hostDir = mLpparam.appInfo.sourceDir;
        }
        System.loadLibrary("dexkit");
        // XposedLogUtils.logE(mTAG, "dexkit: " + hostDir);
        DexKitBridge bridge = DexKitBridge.create(hostDir);
        DexKit.INSTANCE.setInitialized(true);
        setHostDir(hostDir);
        iUseDexKit.useDexKit(true);
        return bridge;
    }

    public static void setHostDir(String dir) {
        DexKit.INSTANCE.setHostDir(dir);
    }

    public void closeHostDir() throws Exception {
        if (mLpparam != null) {
            DexKit.INSTANCE.closeDexKit();
        } else {
            throw new Exception(mTAG + ": lpparam is null");
        }
    }

    @Override
    public void createDexKit(XC_LoadPackage.LoadPackageParam lpparam, String TAG) {
        mLpparam = lpparam;
        mTAG = TAG;
    }
}
