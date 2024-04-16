package com.sevtinge.hyperceiler.module.hook.securitycenter;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

public class PowerSaver extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(
                FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("getEnduranceFromPowerKeeper flag: "))
                                .usingStrings("changePowerMode")
                        )
        ).singleOrNull();
        if (methodData == null) {
            logE(TAG, "method is null!");
            return;
        }
        try {
            hookMethod(methodData.getMethodInstance(lpparam.classLoader), new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(null);
                }
            });
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }
}
