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

        /*MethodData getPropVoidData = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("android.os.SystemProperties", "set", "SystemPropertiesUtils", "SystemPropertiesUtils getInt:")
                .returnType(void.class)
            )
        ).firstOrThrow(() -> new IllegalStateException("EnableGameSpeed: Cannot found getPropVoid method"));
        Method getPropVoid = getPropVoidData.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "getPropVoid method is " + getPropVoid);
        hookMethod(getPropVoid, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (param.args[0] == "debug.game.video.speed") param.args[1] = true;
            }
        });

        MethodData getPropBooleanData = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("android.os.SystemProperties", "getBoolean", "SystemPropertiesUtils", "SystemPropertiesUtils getInt:")
                .returnType(boolean.class)
            )
        ).firstOrThrow(() -> new IllegalStateException("EnableGameSpeed: Cannot found getPropBoolean method"));
        Method getBooleanVoid = getPropBooleanData.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "getPropBoolean method is " + getBooleanVoid);
        hookMethod(getBooleanVoid, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (param.args[0] == "debug.game.video.support") param.setResult(true);
            }
        });*/

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

        MethodData methodData3 = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("debug.game.video.boot")
            )
        ).firstOrThrow(() -> new IllegalStateException("EnableGameSpeed: Cannot found MethodData usingString \"debug.game.video.boot\""));
        Method method3 = methodData3.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "UsingString \"debug.game.video.boot\" method is " + method3);
        hookMethod(method3, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                mSetProp();
                param.setResult(null);
            }
        });

        findAndHookMethod("com.miui.gamebooster.service.GameBoosterService",
            "onCreate", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mSetProp();
                }
            }
        );
    }

    public void mSetProp() {
        setProp("debug.game.video.boot", "true");
        setProp("debug.game.video.speed", "true");
    }
}
