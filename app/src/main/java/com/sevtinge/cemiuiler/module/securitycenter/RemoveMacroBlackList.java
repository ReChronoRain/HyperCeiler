package com.sevtinge.cemiuiler.module.securitycenter;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RemoveMacroBlackList extends BaseHook {

    Class<?> m;

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
                                    .addQuery("unsupportedAppList", List.of("pref_gb_unsupport_macro_apps"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("unsupportedAppList"));
            for (DexMethodDescriptor descriptor : result) {
                Method unsupportedAppList = descriptor.getMethodInstance(lpparam.classLoader);
                //XposedBridge.log("Cemiuiler: UnsupportAppList method is " + unsupportedAppList);
                if (unsupportedAppList.getReturnType() == ArrayList.class) {
                    XposedBridge.log("Cemiuiler: Hook UnsupportAppList method is " + unsupportedAppList);
                    XposedBridge.hookMethod(unsupportedAppList, XC_MethodReplacement.returnConstant(new ArrayList<>()));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        char letter = 'a';

        for (int i = 0; i < 26; i++) {
            String appVersionName = getPackageVersion(lpparam);
            if (appVersionName.startsWith("7.4.9")) {
                m = findClass("q7." + letter + "0");
            } else {
                m = findClass("com.miui.gamebooster.utils." + letter + "0");
            }
            if (m != null) {
                int length = m.getDeclaredMethods().length;
                if (length >= 10 && length <= 15) {
                    Field[] fields = m.getFields();
                    if (fields.length == 0 && m.getDeclaredFields().length >= 2) {
                        if (appVersionName.startsWith("7.4.9")) {
                            findAndHookMethod(m, "g", String.class, new MethodHook() {
                                @Override
                                protected void before(MethodHookParam param) throws Throwable {
                                    param.setResult(false);
                                }
                            });
                        } else {
                            findAndHookMethod(m, "c", String.class, new MethodHook() {
                                @Override
                                protected void before(MethodHookParam param) throws Throwable {
                                    param.setResult(false);
                                }
                            });
                        }
                    }
                    continue;
                }
            }
            letter = (char) (letter + 1);
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
