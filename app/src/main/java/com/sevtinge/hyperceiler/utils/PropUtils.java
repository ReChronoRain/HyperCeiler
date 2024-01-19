package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public class PropUtils {
    private static final String TAG = com.sevtinge.hyperceiler.callback.TAG.TAG;

    public static String getProp(Context context, String name) {
        try {
            return classLoaderMethod(context, name);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils classLoader getProp String", e);
            return "";
        }
    }

    public static boolean getProp(String name, boolean def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return Boolean.TRUE.equals(invokeMethod(cls, "getBoolean", new Class[]{String.class, boolean.class}, name, def));
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp int", e);
            return false;
        }
    }

    public static int getProp(String name, int def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return invokeMethod(cls, "getInt", new Class[]{String.class, int.class}, name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp int", e);
            return 0;
        }
    }

    public static long getProp(String name, long def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return invokeMethod(cls, "getLong", new Class[]{String.class, long.class}, name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp long", e);
            return 0L;
        }
    }

    public static String getProp(String name, String def) {
        try {
            return invokeMethod(Class.forName("android.os.SystemProperties"),
                "get", new Class[]{String.class, String.class}, name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp String", e);
            return "";
        }
    }

    public static String getProp(String name) {
        try {
            return invokeMethod(Class.forName("android.os.SystemProperties"),
                "get", new Class[]{String.class}, name);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp String no def", e);
            return "";
        }
    }

    /**
     * 系统限制只能使用Root。
     * 返回 true 表示成功。
     *
     * @param name
     * @param vale
     * @return boolean
     */
    public static boolean setProp(String name, Object vale) {
        return ShellUtils.getResultBoolean("setprop " + name + " " + vale, true);
    }

    private static String classLoaderMethod(Context context, String name) throws Throwable {
        ClassLoader classLoader = context.getClassLoader();
        Class<?> cls = classLoader.loadClass("android.os.SystemProperties");
        Method method = cls.getDeclaredMethod("get", String.class);
        method.setAccessible(true);
        return (String) method.invoke(cls, name);
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T invokeMethod(Class<?> cls, String str, Class<?>[] clsArr, Object... objArr) throws Throwable {
        Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
        declaredMethod.setAccessible(true);
        return (T) declaredMethod.invoke(null, objArr);
    }
}
