package com.sevtinge.cemiuiler.module;

import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.Helpers.MethodHook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PackagePermissions {

    private static final ArrayList<String> systemPackages = new ArrayList<String>();

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        systemPackages.add(Helpers.mAppModulePkg);

        // Allow signature level permissions for module
        Helpers.hookAllMethods("com.android.server.pm.permission.PermissionManagerService", lpparam.classLoader, "shouldGrantPermissionBySignature",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String pkgName = (String) XposedHelpers.getObjectField(param.args[0], "packageName");
                        if (systemPackages.contains(pkgName)) param.setResult(true);
                    }
                }
        );

        if (!Helpers.findAndHookMethodSilently("com.android.server.pm.PackageManagerServiceUtils", lpparam.classLoader, "verifySignatures",
                "com.android.server.pm.PackageSetting", "com.android.server.pm.PackageSetting", "android.content.pm.PackageParser.SigningDetails", boolean.class, boolean.class, boolean.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String pkgName = (String)XposedHelpers.getObjectField(param.args[0], "name");
                        if (systemPackages.contains(pkgName)) param.setResult(true);
                    }
                }
        )) Helpers.findAndHookMethod("com.android.server.pm.PackageManagerService", lpparam.classLoader, "verifySignaturesLP",
                "com.android.server.pm.PackageSetting", "android.content.pm.PackageParser.Package",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String pkgName = (String)XposedHelpers.getObjectField(param.args[1], "packageName");
                        if (systemPackages.contains(pkgName)) param.setResult(true);
                    }
                }
        );

        // Make module appear as system app
        Helpers.hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "queryIntentActivitiesInternal", new MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void after(MethodHookParam param) throws Throwable {
                List<ResolveInfo> infos = (List<ResolveInfo>)param.getResult();
                if (infos != null)
                    for (ResolveInfo info : infos)
                        if (info != null && info.activityInfo != null && systemPackages.contains(info.activityInfo.packageName))
                            XposedHelpers.setObjectField(info, "system", true);
            }
        });

        Helpers.findAndHookMethod("android.content.pm.ApplicationInfo", lpparam.classLoader, "isSystemApp", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo ai = (ApplicationInfo)param.thisObject;
                if (ai != null && systemPackages.contains(ai.packageName)) param.setResult(true);
            }
        });

        //noinspection ResultOfMethodCallIgnored
        Helpers.findAndHookMethodSilently("android.content.pm.ApplicationInfo", lpparam.classLoader, "isSignedWithPlatformKey", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo ai = (ApplicationInfo)param.thisObject;
                if (ai != null && systemPackages.contains(ai.packageName)) param.setResult(true);
            }
        });

        Helpers.hookAllMethodsSilently("com.android.server.wm.ActivityRecordInjector", lpparam.classLoader, "canShowWhenLocked", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        try {
            Class<?> dpgpiClass = XposedHelpers.findClass("com.android.server.pm.MiuiDefaultPermissionGrantPolicy", lpparam.classLoader);
            String[] MIUI_SYSTEM_APPS = (String[])XposedHelpers.getStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS");
            ArrayList<String> mySystemApps = new ArrayList<String>(Arrays.asList(MIUI_SYSTEM_APPS));
            mySystemApps.addAll(systemPackages);
            XposedHelpers.setStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS", mySystemApps.toArray(new String[0]));
        } catch (Throwable t) {
            Helpers.log(t);
        }
    }
}
