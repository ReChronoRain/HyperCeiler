package com.sevtinge.cemiuiler.module.joyose;

import static com.sevtinge.cemiuiler.module.joyose.JoyoseDexKit.mJoyoseResultMethodsMap;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class EnableGpuTuner extends BaseHook {

    // Class<?> mCloud;

    Method gpuTuner;

    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mJoyoseResultMethodsMap.get("GpuTuner"));
            for (DexMethodDescriptor descriptor : result) {
                gpuTuner = descriptor.getMethodInstance(lpparam.classLoader);
                if (gpuTuner.getReturnType() == boolean.class) {
                    hookMethod(gpuTuner, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.setResult(true);// 2
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

        // mCloud = findClassIfExists("com.xiaomi.joyose.cloud.g$a");

    }
}
