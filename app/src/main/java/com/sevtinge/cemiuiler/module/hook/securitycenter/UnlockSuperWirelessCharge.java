package com.sevtinge.cemiuiler.module.hook.securitycenter;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class UnlockSuperWirelessCharge extends BaseHook {
    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("SuperWirelessCharge"));
            for (DexMethodDescriptor descriptor : result) {
                Method SuperWirelessCharge = descriptor.getMethodInstance(lpparam.classLoader);
                log("SuperWirelessCharge method is " + SuperWirelessCharge);
                if (SuperWirelessCharge.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(SuperWirelessCharge, XC_MethodReplacement.returnConstant(true));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("SuperWirelessChargeTip"));
            for (DexMethodDescriptor descriptor : result) {
                Method SuperWirelessChargeTip = descriptor.getMethodInstance(lpparam.classLoader);
                log("SuperWirelessChargeTip method is " + SuperWirelessChargeTip);
                if (SuperWirelessChargeTip.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(SuperWirelessChargeTip, XC_MethodReplacement.returnConstant(true));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
