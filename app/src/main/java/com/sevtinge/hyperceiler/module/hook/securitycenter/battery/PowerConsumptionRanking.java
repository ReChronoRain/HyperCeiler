package com.sevtinge.hyperceiler.module.hook.securitycenter.battery;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Method;

public class PowerConsumptionRanking extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        ClassDataList data = DexKit.INSTANCE.getDexKitBridge().findClass(
            FindClass.create()
                .matcher(ClassMatcher.create()
                    .usingStrings("%d %s %d %s")
                )
        );
        MethodDataList methodDataList = DexKit.INSTANCE.getDexKitBridge().findMethod(
            FindMethod.create()
                .matcher(MethodMatcher.create()
                    .declaredClass(ClassMatcher.create()
                        .usingStrings("ro.miui.ui.version.code"))
                    .usingNumbers(9)
                    .returnType(boolean.class)
                )
        );
        for (ClassData clazzData : data) {
            try {
                logI(TAG, lpparam.packageName, "Current hooking clazz is " + clazzData.getInstance(lpparam.classLoader));
                try {
                    hookAllConstructors(clazzData.getInstance(lpparam.classLoader), new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws NoSuchMethodException {
                            for (MethodData methodData : methodDataList) {
                                Method method = methodData.getMethodInstance(lpparam.classLoader);
                                logI(TAG, lpparam.packageName, "Current hooking method is " + method);
                                try {
                                    hookMethod(method, new MethodHook() {
                                        @Override
                                        protected void before(MethodHookParam param) throws Throwable {
                                            param.setResult(false);
                                        }
                                    });
                                } catch (Exception ignore) {
                                }
                            }
                        }
                    });
                } catch (Exception ignored) {
                }
            } catch (ClassNotFoundException e) {
                logE(TAG, lpparam.packageName, "Cannot found any clazz" + e);
            }
        }
    }
}
