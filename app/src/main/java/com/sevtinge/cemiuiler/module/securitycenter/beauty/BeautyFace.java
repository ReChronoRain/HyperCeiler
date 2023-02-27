package com.sevtinge.cemiuiler.module.securitycenter.beauty;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;

public class BeautyFace extends BaseHook {
        @Override
        public void init() {
            String appVersionName = getPackageVersion(lpparam);
            if (appVersionName.startsWith("7.4.9")) {
                findAndHookMethod("p5.f", "D", new BaseHook.MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }else{
                findAndHookMethod("com.miui.gamebooster.beauty.i", "z", new BaseHook.MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
        }

    private static String getPackageVersion(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            String versionName = (String) XposedHelpers.getObjectField(pkg, "mVersionName");
            int versionCode = XposedHelpers.getIntField(pkg, "mVersionCode");
            XposedBridge.log("Cemiuiler: " + String.format("%s (%d", versionName, versionCode));
            return String.format("%s (%d", versionName, versionCode);
        } catch (Throwable e) {
            XposedBridge.log("Cemiuiler: Unknown Version.");
            XposedBridge.log(e);
            return "null";
        }
    }
    }
