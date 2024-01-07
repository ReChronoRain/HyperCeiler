package com.sevtinge.hyperceiler.module.hook.securitycenter;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class InstallIntercept extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(
            FindMethod.create()
                .matcher(MethodMatcher.create()
                    .usingStrings("permcenter_install_intercept_enabled")
                    .returnType(boolean.class)
                )
        ).singleOrThrow(() -> new IllegalStateException("Find permcenter_install_intercept_enabled E"));

        // logE(TAG, "find: " + methodData.getMethodInstance(lpparam.classLoader));

        Method method = methodData.getMethodInstance(lpparam.classLoader);

        hookMethod(method,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(false);
                }
            }
        );
    }
}
