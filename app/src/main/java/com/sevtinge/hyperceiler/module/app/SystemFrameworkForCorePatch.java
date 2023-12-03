package com.sevtinge.hyperceiler.module.app;

import android.os.Build;

import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForR;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForS;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForT;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.CorePatchForU;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemFrameworkForCorePatch implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final String TAG = "][HyperCeiler][CorePatch]";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (("android".equals(lpparam.packageName)) && (lpparam.processName.equals("android"))) {
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> // 34
                    new CorePatchForU().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.TIRAMISU -> // 33
                    new CorePatchForT().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.S_V2 -> // 32
                    new CorePatchForS().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.S -> // 31
                    new CorePatchForS().handleLoadPackage(lpparam);
                case Build.VERSION_CODES.R -> // 30
                    new CorePatchForR().handleLoadPackage(lpparam);
                default -> XposedLogUtils.logW("CorePatch", "android","Unsupported Version of Android " + Build.VERSION.SDK_INT);
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        if (startupParam.startsSystemServer) {
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> // 34
                    new CorePatchForU().initZygote(startupParam);
                case Build.VERSION_CODES.TIRAMISU -> // 33
                    new CorePatchForT().initZygote(startupParam);
                case Build.VERSION_CODES.S_V2 -> // 32
                    new CorePatchForS().initZygote(startupParam);
                case Build.VERSION_CODES.S -> // 31
                    new CorePatchForS().initZygote(startupParam);
                case Build.VERSION_CODES.R -> // 30
                    new CorePatchForR().initZygote(startupParam);
                default -> XposedLogUtils.logW("CorePatch", "android","Unsupported Version of Android " + Build.VERSION.SDK_INT);
            }
        }
    }
}
