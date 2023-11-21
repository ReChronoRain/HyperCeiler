package com.sevtinge.hyperceiler.module.hook.milink;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class UnlockHMind extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.milink.hmindlib.j", "C", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
