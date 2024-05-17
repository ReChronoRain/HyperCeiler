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
package com.sevtinge.hyperceiler.module.base.dexkit;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.utils.FileUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DexKit {
    public boolean isInit = false;
    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FILED";
    private static final String TYPE_CONSTRUCTOR = "CONSTRUCTOR";
    private final String TAG;
    private static String callTAG;
    private String hostDir = null;
    private XC_LoadPackage.LoadPackageParam loadPackageParam;
    private static DexKit dexKit = null;
    private static DexKitBridge privateDexKitBridge = null;

    public DexKit(XC_LoadPackage.LoadPackageParam param, String tag) {
        loadPackageParam = param;
        TAG = tag;
        dexKit = this;
    }

    private void init() {
        if (privateDexKitBridge == null) {
            if (hostDir == null) {
                if (loadPackageParam == null) {
                    throw new RuntimeException(TAG != null ? TAG : "InitDexKit" + ": lpparam is null");
                }
                hostDir = loadPackageParam.appInfo.sourceDir;
            }
            System.loadLibrary("dexkit");
            privateDexKitBridge = DexKitBridge.create(hostDir);
        }
        isInit = true;
    }

    @NotNull
    public static DexKitBridge getDexKitBridge() {
        if (privateDexKitBridge == null) {
            if (dexKit == null) {
                throw new RuntimeException("InitDexKit is null!!");
            } else {
                // new DexKitCache(dexKit.loadPackageParam).create();
                dexKit.init();
            }
        }
        return privateDexKitBridge;
    }

    public static AnnotatedElement getDexKitBridge(String tag, IDexKit iDexKit) {
        return getDexKitBridge(tag, iDexKit, null).get(0);
    }

    public static ArrayList<AnnotatedElement> getDexKitBridge(String tag, IDexKitList iDexKitList) {
        return getDexKitBridge(tag, null, iDexKitList);
    }

    private static ArrayList<AnnotatedElement> getDexKitBridge(String tag, IDexKit iDexKit, IDexKitList iDexKitList) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callName = getCallName(stackTrace);
        callTAG = callName;
        DexKitBridge dexKitBridge = getDexKitBridge();
        String dexPath = dexKit.loadPackageParam.appInfo.dataDir + "/cache/dexkit/";
        String dexFile = dexKit.loadPackageParam.appInfo.dataDir + "/cache/dexkit/" + callName;
        // XposedLogUtils.logE(callTAG, "path: " + dexPath + " file: " + dexFile + " cll: " + stackTrace[2].getClassName());
        ClassLoader classLoader = dexKit.loadPackageParam.classLoader;
        if (!FileUtils.exists(dexFile)) {
            if (FileUtils.mkdirs(dexPath) && FileUtils.touch(dexFile)) {
                FileUtils.write(dexFile, new JSONArray().toString());
            } else {
                return getElement(iDexKit, iDexKitList, dexKitBridge);
            }
        }
        if (!FileUtils.exists(dexFile)) {
            XposedLogUtils.logE(callTAG, "failed to create file!");
            return getElement(iDexKit, iDexKitList, dexKitBridge);
        }
        ArrayList<JSONObject> dadaList = DexKitData.toArray(FileUtils.read(dexFile));
        boolean isAdded = isAdded(tag, dadaList);
        if (!isAdded) {
            return getElements(tag, iDexKit, iDexKitList, dexKitBridge, dadaList, dexFile);
        } else {
            return getFileCache(tag, iDexKit, iDexKitList, dadaList, dexKitBridge, classLoader);
        }
    }

    private static String getCallName(StackTraceElement[] stackTrace) {
        String callName = "";
        for (StackTraceElement stackTraceElement : stackTrace) {
            callName = stackTraceElement.getClassName();
            if (callName.contains("com.sevtinge.hyperceiler.module.hook.")) {
                break;
            }
        }
        String[] calls = callName.split("\\.");
        if (calls.length != 0) {
            callName = calls[calls.length - 1];
        } else
            throw new RuntimeException("calls length is 0! stack: " + stackTrace[2].getClassName());
        return callName;
    }

    private static ArrayList<AnnotatedElement> getFileCache(String tag, IDexKit iDexKit, IDexKitList iDexKitList,
                                                            ArrayList<JSONObject> dadaList, DexKitBridge dexKitBridge, ClassLoader classLoader) {
        ArrayList<JSONObject> findJSONs = new ArrayList<>();
        for (JSONObject object : dadaList) {
            String mTag = DexKitData.getTAG(object);
            if (mTag.equals(tag)) {
                findJSONs.add(object);
            }
        }
        if (findJSONs.isEmpty()) {
            return getElement(iDexKit, iDexKitList, dexKitBridge);
        }
        ArrayList<AnnotatedElement> getElement = new ArrayList<>();
        for (JSONObject object : findJSONs) {
            String type = DexKitData.getType(object);
            switch (type) {
                case TYPE_CLASS -> {
                    String clzName = DexKitData.getClazz(object);
                    getElement.add(getClass(clzName, classLoader));
                }
                case TYPE_METHOD -> {
                    String clzName = DexKitData.getClazz(object);
                    String method = DexKitData.getMethod(object);
                    ArrayList<String> paramList = DexKitData.getParam(object);
                    Class<?> clz = getClass(clzName, classLoader);
                    Class<?>[] paramArray = stringToClassArray(paramList, classLoader);
                    try {
                        Method getMethod = clz.getDeclaredMethod(method, paramArray);
                        getElement.add(getMethod);
                    } catch (NoSuchMethodException e) {
                        throwRuntime(e.toString());
                    }
                }
                case TYPE_FIELD -> {
                    String clzName = DexKitData.getClazz(object);
                    String field = DexKitData.getFiled(object);
                    Class<?> clz = getClass(clzName, classLoader);
                    try {
                        Field getField = clz.getDeclaredField(field);
                        getElement.add(getField);
                    } catch (NoSuchFieldException e) {
                        throwRuntime(e.toString());
                    }
                }
                case TYPE_CONSTRUCTOR -> {
                    String clzName = DexKitData.getClazz(object);
                    ArrayList<String> paramList = DexKitData.getParam(object);
                    Class<?> clz = getClass(clzName, classLoader);
                    Class<?>[] paramArray = stringToClassArray(paramList, classLoader);
                    try {
                        Constructor<?> constructor = clz.getConstructor(paramArray);
                        getElement.add(constructor);
                    } catch (NoSuchMethodException e) {
                        throwRuntime(e.toString());
                    }
                }
            }
        }
        return getElement;
    }

    @Nullable
    private static ArrayList<AnnotatedElement> getElements(String tag, IDexKit iDexKit, IDexKitList iDexKitList,
                                                           DexKitBridge dexKitBridge, ArrayList<JSONObject> dadaList, String dexFile) {
        ArrayList<JSONObject> jsonList = new ArrayList<>();
        ArrayList<AnnotatedElement> elements = new ArrayList<>();
        elements = getElement(iDexKit, iDexKitList, dexKitBridge);
        // if (dadaList.isEmpty()) return elements;
        if (elements == null) return null;
        if (elements.isEmpty()) return null;
        for (AnnotatedElement element : elements) {
            if (element instanceof Method method) {
                String methodName = method.getName();
                String clName = method.getDeclaringClass().getName();
                Class<?>[] param = method.getParameterTypes();
                ArrayList<String> paramList = new ArrayList<>();
                for (Class<?> p : param) {
                    paramList.add(p.getName());
                }
                JSONObject data = new DexKitData(tag, TYPE_METHOD, clName, methodName, paramList, DexKitData.EMPTY).toJSON();
                jsonList.add(data);
            } else if (element instanceof Field field) {
                String fieldName = field.getName();
                String clName = field.getDeclaringClass().getName();
                JSONObject data = new DexKitData(tag, TYPE_FIELD, clName, DexKitData.EMPTY,
                        DexKitData.EMPTYLIST, fieldName).toJSON();
                jsonList.add(data);
            } else if (element instanceof Constructor<?> constructor) {
                String clzName = constructor.getName();
                Class<?>[] param = constructor.getParameterTypes();
                ArrayList<String> paramList = new ArrayList<>();
                for (Class<?> p : param) {
                    paramList.add(p.getName());
                }
                JSONObject data = new DexKitData(tag, TYPE_CONSTRUCTOR, clzName,
                        DexKitData.EMPTY, paramList, DexKitData.EMPTY).toJSON();
                jsonList.add(data);
            } else if (element instanceof Class<?> c) {
                String clzName = c.getName();
                JSONObject data = new DexKitData(tag, TYPE_CLASS, clzName,
                        DexKitData.EMPTY, DexKitData.EMPTYLIST, DexKitData.EMPTY).toJSON();
                jsonList.add(data);
            }
        }
        dadaList.addAll(jsonList);
        FileUtils.write(dexFile, dadaList.toString());
        return elements;
    }

    @NotNull
    private static Class<?> getClass(String name, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throwRuntime(e.toString());
            return null;
        }
    }

    private static void throwRuntime(String msg) {
        throw new RuntimeException("failed: " + msg);
    }

    private static Class<?>[] stringToClassArray(ArrayList<String> arrayList, ClassLoader classLoader) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        if (arrayList.isEmpty()) return new Class<?>[]{};
        if (arrayList.get(0).isEmpty()) return new Class<?>[]{};
        for (String s : arrayList) {
            classes.add(getClass(s, classLoader));
        }
        return classes.toArray(new Class<?>[classes.size()]);
    }

    private static boolean isAdded(String tag, ArrayList<JSONObject> dadaList) {
        boolean isAdded = false;
        for (JSONObject object : dadaList) {
            String mTag = DexKitData.getTAG(object);
            if (tag.equals(mTag)) {
                isAdded = true;
                break;
            }
        }
        return isAdded;
    }

    private static ArrayList<AnnotatedElement> getElement(IDexKit iDexKit, IDexKitList iDexKitList, DexKitBridge dexKitBridge) {
        ArrayList<AnnotatedElement> memberList = new ArrayList<>();
        if (iDexKit != null) {
            try {
                memberList.add(iDexKit.dexkit(dexKitBridge));
            } catch (ReflectiveOperationException e) {
                throwRuntime(e.toString());
            }
        } else if (iDexKitList != null) {
            try {
                memberList = iDexKitList.dexkit(dexKitBridge);
            } catch (ReflectiveOperationException e) {
                throwRuntime(e.toString());
            }
        }
        return memberList;
    }

    public static ArrayList<AnnotatedElement> toElementList(BaseDataList<?> baseDataList, ClassLoader classLoader) {
        ArrayList<AnnotatedElement> elements = new ArrayList<>();
        for (Object baseData : baseDataList) {
            if (baseData instanceof MethodData) {
                try {
                    elements.add(((MethodData) baseData).getMethodInstance(classLoader));
                } catch (NoSuchMethodException e) {
                    XposedLogUtils.logE(callTAG, e.toString());
                }
            } else if (baseData instanceof FieldData) {
                try {
                    elements.add(((FieldData) baseData).getFieldInstance(classLoader));
                } catch (NoSuchFieldException e) {
                    XposedLogUtils.logE(callTAG, e.toString());
                }
            } else if (baseData instanceof ClassData) {
                try {
                    elements.add(((ClassData) baseData).getInstance(classLoader));
                } catch (ClassNotFoundException e) {
                    XposedLogUtils.logE(callTAG, e.toString());
                }
            }
        }
        return elements;
    }

    /**
     * 请勿手动调用。
     */
    public void close() {
        if (privateDexKitBridge != null) {
            privateDexKitBridge.close();
            privateDexKitBridge = null;
        }
        loadPackageParam = null;
        dexKit = null;
        hostDir = null;
        isInit = false;
    }
}
