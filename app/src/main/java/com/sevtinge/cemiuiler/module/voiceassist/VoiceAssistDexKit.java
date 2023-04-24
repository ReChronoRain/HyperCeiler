package com.sevtinge.cemiuiler.module.voiceassist;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.DexKitHelper;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.util.List;
import java.util.Map;

import com.sevtinge.cemiuiler.utils.DexKitHelper;

public class VoiceAssistDexKit extends BaseHook {

    public static Map<String, List<DexMethodDescriptor>> mVoiceAssistResultMethodsMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            mVoiceAssistResultMethodsMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("BrowserActivityWithIntent", List.of("IntentUtils", "permission click No Application can handle your intent"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
        DexKitHelper.closeDexKit();
    }
}