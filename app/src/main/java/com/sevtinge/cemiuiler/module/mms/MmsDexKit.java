package com.sevtinge.cemiuiler.module.mms;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor;
import io.luckypray.dexkit.enums.MatchType;

public class MmsDexKit extends BaseHook {

    public static Map<String, List<DexClassDescriptor>> mMmsResultClassMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        DexKitBridge bridge = DexKitBridge.create(apkPath);
        try {
            if (bridge == null) {
                return;
            }
            mMmsResultClassMap =
                bridge.batchFindClassesUsingStrings(
                    BatchFindArgs.builder()
                        .addQuery("DisableAd", Set.of("Unknown type of the message: "))
                        .matchType(MatchType.CONTAINS)
                        .build()
                );
        } catch (Throwable e) {
            e.printStackTrace();
        }
        bridge.close();
    }
}
