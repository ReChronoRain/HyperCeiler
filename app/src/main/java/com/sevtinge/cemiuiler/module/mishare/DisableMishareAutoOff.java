package com.sevtinge.cemiuiler.module.mishare;

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

public class DisableMishareAutoOff extends BaseHook {

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
                                    .addQuery("MiShareAutoOff", List.of("MiShareService", "EnabledState"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("MiShareAutoOff"));
            for (DexMethodDescriptor descriptor : result) {
                Method miShareAutoOff = descriptor.getMethodInstance(lpparam.classLoader);
                if (miShareAutoOff.getReturnType() == Boolean.class) {
                    XposedBridge.hookMethod(miShareAutoOff, XC_MethodReplacement.returnConstant(null));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}


