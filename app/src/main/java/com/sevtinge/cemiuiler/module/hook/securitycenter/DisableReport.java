package com.sevtinge.cemiuiler.module.hook.securitycenter;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class DisableReport extends BaseHook {
    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("IsShowReport"));
            for (DexMethodDescriptor descriptor : result) {
                Method isShowReport = descriptor.getMethodInstance(lpparam.classLoader);
                log("isShowReport method is " + isShowReport);
                if (isShowReport.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(isShowReport, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
