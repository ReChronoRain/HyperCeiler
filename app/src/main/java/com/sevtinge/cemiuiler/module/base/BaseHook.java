package com.sevtinge.cemiuiler.module.base;

import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.utils.ResourcesHook;
import com.sevtinge.cemiuiler.utils.LogUtils;
import com.sevtinge.cemiuiler.utils.PrefsMap;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseHook {

    public String TAG = getClass().getSimpleName();

    public boolean detailLog = !mPrefsMap.getBoolean("settings_disable_detailed_log");

    public LoadPackageParam lpparam;
    public ResourcesHook mResHook = XposedInit.mResHook;
    public static PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;

    public static final String ACTION_PREFIX = "com.sevtinge.cemiuiler.module.action.";

    public abstract void init();

    public void onCreate(LoadPackageParam lpparam) {
        try {
            setLoadPackageParam(lpparam);
            init();
            if (!mPrefsMap.getBoolean("settings_disable_detailed_log"))
                printHookStateLog("Hook Success!");
        } catch (Throwable t) {
            printHookStateLog("Hook Failed!");
            printHookFailedLog(t);
        }
    }

    public void setLoadPackageParam(LoadPackageParam param) {
        lpparam = param;
    }

    private void printHookStateLog(String state) {
        LogUtils.log(TAG + " " + state);
    }

    private void printHookFailedLog(Throwable th) {
        LogUtils.log(TAG + " FailedInfo：" + th);
    }

    public void log(String log) {
        if (!mPrefsMap.getBoolean("settings_disable_detailed_log")) {
            XposedBridge.log("Cemiuiler: " + TAG + " " + log);
        }
    }

    public void logI(String log) {
        XposedBridge.log("Cemiuiler: " + TAG + " " + log);
    }

    public void logE(Exception e) {
        XposedBridge.log("Cemiuiler: " + TAG + " hook failed by: " + e);
    }

    public void logE(Throwable t) {
        XposedBridge.log("Cemiuiler: " + TAG + " hook failed by: " + t);
    }

    public void logE(String tag, Exception e) {
        XposedBridge.log("Cemiuiler: " + TAG + tag + " hook failed by: " + e);
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
            LogUtils.log("find " + className + " is Null", e);
            return null;
        }
    }

    public Class<?> findClassIfExists(String newClassName, String oldClassName) {
        try {
            return findClass(findClassIfExists(newClassName) != null ? newClassName : oldClassName);
        } catch (XposedHelpers.ClassNotFoundError e) {
            LogUtils.log("find " + newClassName + " and " + oldClassName + " is Null", e);
            return null;
        }
    }

    public Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try {
            return findClass(className, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            LogUtils.log("find " + className + " is Null", e);
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


        @Override
        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.before(param);
            } catch (Throwable t) {
                LogUtils.log(t);
            }
        }

        @Override
        public void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.after(param);
            } catch (Throwable t) {
                LogUtils.log(t);
            }
        }
    }


    public void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    public void findAndHookMethod(String className, String methodName, Object... parameterTypesAndCallback) {
        findAndHookMethod(findClassIfExists(className), methodName, parameterTypesAndCallback);
    }

    public boolean findAndHookMethodSilently(String className, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(className, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            LogUtils.log(t);
            return false;
        }
    }

    public boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            LogUtils.log(t);
            return false;
        }
    }

    public void findAndHookConstructor(String className, Object... parameterTypesAndCallback) {
        findAndHookConstructor(findClassIfExists(className), parameterTypesAndCallback);
    }

    public void findAndHookConstructor(Class<?> hookClass, Object... parameterTypesAndCallback) {
        XposedHelpers.findAndHookConstructor(hookClass, parameterTypesAndCallback);
    }

    public void hookMethod(Method method, MethodHook callback) {
        XposedBridge.hookMethod(method, callback);
    }

    public void hookAllMethods(String className, String methodName, XC_MethodHook callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback).size();
            }

        } catch (Throwable t) {
            LogUtils.log(t);
        }
    }

    public void hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        try {
            XposedBridge.hookAllMethods(hookClass, methodName, callback).size();
        } catch (Throwable t) {
            LogUtils.log(t);
        }
    }

    public void hookAllMethodsSilently(String className, String methodName, XC_MethodHook callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback).size();
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean hookAllMethodsSilently(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        try {
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback).size();
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
                XposedBridge.hookAllConstructors(hookClass, callback).size();
            }
        } catch (Throwable t) {
            LogUtils.log(t);
        }
    }

    public void hookAllConstructors(Class<?> hookClass, MethodHook callback) {
        try {
            XposedBridge.hookAllConstructors(hookClass, callback).size();
        } catch (Throwable t) {
            LogUtils.log(t);
        }
    }


    public Object getStaticObjectFieldSilently(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (Throwable t) {
            return null;
        }
    }
}
