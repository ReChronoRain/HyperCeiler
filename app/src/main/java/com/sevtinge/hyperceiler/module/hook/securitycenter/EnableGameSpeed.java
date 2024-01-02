package com.sevtinge.hyperceiler.module.hook.securitycenter;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class EnableGameSpeed extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData1 = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("debug.game.video.support")
                .returnType(boolean.class)
            )
        ).firstOrThrow(() -> new IllegalStateException("EnableGameSpeed: Cannot found MethodData usingString \"debug.game.video.support\""));
        Method method1 = methodData1.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "UsingString \"debug.game.video.support\" method is " + method1);
        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        MethodData methodData2 = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("pref_open_game_booster")
                .returnType(boolean.class)
            )
        ).firstOrThrow(() -> new IllegalStateException("EnableGameSpeed: Cannot found MethodData usingString \"pref_open_game_booster\""));
        Method method2 = methodData2.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "UsingString \"pref_open_game_booster\" method is " + method2);
        hookMethod(method2, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
