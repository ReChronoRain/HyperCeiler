package com.sevtinge.hyperceiler.module.hook.milink;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.Objects;

public class UnlockHMind extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("HMindManager", "isHMindAble() context == null", "isHMindAble() isSupport: ")
            )
        ).firstOrThrow(() -> new IllegalStateException("UnlockHMind: Cannot found MethodData"));
        Method method = methodData.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "isHMindAble() method is " + method);
        hookMethod(method, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}

