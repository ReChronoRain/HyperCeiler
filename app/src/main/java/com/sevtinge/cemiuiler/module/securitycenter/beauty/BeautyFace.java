package com.sevtinge.cemiuiler.module.securitycenter.beauty;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BeautyFace extends BaseHook {
    static Method beautyFace;
    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            Map<String, List<DexMethodDescriptor>> resultMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("BeautyFace", List.of("taoyao", "IN", "persist.vendor.vcb.ability"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("BeautyFace"));
            for (DexMethodDescriptor descriptor : result) {
                beautyFace = descriptor.getMethodInstance(lpparam.classLoader);
                XposedBridge.log("Cemiuiler: beautyFace method is " + beautyFace);
                if (beautyFace.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(beautyFace, XC_MethodReplacement.returnConstant(true));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        /*
        int appVersionCode = getPackageVersionCode(lpparam);
        if (appVersionCode == 40000749) {
            findAndHookMethod("p5.f", "D", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else {
            findAndHookMethod("com.miui.gamebooster.beauty.i", "z", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }
        */
    }

    private static String getPackageVersionName(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            String versionName = (String) XposedHelpers.getObjectField(pkg, "mVersionName");
            XposedBridge.log("Cemiuiler: " + lpparam + " versionName is " + versionName);
            return versionName;
        } catch (Throwable e) {
            XposedBridge.log("Cemiuiler: Unknown Version.");
            XposedBridge.log(e);
            return "null";
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
