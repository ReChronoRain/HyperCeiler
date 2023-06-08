package com.sevtinge.cemiuiler.module;

import android.os.Build;
import android.util.Log;

import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForR;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForS;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForSv2;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForT;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemFrameworkForCorePatch implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final String TAG = "Cemiuiler: CorePatch";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (("android".equals(lpparam.packageName)) && (lpparam.processName.equals("android"))) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.TIRAMISU -> // 33
                    new CorePatchForT().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.S_V2 -> // 32
                    new CorePatchForSv2().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.S -> // 31
                    new CorePatchForS().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.R -> // 30
                    new CorePatchForR().handleLoadPackage(lpparam);
                default -> XposedBridge.log(TAG + ": Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (startupParam.startsSystemServer) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.TIRAMISU -> // 33
                    new CorePatchForT().initZygote(startupParam);
                case Build.VERSION_CODES.S_V2 -> // 32
                    new CorePatchForSv2().initZygote(startupParam);
                case Build.VERSION_CODES.S -> // 31
                    new CorePatchForS().initZygote(startupParam);
                case Build.VERSION_CODES.R -> // 30
                    new CorePatchForR().initZygote(startupParam);
                default -> XposedBridge.log(TAG + ": Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
            }
        }
    }
}
