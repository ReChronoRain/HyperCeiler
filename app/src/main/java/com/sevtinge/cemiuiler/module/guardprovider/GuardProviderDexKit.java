package com.sevtinge.cemiuiler.module.guardprovider;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.DexKitHelper;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.util.List;
import java.util.Map;

public class GuardProviderDexKit extends BaseHook {

    public static Map<String, List<DexMethodDescriptor>> mGuardProviderResultMethodsMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            mGuardProviderResultMethodsMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("AntiDefraudAppManager", List.of("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
        DexKitHelper.closeDexKit();
    }
}