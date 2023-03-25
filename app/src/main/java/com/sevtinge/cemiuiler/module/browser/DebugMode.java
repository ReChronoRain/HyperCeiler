package com.sevtinge.cemiuiler.module.browser;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DebugMode extends BaseHook {
    @Override
    public void init() {
        boolean found = false;
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            Map<String, List<DexMethodDescriptor>> resultMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("DebugMode", List.of("pref_key_debug_mode_new"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );
            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("DebugMode"));
            for (DexMethodDescriptor descriptor : result) {
                Method DebugMode = descriptor.getMethodInstance(lpparam.classLoader);
                if (DebugMode.getReturnType() == boolean.class && String.valueOf(DebugMode).contains("getDebugMode")) {
                    XposedBridge.log("Cemiuiler: DebugMode method is " + DebugMode);
                    found = true;
                    XposedBridge.hookMethod(DebugMode, XC_MethodReplacement.returnConstant(true));
                }
            }
            if (!found) {
                Map<String, List<DexMethodDescriptor>> resultMap1 =
                        bridge.batchFindMethodsUsingStrings(
                                BatchFindArgs.builder()
                                        .addQuery("DebugMode1", List.of("pref_key_debug_mode"))
                                        .matchType(MatchType.CONTAINS)
                                        .build()
                        );
                List<DexMethodDescriptor> result1 = Objects.requireNonNull(resultMap1.get("DebugMode1"));
                for (DexMethodDescriptor descriptor1 : result1) {
                    Method DebugMode1 = descriptor1.getMethodInstance(lpparam.classLoader);
                    if (DebugMode1.getReturnType() == boolean.class && String.valueOf(DebugMode1).contains("getDebugMode")) {
                        XposedBridge.log("Cemiuiler: DebugMode1 method is " + DebugMode1);
                        XposedBridge.hookMethod(DebugMode1, XC_MethodReplacement.returnConstant(true));
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}


