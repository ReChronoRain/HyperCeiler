package com.sevtinge.cemiuiler.module.hook.joyose;

/*import static com.sevtinge.cemiuiler.module.hook.joyose.JoyoseDexKit.mJoyoseResultMethodsMap;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class DisableCloudControl extends BaseHook {

    // Class<?> mCloud;

    Method cloudControl;

    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(JoyoseDexKit.mJoyoseResultMethodsMap.get("CloudControl"));
            for (DexMethodDescriptor descriptor : result) {
                cloudControl = descriptor.getMethodInstance(lpparam.classLoader);
                if (cloudControl.getReturnType() == void.class) {
                    hookMethod(cloudControl, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            param.setResult(null);// 2
                        }
                    });
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        *//*hookMethod(cloudControl, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);//2
            }
        });*//*

        // mCloud = findClassIfExists("com.xiaomi.joyose.cloud.g$a");

    }
}*/
