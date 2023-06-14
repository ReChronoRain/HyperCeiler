package com.sevtinge.cemiuiler.module.securitycenter;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.luckypray.dexkit.descriptor.member.DexClassDescriptor;

public class ScreenUsedTime extends BaseHook {
    Class<?> powerRankHelperHolder;
    @Override
    public void init() {
        try {
            List<DexClassDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultClassMap.get("PowerRankHelperHolder"));
            for (DexClassDescriptor descriptor : result) {
                powerRankHelperHolder = descriptor.getClassInstance(lpparam.classLoader);
                log("powerRankHelperHolder class is " + powerRankHelperHolder);
                findAndHookMethod(powerRankHelperHolder, "l", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
