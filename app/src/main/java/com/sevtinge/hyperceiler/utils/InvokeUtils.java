package com.sevtinge.hyperceiler.utils;

import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 焕晨HChen
 * @noinspection unchecked
 */
public class InvokeUtils {
    private final static String TAG = "invokeUtils";

    public static <T> T invokeSetField(Class<?> clz, Object instance, String field, Object value) {
        return baseInvokeField(clz, instance, field, value);
    }

    public static <T> T invokeSetStaticField(Class<?> clz, String field, Object value) {
        return baseInvokeField(clz, null, field, value);
    }

    public static <T> T invokeField(Class<?> clz, Object instance, String field) {
        return baseInvokeField(clz, instance, field, null);
    }

    public static <T> T invokeStaticField(Class<?> clz, String field) {
        return baseInvokeField(clz, null, field, null);
    }

    public static <T> T invokeMethod(Class<?> clz, Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(clz, instance, method, param, value);
    }

    public static <T> T invokeStaticMethod(Class<?> clz, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(clz, null, method, param, value);
    }

    public static <T> T invokeSetField(String className, ClassLoader classLoader, Object instance, String field, Object value) {
        return baseInvokeField(className, classLoader, instance, field, value);
    }

    public static <T> T invokeSetStaticField(String className, ClassLoader classLoader, String field, Object value) {
        return baseInvokeField(className, classLoader, null, field, value);
    }

    public static <T> T invokeField(String className, ClassLoader classLoader, Object instance, String field) {
        return baseInvokeField(className, classLoader, instance, field, null);
    }

    public static <T> T invokeStaticField(String className, ClassLoader classLoader, String field) {
        return baseInvokeField(className, classLoader, null, field, null);
    }

    public static <T> T invokeMethod(String className, ClassLoader classLoader, Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(className, classLoader, instance, method, param, value);
    }

    public static <T> T invokeStaticMethod(String className, ClassLoader classLoader, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(className, classLoader, null, method, param, value);
    }

    public static <T> T invokeSetField(String className, Object instance, String field, Object value) {
        return baseInvokeField(className, instance, field, value);
    }

    public static <T> T invokeSetStaticField(String className, String field, Object value) {
        return baseInvokeField(className, null, field, value);
    }

    public static <T> T invokeField(String className, Object instance, String field) {
        return baseInvokeField(className, instance, field, null);
    }

    public static <T> T invokeStaticField(String className, String field) {
        return baseInvokeField(className, null, field, null);
    }

    public static <T> T invokeMethod(String className, Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(className, instance, method, param, value);
    }


    public static <T> T invokeStaticMethod(String className, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(className, null, method, param, value);
    }

    private static <T> T baseInvokeMethod(String className, Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(className, null, instance, method, param, value);
    }

    private static <T> T baseInvokeMethod(String className, ClassLoader classLoader, Object instance, String method, Class<?>[] param, Object... value) {
        try {
            Class<?> clz = baseClass(classLoader, className);
            return baseInvokeMethod(clz, instance, method, param, value);
        } catch (ClassNotFoundException e) {
            AndroidLogUtils.logE(TAG, "Reflection call method failed! class: " + className + " method: " + method, e);
            return null;
        }
    }

    private static <T> T baseInvokeMethod(Class<?> clz, Object instance, String method, Class<?>[] param, Object... value) {
        try {
            Method declaredMethod = clz.getDeclaredMethod(method, param);
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(instance, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            AndroidLogUtils.logE(TAG, "Reflection call method failed! class: " + clz.getName() + " method: " + method, e);
            return null;
        }
    }

    private static <T> T baseInvokeField(String className, Object instance, String field, Object value) {
        return baseInvokeField(className, null, instance, field, value);
    }

    private static <T> T baseInvokeField(String className, ClassLoader classLoader, Object instance, String field, Object value) {
        try {
            Class<?> clz = baseClass(classLoader, className);
            return baseInvokeField(clz, instance, field, value);
        } catch (ClassNotFoundException e) {
            AndroidLogUtils.logE(TAG, "Reflection call method failed! class: " + className + " field: " + field, e);
            return null;
        }
    }

    private static <T> T baseInvokeField(Class<?> clz, Object instance, String field, Object value) {
        try {
            Field declaredField = clz.getDeclaredField(field);
            declaredField.setAccessible(true);
            if (value != null) {
                declaredField.set(instance, value);
                return null;
            } else
                return (T) declaredField.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            AndroidLogUtils.logE(TAG, "Reflection call method failed! class: " + clz.getName() + " field: " + field, e);
            return null;
        }
    }

    private static Class<?> baseClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (classLoader == null) {
            return Class.forName(className);
        }
        return classLoader.loadClass(className);
    }
}