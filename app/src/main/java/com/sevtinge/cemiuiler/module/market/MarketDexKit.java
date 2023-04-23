package com.sevtinge.cemiuiler.module.market;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.util.List;
import java.util.Map;

public class MarketDexKit extends BaseHook {

    public static Map<String, List<DexMethodDescriptor>> mMarketResultMethodsMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            mMarketResultMethodsMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("DesktopSupportOperationIcon", List.of("com.miui.home", "supportOperationIcon", "AppGlobals.getContext()"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}