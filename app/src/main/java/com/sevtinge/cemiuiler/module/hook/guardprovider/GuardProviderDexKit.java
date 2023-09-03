package com.sevtinge.cemiuiler.module.hook.guardprovider;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

public class GuardProviderDexKit extends BaseHook {

    public static Map<String, List<DexMethodDescriptor>> mGuardProviderResultMethodsMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        DexKitBridge bridge = DexKitBridge.create(apkPath);
        try {
            if (bridge == null) {
                return;
            }
            mGuardProviderResultMethodsMap =
                bridge.batchFindMethodsUsingStrings(
                    BatchFindArgs.builder()
                        .addQuery("AntiDefraudAppManager", Set.of("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app"))
                        .matchType(MatchType.CONTAINS)
                        .build()
                );
        } catch (Throwable e) {
            e.printStackTrace();
        }
        bridge.close();
    }
}
