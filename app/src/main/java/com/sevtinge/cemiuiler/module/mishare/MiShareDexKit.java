package com.sevtinge.cemiuiler.module.mishare;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.util.List;
import java.util.Map;

public class MiShareDexKit extends BaseHook {

    public static Map<String, List<DexMethodDescriptor>> mMiShareResultMethodsMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        DexKitBridge bridge = DexKitBridge.create(apkPath);
        try {
            if (bridge == null) {
                return;
            }
            mMiShareResultMethodsMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("MiShareAutoOff", List.of("MiShareService", "EnabledState"))
                                    .addQuery("qwq", List.of("EnabledState", "mishare_enabled"))
                                    .addQuery("qwq2", List.of("null context", "cta_agree"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );
            } catch (Throwable e) {
            e.printStackTrace();
        }
        bridge.close();
    }
}
