package com.sevtinge.cemiuiler.module.clock;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.io.File;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class EnableHourGlass extends BaseHook {
    @Override
    public void init() {
        int appVersionCode = getPackageVersionCode(lpparam);
        if (appVersionCode <= 130206400) {
            hookAllMethods("com.android.deskclock.util.Util", "isHourGlassEnable", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else {
            XposedBridge.log("Cemiuiler: Your clock versionCode is " + appVersionCode);
            XposedBridge.log("Cemiuiler: Please revert to a supported version yourself");
        }
    }

    private static int getPackageVersionCode(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            int versionCode = XposedHelpers.getIntField(pkg, "mVersionCode");
            XposedBridge.log("Cemiuiler: " + lpparam + " versionCode is " + versionCode);
            return versionCode;
        } catch (Throwable e) {
            XposedBridge.log("Cemiuiler: Unknown Version.");
            XposedBridge.log(e);
            return -1;
        }
    }
}
