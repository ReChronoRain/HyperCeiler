/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.base.tool;

import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsMap;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookTool extends XposedLogUtils {
    public static final PrefsMap<String, Object> mPrefsMap = PrefsUtils.mPrefsMap;
    private final String TAG = getClass().getSimpleName();

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
            // logE("findClassIfExists", "find " + className + " is Null: " + e);
            return null;
        }
    }

    public Class<?> findClassIfExists(String newClassName, String oldClassName) {
        try {
            return findClass(findClassIfExists(newClassName) != null ? newClassName : oldClassName);
        } catch (XposedHelpers.ClassNotFoundError e) {
            // logE("findClassIfExists", "find " + newClassName + " and " + oldClassName + " is Null: " + e);
            return null;
        }
    }

    public Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try {
            return findClass(className, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            // logE("findClassIfExists", "find " + className + " is Null: " + e);
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
                // logE("BeforeHook", t);
            }
        }

        @Override
        public void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.after(param);
            } catch (Throwable t) {
                // logE("AfterHook", t);
            }
        }
    }

    /*用于替换方法*/
    public abstract static class replaceHookedMethod extends MethodHook {

        public replaceHookedMethod() {
            super();
        }

        public replaceHookedMethod(int priority) {
            super(priority);
        }

        protected abstract Object replace(MethodHookParam param) throws Throwable;

        @Override
        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Object result = replace(param);
                param.setResult(result);
            } catch (Throwable t) {
                param.setThrowable(t);
            }
        }
    }

    public static void hookMethod(Method method, MethodHook callback) {
        XposedBridge.hookMethod(method, callback);
    }

    public static Object getObjectFieldSilently(Object obj, String fieldName) {
        try {
            return XposedHelpers.getObjectField(obj, fieldName);
        } catch (Throwable t) {
            return "ObjectFieldNotExist";
        }
    }

    public void safeHookMethod(Method method, MethodHook callback) {
        try {
            hookMethod(method, callback);
        } catch (Throwable e) {

        }
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    public void findAndHookMethod(String className, String methodName, Object... parameterTypesAndCallback) {
        findAndHookMethod(findClassIfExists(className), methodName, parameterTypesAndCallback);
    }

    public void safeFindAndHookMethod(String className, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(className, methodName, parameterTypesAndCallback);
        } catch (Throwable e) {
            logE(TAG, "safeHook: " + e);
        }
    }

    public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
    }

    public XC_MethodHook.Unhook findAndHookMethodUseUnhook(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            // logE("findAndHookMethodUseUnhook", "Failed to hook " + methodName + " method in " + className);
            return null;
        }
    }

    public XC_MethodHook.Unhook findAndHookMethodUseUnhook(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            // logE("findAndHookMethodUseUnhook", "Failed to hook " + methodName + " method in " + clazz.getCanonicalName());
            return null;
        }
    }

    public boolean findAndHookMethodSilently(String className, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(className, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            // logE("findAndHookMethodSilently", className + methodName + " is null: " + t);
            return false;
        }
    }

    public boolean findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            // logE("findAndHookMethodSilently", className + methodName + " is null: " + t);
            return false;
        }
    }

    public boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            // logE("findAndHookMethodSilently", clazz + methodName + " is null: " + t);
            return false;
        }
    }

    public void findAndHookConstructor(String className, Object... parameterTypesAndCallback) {
        findAndHookConstructor(findClassIfExists(className), parameterTypesAndCallback);
    }

    public void findAndHookConstructor(Class<?> hookClass, Object... parameterTypesAndCallback) {
        XposedHelpers.findAndHookConstructor(hookClass, parameterTypesAndCallback);
    }

    public static void findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        XposedHelpers.findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
    }

    public void hookAllMethods(String className, String methodName, MethodHook callback) {
        Class<?> hookClass = findClassIfExists(className);
        if (hookClass != null) {
            XposedBridge.hookAllMethods(hookClass, methodName, callback);
        }
    }

    public static void hookAllMethods(Class<?> hookClass, String methodName, MethodHook callback) {
        XposedBridge.hookAllMethods(hookClass, methodName, callback);
    }

    public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, MethodHook callback) {
        Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
        if (hookClass != null) {
            XposedBridge.hookAllMethods(hookClass, methodName, callback);
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

    public void hookAllMethodsSilently(Class<?> hookClass, String methodName, MethodHook callback) {
        try {
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback);
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean hookAllMethodsBoolean(String className, String methodName, MethodHook callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass != null) {
                return !XposedBridge.hookAllMethods(hookClass, methodName, callback).isEmpty();
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    public boolean hookAllMethodsBoolean(Class<?> hookClass, String methodName, MethodHook callback) {
        try {
            if (hookClass != null) {
                return !XposedBridge.hookAllMethods(hookClass, methodName, callback).isEmpty();
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public void hookAllConstructors(String className, MethodHook callback) {
        Class<?> hookClass = findClassIfExists(className);
        if (hookClass != null) {
            XposedBridge.hookAllConstructors(hookClass, callback);
        }
    }

    public void hookAllConstructors(Class<?> hookClass, MethodHook callback) {
        XposedBridge.hookAllConstructors(hookClass, callback);
    }

    public void hookAllConstructors(String className, ClassLoader classLoader, MethodHook callback) {
        Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
        if (hookClass != null) {
            XposedBridge.hookAllConstructors(hookClass, callback);
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

    public Method getDeclaredMethod(String className, String method, Object... type) throws NoSuchMethodException {
        return getDeclaredMethod(findClassIfExists(className), method, type);
    }

    public Method getDeclaredMethod(Class<?> clazz, String method, Object... type) throws NoSuchMethodException {
        String tag = "getDeclaredMethod";
        ArrayList<Method> haveMethod = new ArrayList<>();
        Method hqMethod = null;
        int methodNum;
        if (clazz == null) {
            logE(tag, "find class is null: " + method);
            throw new NoSuchMethodException("find class is null");
        }
        for (Method getMethod : clazz.getDeclaredMethods()) {
            if (getMethod.getName().equals(method)) {
                haveMethod.add(getMethod);
            }
        }
        if (haveMethod.isEmpty()) {
            logE(tag, "find method is null: " + method);
            throw new NoSuchMethodException("find method is null");
        }
        methodNum = haveMethod.size();
        if (type != null) {
            Class<?>[] classes = new Class<?>[type.length];
            Class<?> newclass = null;
            Object getType;
            for (int i = 0; i < type.length; i++) {
                getType = type[i];
                if (getType instanceof Class<?>) {
                    newclass = (Class<?>) getType;
                }
                if (getType instanceof String) {
                    newclass = findClassIfExists((String) getType);
                    if (newclass == null) {
                        logE(tag, "get class error: " + i);
                        throw new NoSuchMethodException("get class error");
                    }
                }
                classes[i] = newclass;
            }
            boolean noError = true;
            for (int i = 0; i < methodNum; i++) {
                hqMethod = haveMethod.get(i);
                boolean allHave = true;
                if (hqMethod.getParameterTypes().length != classes.length) {
                    if (methodNum - 1 == i) {
                        logE(tag, "class length bad: " + Arrays.toString(hqMethod.getParameterTypes()));
                        throw new NoSuchMethodException("class length bad");
                    } else {
                        noError = false;
                        continue;
                    }
                }
                for (int t = 0; t < hqMethod.getParameterTypes().length; t++) {
                    Class<?> getClass = hqMethod.getParameterTypes()[t];
                    if (!getClass.getSimpleName().equals(classes[t].getSimpleName())) {
                        allHave = false;
                        break;
                    }
                }
                if (!allHave) {
                    if (methodNum - 1 == i) {
                        logE(tag, "type bad: " + Arrays.toString(hqMethod.getParameterTypes())
                                + " input: " + Arrays.toString(classes));
                        throw new NoSuchMethodException("type bad");
                    } else {
                        noError = false;
                        continue;
                    }
                }
                if (noError) {
                    break;
                }
            }
            return hqMethod;
        } else {
            if (methodNum > 1) {
                logE(tag, "no type method must only have one: " + haveMethod);
                throw new NoSuchMethodException("no type method must only have one");
            }
        }
        return haveMethod.get(0);
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
                    // logE("IllegalAccessException to: " + iNeedString + " need to: " + iNeedTo + " code: " + e);
                }
            } catch (NoSuchFieldException e) {
                // logE("No such the: " + iNeedString + " code: " + e);
            }
        } else {
            // logE("Param is null Field: " + iNeedString + " to: " + iNeedTo);
        }
    }

    public void checkLast(String setObject, Object fieldName, Object value, Object last) {
        if (value.equals(last)) {
            logI(setObject + " Success! set " + fieldName + " to " + value);
        } else {
            // logE(setObject + " Failed! set " + fieldName + " to " + value + " hope: " + value + " but: " + last);
        }
    }
}
