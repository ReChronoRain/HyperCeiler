package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;

public class DisableVerifyCanBeDisabled extends BaseHook {
    @Override
    public void init() {
        Class<?> pkg = findClassIfExists("com.android.server.pm.PackageManagerServiceImpl");
        if (pkg == null) {
            logE(TAG, "find class E com.android.server.pm.PackageManagerServiceImpl");
            return;
        }
        Method[] methods = pkg.getDeclaredMethods();
        Method voidMethod = null;
        Method booleanMethod = null;
        for (Method method : methods) {
            if ("canBeDisabled".equals(method.getName())) {
                if (method.getReturnType().equals(void.class)) {
                    voidMethod = method;
                    break;
                } else if (method.getReturnType().equals(boolean.class)) {
                    booleanMethod = method;
                    break;
                }
            }
        }
        if (voidMethod == null && booleanMethod == null) {
            logE(TAG, "method is null");
            return;
        }
        if (voidMethod != null) {
            hookMethod(voidMethod,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );
        } else {
            hookMethod(booleanMethod,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        }
    }
}
