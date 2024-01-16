package com.sevtinge.hyperceiler;

import android.app.ApplicationErrorReport;

import com.sevtinge.hyperceiler.utils.hook.HookUtils;

import java.lang.reflect.Method;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 推荐使用的方法，直接 Hook 系统，
 * 可能误报，但是很稳定。
 */
public class CrashHook extends HookUtils {

    public CrashHook(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Exception {
        init(loadPackageParam.classLoader);
    }

    public void init(ClassLoader classLoader) throws Exception {
        Class<?> appError = findClassIfExists("com.android.server.am.AppErrors", classLoader);
        if (appError == null) {
            throw new ClassNotFoundException("No such 'com.android.server.am.AppErrors' classLoader: " + classLoader);
        }
        Method hookError = null;
        for (Method error : appError.getDeclaredMethods()) {
            if ("handleAppCrashInActivityController".equals(error.getName()))
                if (error.getReturnType().equals(boolean.class)) {
                    hookError = error;
                    break;
                }
        }
        if (hookError == null) {
            throw new NoSuchMethodException("No such Method: handleAppCrashInActivityController, ClassLoader: " + classLoader);
        }
        logE("CrashHook", "get method: " + hookError.getName());
        hookMethod(hookError, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Object proc = param.args[0];
                    ApplicationErrorReport.CrashInfo crashInfo = (ApplicationErrorReport.CrashInfo) param.args[1];
                }
            }
        );
    }
}
