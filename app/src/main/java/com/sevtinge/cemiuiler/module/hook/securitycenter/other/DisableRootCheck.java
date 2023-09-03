package com.sevtinge.cemiuiler.module.hook.securitycenter.other;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.hook.securitycenter.SecurityCenterDexKit;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class DisableRootCheck extends BaseHook {
    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("rootCheck"));
            for (DexMethodDescriptor descriptor : result) {
                Method checkIsRooted = descriptor.getMethodInstance(lpparam.classLoader);
                if (checkIsRooted.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(checkIsRooted, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


}
