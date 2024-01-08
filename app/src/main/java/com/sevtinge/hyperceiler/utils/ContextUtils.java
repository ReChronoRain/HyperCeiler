package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

@SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
public class ContextUtils {
    private static final String TAG = "[HyperCeiler]";

    public static Context getContext() {
        try {
            return invokeMethod();
        } catch (Throwable e) {
            Log.e(TAG, "getContext: ", e);
            return null;
        }
    }

    private static Context invokeMethod() throws Throwable {
        Context context;
        Class<?> clz = Class.forName("android.app.ActivityThread");
        Method currentApplication = clz.getDeclaredMethod("currentApplication");
        currentApplication.setAccessible(true);
        context = (Application) currentApplication.invoke(null);
        if (context == null) {
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
        }
        if (context == null) throw new Throwable("Context is null");
        return context;
    }

}
