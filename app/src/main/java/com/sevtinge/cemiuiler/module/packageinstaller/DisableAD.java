package com.sevtinge.cemiuiler.module.packageinstaller;

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

public class DisableAD extends BaseHook {

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
                                    .addQuery("EnableAds", List.of("ads_enable"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("EnableAds"));
            for (DexMethodDescriptor descriptor : result) {
                Method enableAds = descriptor.getMethodInstance(lpparam.classLoader);
                XposedBridge.log("Cemiuiler: DisableAD enableAds method is "+ enableAds);
                if (enableAds.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(enableAds, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            Map<String, List<DexMethodDescriptor>> resultMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("AppStoreRecommend", List.of("app_store_recommend"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("AppStoreRecommend"));
            for (DexMethodDescriptor descriptor : result) {
                Method appStoreRecommend = descriptor.getMethodInstance(lpparam.classLoader);
                XposedBridge.log("Cemiuiler: DisableAD appStoreRecommend method is "+ appStoreRecommend);
                if (appStoreRecommend.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(appStoreRecommend, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            Map<String, List<DexMethodDescriptor>> resultMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("InstallerOpenSafetyModel", List.of("installerOpenSafetyModel"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("InstallerOpenSafetyModel"));
            for (DexMethodDescriptor descriptor : result) {
                Method installerOpenSafetyModel = descriptor.getMethodInstance(lpparam.classLoader);
                XposedBridge.log("Cemiuiler: DisableAD installerOpenSafetyModel method is "+ installerOpenSafetyModel);
                if (installerOpenSafetyModel.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(installerOpenSafetyModel, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
