package com.sevtinge.cemiuiler.module.joyose;

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

public class DisableCloudControl extends BaseHook {

    //Class<?> mCloud;

    Method cloudControl;

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
                                    .addQuery("CloudControl", List.of("job exist, sync local..."))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("CloudControl"));
            for (DexMethodDescriptor descriptor : result) {
                cloudControl = descriptor.getMethodInstance(lpparam.classLoader);
                if (cloudControl.getReturnType() == void.class) {
                    hookMethod(cloudControl, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.setResult(null);//2
                        }
                    });
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /*hookMethod(cloudControl, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);//2
            }
        });*/

        //mCloud = findClassIfExists("com.xiaomi.joyose.cloud.g$a");

    }
}
