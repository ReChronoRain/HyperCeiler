package com.sevtinge.cemiuiler.module.hook.thememanager;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

public class ThemeManagerDexKit extends BaseHook {

    public static Map<String, List<DexMethodDescriptor>> mThemeManagerResultMethodsMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        DexKitBridge bridge = DexKitBridge.create(apkPath);
        try {
            if (bridge == null) {
                return;
            }
            mThemeManagerResultMethodsMap =
                bridge.batchFindMethodsUsingStrings(
                    BatchFindArgs.builder()
                        .addQuery("DrmResult", Set.of("theme", "ThemeManagerTag", "/system", "check rights isLegal:"))
                        .addQuery("LargeIcon", Set.of(
                            "apply failed", "/data/system/theme/large_icons/", "default_large_icon_product_id", "largeicons", "relativePackageList is empty"
                        ))
                        .matchType(MatchType.CONTAINS)
                        .build()
                );
        } catch (Throwable e) {
            e.printStackTrace();
        }
        bridge.close();
    }
}
