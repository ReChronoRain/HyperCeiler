package com.sevtinge.hyperceiler.utils;

import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author 焕晨HChen
 * @noinspection unused
 */
public class InvokeUtils {
    private static final HashMap<String, Method> methodCache = new HashMap<>();
    private static final HashMap<String, Field> fieldCache = new HashMap<>();

    private final static String TAG = "invokeUtils";

    // ----------------------------设置字段--------------------------------
    // Class 类型
    public static <T> T setField(Class<?> clz, Object instance, String field, Object value) {
        return baseInvokeField(clz, instance, field, true, value);
    }

    public static <T> T setStaticField(Class<?> clz, String field, Object value) {
        return baseInvokeField(clz, null, field, true, value);
    }

    public static <T> T getField(Class<?> clz, Object instance, String field) {
        return baseInvokeField(clz, instance, field, false, null);
    }

    public static <T> T getStaticField(Class<?> clz, String field) {
        return baseInvokeField(clz, null, field, false, null);
    }

    // String类型
    public static <T> T setField(String className, ClassLoader classLoader, Object instance, String field, Object value) {
        return baseInvokeField(className, classLoader, instance, field, true, value);
    }

    public static <T> T setStaticField(String className, ClassLoader classLoader, String field, Object value) {
        return baseInvokeField(className, classLoader, null, field, true, value);
    }

    public static <T> T getField(String className, ClassLoader classLoader, Object instance, String field) {
        return baseInvokeField(className, classLoader, instance, field, false, null);
    }

    public static <T> T getStaticField(String className, ClassLoader classLoader, String field) {
        return baseInvokeField(className, classLoader, null, field, false, null);
    }

    // 无 ClassLoader
    public static <T> T setField(String className, Object instance, String field, Object value) {
        return baseInvokeField(className, instance, field, true, value);
    }

    public static <T> T setStaticField(String className, String field, Object value) {
        return baseInvokeField(className, null, field, true, value);
    }

    public static <T> T getField(String className, Object instance, String field) {
        return baseInvokeField(className, instance, field, false, null);
    }

    public static <T> T getStaticField(String className, String field) {
        return baseInvokeField(className, null, field, false, null);
    }

    // ----------------------------反射调用方法--------------------------------
    // Class 类型
    public static <T> T callMethod(Class<?> clz, Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(clz, instance, method, param, value);
    }

    public static <T> T callStaticMethod(Class<?> clz, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(clz, null, method, param, value);
    }

    // String类型
    public static <T> T callMethod(String className, ClassLoader classLoader, Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(className, classLoader, instance, method, param, value);
    }

    public static <T> T callStaticMethod(String className, ClassLoader classLoader, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(className, classLoader, null, method, param, value);
    }

    // 无 ClassLoader
    public static <T> T callMethod(String className, Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(className, instance, method, param, value);
    }

    public static <T> T callStaticMethod(String className, String method, Class<?>[] param, Object... value) {
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

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeMethod(Class<?> clz, Object instance, String method, Class<?>[] param, Object... value) {
        Method declaredMethod;
        try {
            declaredMethod = methodCache.get(clz.getName() + "." + method + Arrays.toString(param));
            if (declaredMethod == null) {
                try {
                    declaredMethod = clz.getMethod(method, param);
                } catch (NoSuchMethodException e) {
                    try {
                        declaredMethod = clz.getDeclaredMethod(method, param);
                    } catch (NoSuchMethodException ex) {
                        throw new NoSuchMethodException("getMethod: " + e + " getDeclaredMethod: " + ex);
                    }
                }
                methodCache.put(clz.getName() + "." + declaredMethod.getName() + Arrays.toString(declaredMethod.getParameterTypes()), declaredMethod);
            }
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(instance, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            AndroidLogUtils.logE(TAG, "Reflection call method failed! class: " + clz.getName() + " method: " + method, e);
            return null;
        }
    }

    private static <T> T baseInvokeField(String className, Object instance, String field, boolean set, Object value) {
        return baseInvokeField(className, null, instance, field, set, value);
    }

    private static <T> T baseInvokeField(String className, ClassLoader classLoader, Object instance, String field, boolean set, Object value) {
        try {
            Class<?> clz = baseClass(classLoader, className);
            return baseInvokeField(clz, instance, field, set, value);
        } catch (ClassNotFoundException e) {
            AndroidLogUtils.logE(TAG, "Reflection call method failed! class: " + className + " field: " + field, e);
            return null;
        }
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeField(Class<?> clz, Object instance, String field, boolean set, Object value) {
        Field declaredField = null;
        try {
            declaredField = fieldCache.get(clz.getName() + "." + field);
            if (declaredField == null) {
                try {
                    declaredField = clz.getField(field);
                } catch (NoSuchFieldException e) {
                    try {
                        declaredField = clz.getDeclaredField(field);
                    } catch (NoSuchFieldException ex) {
                        throw new NoSuchFieldException("getField: " + e + " getDeclaredField: " + ex);
                    }
                }
                fieldCache.put(clz.getName() + "." + declaredField.getName(), declaredField);
            }
            declaredField.setAccessible(true);
            if (set) {
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
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                try {
                    return ClassLoader.getSystemClassLoader().loadClass(className);
                } catch (ClassNotFoundException f) {
                    throw new ClassNotFoundException("forName: " + e + " loadClass: " + f);
                }
            }
        }
        return classLoader.loadClass(className);
    }
}