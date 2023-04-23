package com.sevtinge.cemiuiler.module.joyose;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import static com.sevtinge.cemiuiler.module.joyose.JoyoseDexKit.mJoyoseResultMethodsMap;

public class DisableCloudControl extends BaseHook {

    //Class<?> mCloud;

    Method cloudControl;

    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mJoyoseResultMethodsMap.get("CloudControl"));
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
