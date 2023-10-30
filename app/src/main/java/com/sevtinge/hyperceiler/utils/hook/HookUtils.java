package com.sevtinge.hyperceiler.utils.hook;

import static com.sevtinge.hyperceiler.utils.log.AndroidLogUtils.LogI;

import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookUtils extends XposedLogUtils {

    public XC_LoadPackage.LoadPackageParam lpparam;

    public void setLoadPackageParam(XC_LoadPackage.LoadPackageParam param) {
    }

    public Class<?> findClass(String className) {
        return findClass(className, lpparam.classLoader);
    }

    public Class<?> findClass(String className, ClassLoader classLoader) {
        return XposedHelpers.findClass(className, classLoader);
    }

    public Class<?> findClassIfExists(String className) {
        try {
            return findClass(className);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE("findClassIfExists", "find " + className + " is Null: " + e);
            return null;
        }
    }

    public Class<?> findClassIfExists(String newClassName, String oldClassName) {
        try {
            return findClass(findClassIfExists(newClassName) != null ? newClassName : oldClassName);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE("findClassIfExists", "find " + newClassName + " and " + oldClassName + " is Null: " + e);
            return null;
        }
    }

    public Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try {
            return findClass(className, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE("findClassIfExists", "find " + className + " is Null: " + e);
            return null;
        }
    }

    public static class MethodHook extends XC_MethodHook {

        protected void before(MethodHookParam param) throws Throwable {
        }

        protected void after(MethodHookParam param) throws Throwable {
        }

        public MethodHook() {
            super();
        }

        public MethodHook(int priority) {
            super(priority);
        }

        public static MethodHook returnConstant(final Object result) {
            return new MethodHook(PRIORITY_DEFAULT) {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(result);
                }
            };
        }

        public static final MethodHook DO_NOTHING = new MethodHook(PRIORITY_HIGHEST * 2) {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(null);
            }
        };

        @Override
        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.before(param);
            } catch (Throwable t) {
                logE("BeforeHook", t);
            }
        }

        @Override
        public void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.after(param);
            } catch (Throwable t) {
                logE("AfterHook", t);
            }
        }
    }

    public void hookMethod(Method method, MethodHook callback) {
        try {
            XposedBridge.hookMethod(method, callback);
        } catch (Throwable t) {
            logE("hookMethod", "Failed to hook " + method.getName() + " method");
        }
    }

    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    public void findAndHookMethod(String className, String methodName, Object... parameterTypesAndCallback) {
        findAndHookMethod(findClassIfExists(className), methodName, parameterTypesAndCallback);
    }

    public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            logE("findAndHookMethod", "Failed to hook " + methodName + " method in " + className);
        }
    }

    public XC_MethodHook.Unhook findAndHookMethodUseUnhook(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            logE("findAndHookMethodUseUnhook", "Failed to hook " + methodName + " method in " + className);
            return null;
        }
    }

    public XC_MethodHook.Unhook findAndHookMethodUseUnhook(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            logE("findAndHookMethodUseUnhook", "Failed to hook " + methodName + " method in " + clazz.getCanonicalName());
            return null;
        }
    }

    public boolean findAndHookMethodSilently(String className, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(className, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            logE("findAndHookMethodSilently", className + methodName + " is null: " + t);
            return false;
        }
    }

    public boolean findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            logE("findAndHookMethodSilently", className + methodName + " is null: " + t);
            return false;
        }
    }

    public boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            logE("findAndHookMethodSilently", clazz + methodName + " is null: " + t);
            return false;
        }
    }

    public void findAndHookConstructor(String className, Object... parameterTypesAndCallback) {
        findAndHookConstructor(findClassIfExists(className), parameterTypesAndCallback);
    }

    public void findAndHookConstructor(Class<?> hookClass, Object... parameterTypesAndCallback) {
        XposedHelpers.findAndHookConstructor(hookClass, parameterTypesAndCallback);
    }

    public void findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
        } catch (Throwable t) {
            LogI("findAndHookConstructor", "Failed to hook constructor in " + className + " Error: " + t);
        }
    }

    public void hookAllMethods(String className, String methodName, MethodHook callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback);
            }
        } catch (Throwable t) {
            logE("HookAllMethods", className + " is " + methodName + " abnormal: " + t);
        }
    }

    public static void hookAllMethods(Class<?> hookClass, String methodName, MethodHook callback) {
        try {
            XposedBridge.hookAllMethods(hookClass, methodName, callback);
        } catch (Throwable t) {
            logE("HookAllMethods", hookClass + " is " + methodName + " abnormal: " + t);
        }
    }

    public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback);
            }
        } catch (Throwable t) {
            logE("hookAllMethods", className + " is abnormal", t);
        }
    }

    public void hookAllMethodsSilently(String className, String methodName, MethodHook callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback);
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean hookAllMethodsSilently(Class<?> hookClass, String methodName, MethodHook callback) {
        try {
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback);
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public void hookAllConstructors(String className, MethodHook callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass != null) {
                XposedBridge.hookAllConstructors(hookClass, callback);
            }
        } catch (Throwable t) {
            logE("hookAllConstructors", className + " is  abnormal: " + t);
        }
    }

    public void hookAllConstructors(Class<?> hookClass, MethodHook callback) {
        try {
            XposedBridge.hookAllConstructors(hookClass, callback);
        } catch (Throwable t) {
            logE("hookAllConstructors", hookClass + " is  abnormal: " + t);
        }
    }

    public void hookAllConstructors(String className, ClassLoader classLoader, MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass != null) {
                XposedBridge.hookAllConstructors(hookClass, callback);
            }
        } catch (Throwable t) {
            logE("hookAllConstructors", className + " is abnormal", t);
        }
    }

    public Object getStaticObjectFieldSilently(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (Throwable t) {
            return null;
        }
    }

    public Object proxySystemProperties(String method, String prop, int val, ClassLoader classLoader) {
        return XposedHelpers.callStaticMethod(findClassIfExists("android.os.SystemProperties", classLoader),
            method, prop, val);
    }

    public void setDeclaredField(XC_MethodHook.MethodHookParam param, String iNeedString, Object iNeedTo) {
        if (param != null) {
            try {
                Field setString = param.thisObject.getClass().getDeclaredField(iNeedString);
                setString.setAccessible(true);
                try {
                    setString.set(param.thisObject, iNeedTo);
                    Object result = setString.get(param.thisObject);
                    checkLast("getDeclaredField", iNeedString, iNeedTo, result);
                } catch (IllegalAccessException e) {
                    logE("IllegalAccessException to: " + iNeedString + " need to: " + iNeedTo + " code: " + e);
                }
            } catch (NoSuchFieldException e) {
                logE("No such the: " + iNeedString + " code: " + e);
            }
        } else {
            logE("Param is null Field: " + iNeedString + " to: " + iNeedTo);
        }
    }

    public void checkLast(String setObject, Object fieldName, Object value, Object last) {
        if (value.equals(last)) {
            logI(setObject + " Success! set " + fieldName + " to " + value);
        } else {
            logE(setObject + " Failed! set " + fieldName + " to " + value + " hope: " + value + " but: " + last);
        }
    }
}
