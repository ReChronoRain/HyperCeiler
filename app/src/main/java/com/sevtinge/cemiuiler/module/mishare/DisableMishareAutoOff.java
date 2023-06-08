package com.sevtinge.cemiuiler.module.mishare;

import static com.sevtinge.cemiuiler.module.mishare.MiShareDexKit.mMiShareResultMethodsMap;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class DisableMishareAutoOff extends BaseHook {

    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mMiShareResultMethodsMap.get("MiShareAutoOff"));
            for (DexMethodDescriptor descriptor : result) {
                Method miShareAutoOff = descriptor.getMethodInstance(lpparam.classLoader);
                if (miShareAutoOff.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(miShareAutoOff, XC_MethodReplacement.returnConstant(null));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}


