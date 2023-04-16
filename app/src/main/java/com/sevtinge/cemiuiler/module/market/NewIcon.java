package com.sevtinge.cemiuiler.module.market;

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

public class NewIcon extends BaseHook {
    static Method isDesktopSupportOperationIcon;
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
                                    .addQuery("DesktopSupportOperationIcon", List.of("com.miui.home", "supportOperationIcon", "AppGlobals.getContext()"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("DesktopSupportOperationIcon"));
            for (DexMethodDescriptor descriptor : result) {
                isDesktopSupportOperationIcon = descriptor.getMethodInstance(lpparam.classLoader);
                log("isDesktopSupportOperationIcon method is " + isDesktopSupportOperationIcon);
                if (isDesktopSupportOperationIcon.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(isDesktopSupportOperationIcon, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        hookAllMethods("com.xiaomi.market.util.FileUtils", "ensureExternalIconDir", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(false);
            }
        });
    }
}
