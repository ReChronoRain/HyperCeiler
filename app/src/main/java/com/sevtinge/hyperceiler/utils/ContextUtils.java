package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

@SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
public class ContextUtils {
    private static final String TAG = "[HyperCeiler]";
    // 尝试全部
    public static final int FLAG_ALL = 0;
    // 仅获取当前应用
    public static final int FLAG_CURRENT_APP = 1;
    // 获取 Android 系统
    public static final int FlAG_ONLY_ANDROID = 2;

    public static Context getContext(int flag) {
        try {
            return invokeMethod(flag);
        } catch (Throwable e) {
            Log.e(TAG, "getContext: ", e);
            return null;
        }
    }

    private static Context invokeMethod(int flag) throws Throwable {
        Context context;
        Class<?> clz = Class.forName("android.app.ActivityThread");
        switch (flag) {
            case 0 -> {
                if ((context = currentApp(clz)) == null) {
                    context = android(clz);
                }
            }
            case 1 -> {
                context = currentApp(clz);
            }
            case 2 -> {
                context = android(clz);
            }
            default -> {
                throw new Throwable("Unexpected flag");
            }
        }
        if (context == null) throw new Throwable("Context is null");
        return context;
    }

    private static Context currentApp(Class<?> clz) throws Throwable {
        // 获取当前界面应用 Context
        Method currentApplication = clz.getDeclaredMethod("currentApplication");
        currentApplication.setAccessible(true);
        return (Application) currentApplication.invoke(null);
    }

    private static Context android(Class<?> clz) throws Throwable {
        // 获取 Android
        Context context;
        Method currentActivityThread = clz.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        Object o = currentActivityThread.invoke(null);
        Method getSystemContext = clz.getDeclaredMethod("getSystemContext");
        getSystemContext.setAccessible(true);
        context = (Context) getSystemContext.invoke(o);
        if (context == null) {
            Method getSystemUiContext = clz.getDeclaredMethod("getSystemUiContext");
            getSystemUiContext.setAccessible(true);
            context = (Context) getSystemContext.invoke(o);
        }
        return context;
    }

}
