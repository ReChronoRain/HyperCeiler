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
package com.sevtinge.hyperceiler.utils;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectUtils {

    public static boolean mCheckThrowException;
    private static Map<String, Method> sMethodCache = new HashMap();
    private static Map<String, Field> sFieldCache = new HashMap();
    private static Class<?>[] PRIMITIVE_CLASSES = {Boolean.TYPE, Byte.TYPE, Character.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Void.TYPE};
    private static String[] SIGNATURE_OF_PRIMTIVE_CLASSES = {"Z", "B", "C", "S", "I", "J", "F", "D", "V"};
    public static StringBuffer stringBuffer = new StringBuffer();

    private ReflectUtils() {
    }

    public static <T> T invokeObject(Class<?> cls, Object obj, String str, Class<?> cls2, Class<?>[] clsArr, Object... objArr) {
        if (clsArr == null) {
            try {
                clsArr = new Class[0];
            } catch (Exception e) {
                Log.e("ReflectUtils", "invokeObject", e);
                return null;
            }
        }
        Method method = getMethod(cls, str, getMethodSignature(cls2, clsArr), clsArr);
        if (method != null) {
            try {
                return (T) method.invoke(obj, objArr);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static <T> T invokeObject(Class<?> cls, Object obj, String str, Class<?> cls2, T t, Class<?>[] clsArr, Object... objArr) {
        if (clsArr == null) {
            try {
                clsArr = new Class[0];
            } catch (Exception e) {
                Log.e("ReflectUtils", "invokeObject", e);
            }
        }
        Method method = getMethod(cls, str, getMethodSignature(cls2, clsArr), clsArr);
        if (method != null) {
            try {
                return (T) method.invoke(obj, objArr);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return t;
    }

    public static Field getDeclaredField(Class<?> cls, String str, String str2) {
        try {
            String generateFieldCacheKey = generateFieldCacheKey(cls, str, str2);
            Field field = sFieldCache.get(generateFieldCacheKey);
            if (field == null) {
                Field declaredField = cls.getDeclaredField(str);
                declaredField.setAccessible(true);
                sFieldCache.put(generateFieldCacheKey, declaredField);
                return declaredField;
            }
            return field;
        } catch (Exception e) {
            Log.e("ReflectUtils", "getField", e);
            throwException(cls.getName(), str, e);
            return null;
        }
    }

    public static Method getMethod(Class<?> cls, String str, String str2, Class<?>... clsArr) {
        Method declaredMethod;
        try {
            String generateMethodCacheKey = generateMethodCacheKey(cls, str, str2);
            Method method = sMethodCache.get(generateMethodCacheKey);
            if (method == null) {
                try {
                    declaredMethod = cls.getMethod(str, clsArr);
                } catch (Exception unused) {
                    declaredMethod = cls.getDeclaredMethod(str, clsArr);
                }
                method = declaredMethod;
                sMethodCache.put(generateMethodCacheKey, method);
            }
            return method;
        } catch (Exception e) {
            Log.d("ReflectUtils", "getMethod", e);
            throwException(cls.getName(), str, e);
            return null;
        }
    }

    private static String generateMethodCacheKey(Class<?> cls, String str, String str2) {
        return cls.toString() + "/" + str + "/" + str2;
    }

    private static String generateFieldCacheKey(Class<?> cls, String str, String str2) {
        return cls.toString() + "/" + str + "/" + str2;
    }

    public static String getMethodSignature(Class<?> cls, Class<?>... clsArr) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        if (clsArr != null) {
            for (Class<?> cls2 : clsArr) {
                sb.append(getSignature(cls2));
            }
        }
        sb.append(')');
        sb.append(getSignature(cls));
        return sb.toString();
    }

    public static String getSignature(Class<?> cls) {
        int i = 0;
        while (true) {
            Class<?>[] clsArr = PRIMITIVE_CLASSES;
            if (i < clsArr.length) {
                if (cls == clsArr[i]) {
                    return SIGNATURE_OF_PRIMTIVE_CLASSES[i];
                }
                i++;
            } else {
                return getSignature(cls.getName());
            }
        }
    }

    public static String getSignature(String str) {
        int i = 0;
        while (true) {
            Class<?>[] clsArr = PRIMITIVE_CLASSES;
            if (i >= clsArr.length) {
                break;
            }
            if (clsArr[i].getName().equals(str)) {
                str = SIGNATURE_OF_PRIMTIVE_CLASSES[i];
            }
            i++;
        }
        String replace = str.replace(".", "/");
        if (replace.startsWith("[")) {
            return replace;
        }
        return "L" + replace + ";";
    }

    public static <T> T getDeclaredFieldValue(Object obj, Class<?> cls, String str, Class<?> cls2) {
        Field declaredField = getDeclaredField(cls, str, getSignature(cls2));
        if (declaredField != null) {
            try {
                return (T) declaredField.get(obj);
            } catch (IllegalAccessException unused) {
                return null;
            }
        }
        return null;
    }

    public static void throwException(String str, String str2, Exception exc) {
        if (mCheckThrowException) {
            String name = exc.getClass().getName();
            StringBuffer stringBuffer2 = stringBuffer;
            stringBuffer2.append(str + "---" + name + "---" + str2 + "\n");
        }
    }
}
