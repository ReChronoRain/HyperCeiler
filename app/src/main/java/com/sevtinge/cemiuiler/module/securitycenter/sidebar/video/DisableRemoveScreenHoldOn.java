package com.sevtinge.cemiuiler.module.securitycenter.sidebar.video;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class DisableRemoveScreenHoldOn extends BaseHook {
    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("RemoveScreenHoldOn"));
            for (DexMethodDescriptor descriptor : result) {
                Method removeScreenHoldOn = descriptor.getMethodInstance(lpparam.classLoader);
                log("removeScreenHoldOn method is " + removeScreenHoldOn);
                if (removeScreenHoldOn.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(removeScreenHoldOn, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
