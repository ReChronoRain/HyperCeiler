package com.sevtinge.cemiuiler.module.joyose;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.util.List;
import java.util.Map;

public class JoyoseDexKit extends BaseHook {

    public static Map<String, List<DexMethodDescriptor>> mJoyoseResultMethodsMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            mJoyoseResultMethodsMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("CloudControl", List.of("job exist, sync local..."))
                                    .addQuery("GpuTuner", List.of("GPUTUNER_SWITCH"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}