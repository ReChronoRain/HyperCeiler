package com.sevtinge.hyperceiler.module.base;

import com.sevtinge.hyperceiler.utils.DexKit;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HostDir {
}

class LoadHostDir {
    public LoadHostDir(LoadPackageParam lpparam, String tag) throws Exception {
        if (lpparam != null) {
            DexKit.INSTANCE.initDexKit(lpparam);
        } else {
            throw new Exception(tag + ": lpparam is null");
        }
    }
}

class CloseHostDir {
    public CloseHostDir(LoadPackageParam lpparam, String tag) throws Exception {
        if (lpparam != null) {
            DexKit.INSTANCE.closeDexKit();
        } else {
            throw new Exception(tag + ": lpparam is null");
        }
    }
}
