package com.sevtinge.cemiuiler.module.securitycenter.lab;

import android.content.ComponentName;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

import java.io.File;
import java.util.Map;

public class AiClipboardEnable extends BaseHook {

    Class<?> mLab;
    Class<?> mStableVer;
    Class<?> utilCls;


    @Override
    public void init() {
        /*mLab = findClassIfExists("com.miui.permcenter.q");
        mStableVer = findClassIfExists("miui.os.Build");

        findAndHookMethod(mLab, "e", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });*/

        Helpers.findAndHookMethod("com.miui.permcenter.settings.PrivacyLabActivity", lpparam.classLoader, "onCreateFragment", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String appVersionName = getPackageVersion(lpparam);
                if (appVersionName.startsWith("7.4.9")) {
                    utilCls = findClassIfExists("rb.e", lpparam.classLoader);
                } else {
                    utilCls = findClassIfExists("com.miui.permcenter.utils.h", lpparam.classLoader);
                }
                if (utilCls != null) {
                    Object fm = Helpers.getStaticObjectFieldSilently(utilCls, "b");
                    if (fm != null) {
                        try {
                            Map<String, Integer> featMap = (Map<String, Integer>) fm;
                            featMap.put("mi_lab_ai_clipboard_enable", 0);
                            //featMap.put("mi_lab_blur_location_enable", 0);
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
        });

        /*findAndHookMethod("com.miui.permcenter.utils.h", "<clinit>", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "IS_STABLE_VERSION", true);
            }
            //protected void before(MethodHookParam param) throws Throwable {
            //    param.setResult(true);
            //}
            protected void after(MethodHookParam param) throws Throwable {
                XC_MethodHook.Unhook();
            }
        });*/
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


