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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sevtinge.hyperceiler.utils.FileUtils;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DexKit {
    public boolean isInit = false;
    private final String version = "2";
    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";
    // 不关我事 (逃 ε=ε=ε=┏(゜ロ゜;)┛
    private static final String TYPE_FILED = "FILED";
    private static final String TYPE_CONSTRUCTOR = "CONSTRUCTOR";
    private final String TAG;
    private static String callTAG;
    private static Gson gson;
    private static final HashMap<String, Boolean> touchMap = new HashMap<>();
    private static final HashMap<String, ArrayList<DexKitData>> cacheMap = new HashMap<>();
    private List<AnnotatedElement> elementList = new ArrayList<>();
    private static final String DEXKIT_PATH = "/cache/dexkit/";
    private String hostDir = null;
    private String dexPath = null;
    private static boolean isMkdir = false;
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
                dexPath = loadPackageParam.appInfo.dataDir + DEXKIT_PATH;
            }
            gson = new GsonBuilder().setPrettyPrinting().create();
            System.loadLibrary("dexkit");
            privateDexKitBridge = DexKitBridge.create(hostDir);
            versionCheck();
        }
        isInit = true;
    }

    /**
     * 检查当前软件版本是否发生变化。
     */
    private void versionCheck() {
        String versionName = Helpers.getPackageVersionName(loadPackageParam);
        int versionCode = Helpers.getPackageVersionCode(loadPackageParam);
        // String dexFile = dir + callTAG;
        String nameFile = getFile("versionName");
        String codeFile = getFile("versionCode");
        String dexkitVersion = getFile("cacheVersion");
        if (!FileUtils.mkdirs(dexPath)) {
            isMkdir = false;
            XposedLogUtils.logE(callTAG, "failed to create mkdirs: " + dexPath + " cant use dexkit cache!!");
        } else isMkdir = true;
        if (isMkdir) {
            if (!FileUtils.touch(dexkitVersion))
                XposedLogUtils.logE(callTAG, "failed to create file: " + dexkitVersion);
            String deVer = FileUtils.read(dexkitVersion);
            if (!version.equals(deVer)) {
                clearCache(dexPath);
                FileUtils.write(dexkitVersion, version);
            }
            if (!FileUtils.touch(nameFile))
                XposedLogUtils.logE(callTAG, "failed to create file: " + nameFile);
            if (!FileUtils.touch(codeFile))
                XposedLogUtils.logE(callTAG, "failed to create file: " + codeFile);
            String verName = FileUtils.read(nameFile);
            String codeName = FileUtils.read(codeFile);
            if (verName != null && codeName != null) {
                if ("null".equals(versionName) && versionCode == -1) return;
                if (verName.isEmpty() || codeName.isEmpty()) {
                    FileUtils.write(nameFile, versionName);
                    FileUtils.write(codeFile, Integer.toString(versionCode));
                } else if (!(verName.equals(versionName)) || (!codeName.equals(Integer.toString(versionCode)))) {
                    // FileUtils.write(dexFile, new JSONArray().toString());
                    clearCache(dexPath);
                    FileUtils.write(nameFile, versionName);
                    FileUtils.write(codeName, String.valueOf(versionCode));
                }
            }
        }
    }

    public static void clearCache() {
        clearCache(getFile(callTAG));
    }

    public static void clearCache(@NonNull String path) {
        FileUtils.delete(path, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String s = file.toString();
                if (s.contains("versionName") || s.contains("versionCode")
                        || s.contains("cacheVersion")
                ) return FileVisitResult.CONTINUE;
                Files.delete(file);
                XposedLogUtils.logI(callTAG, "success delete file: " + file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

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

    /**
     * 不检查缓存，每次执行搜索。
     */
    @Nullable
    public static BaseDataList<?> useDexKitIfNoCache(String[] tags, IDexKitData iDexKitData) {
        return useDexKitIfNoCache(tags, false, iDexKitData);
    }

    /**
     * 当结果被缓存时返回 null，否则执行搜索。
     */
    @Nullable
    public static BaseDataList<?> useDexKitIfNoCache(String[] tags, boolean noCache, IDexKitData iDexKitData) {
        if (noCache) iDexKitData.dexkit(getDexKitBridge());
        callTAG = getCallName(Thread.currentThread().getStackTrace());
        String dexFile = getFile(callTAG);
        ArrayList<DexKitData> data = getCacheMapOrReadFile(dexFile);
        // XposedLogUtils.logE(callTAG, "data: " + data);
        // 非常不严谨，按你需求改吧。
        boolean have = false;
        for (String tag : tags) {
            if (isAdded(tag, data)) {
                have = true;
                break;
            }
        }
        if (!have) {
            return iDexKitData.dexkit(getDexKitBridge());
        }
        return null;
    }

    /**
     * 不写入缓存，直接返回 baseDataList 内结果。
     */
    public static DexKit createCache(String tag, List<?> baseDataList, ClassLoader classLoader) {
        return createCache(tag, false, baseDataList, classLoader);
    }

    /**
     * 创建缓存，创建成功后将返回缓存内结果。
     */
    public static DexKit createCache(String tag, boolean noCache, List<?> baseDataList, ClassLoader classLoader) {
        dexKit.elementList = getDexKitBridge(tag, noCache, null, null, toElementList(baseDataList, classLoader));
        return dexKit;
    }

    /**
     * 获取一个结果的时候使用，并且设置 debug 模式，可以使结果不被写入缓存。
     */
    public static AnnotatedElement getDexKitBridge(String tag, boolean noCache, IDexKit iDexKit) {
        return getDexKitBridge(tag, noCache, iDexKit, null, null).get(0);
    }

    /**
     * 获取一个结果的时候使用。
     */
    public static AnnotatedElement getDexKitBridge(String tag, IDexKit iDexKit) {
        return getDexKitBridge(tag, false, iDexKit, null, null).get(0);
    }

    /**
     * 获取列表型结果时使用,并且设置 debug 模式，可以使结果不被写入缓存。。<br/>
     * 配合
     * {@link DexKit#toClassList()}，
     * {@link DexKit#toMethodList()}，
     * {@link DexKit#toFieldList()}，
     * {@link DexKit#toConstructorList()}
     * 使用
     */
    public static DexKit getDexKitBridgeList(String tag, boolean noCache, IDexKitList iDexKitList) {
        dexKit.elementList = getDexKitBridge(tag, noCache, null, iDexKitList, null);
        return dexKit;
    }

    /**
     * 获取列表型结果时使用。<br/>
     * 配合
     * {@link DexKit#toClassList()}，
     * {@link DexKit#toMethodList()}，
     * {@link DexKit#toFieldList()}，
     * {@link DexKit#toConstructorList()}
     * 使用
     */
    public static DexKit getDexKitBridgeList(String tag, IDexKitList iDexKitList) {
        dexKit.elementList = getDexKitBridge(tag, false, null, iDexKitList, null);
        return dexKit;
    }

    // 主体代码
    private static List<AnnotatedElement> getDexKitBridge(@NotNull String tag, boolean noCache, IDexKit iDexKit,
                                                          IDexKitList iDexKitList, List<AnnotatedElement> list) {
        String callName = getCallName(Thread.currentThread().getStackTrace());
        callTAG = callName;
        DexKitBridge dexKitBridge = getDexKitBridge();
        String dexFile = getFile(callName);
        // XposedLogUtils.logE(callTAG, "path: " + dexPath + " file: " + dexFile + " cll: " + stackTrace[2].getClassName());
        ClassLoader classLoader = dexKit.loadPackageParam.classLoader;
        if (noCache) {
            return getElements(iDexKit, iDexKitList, dexKitBridge);
        }
        if (!isMkdir) {
            return getElements(iDexKit, iDexKitList, dexKitBridge);
        }
        if (!touchIfNeed(dexFile))
            return getElements(iDexKit, iDexKitList, dexKitBridge);
        return run(tag, iDexKit, iDexKitList, dexFile, dexKitBridge, classLoader, list);
    }

    // 执行缓存或读取
    private static List<AnnotatedElement> run(@NonNull String tag, IDexKit iDexKit, IDexKitList iDexKitList,
                                              String dexFile, DexKitBridge dexKitBridge, ClassLoader classLoader, List<AnnotatedElement> list) {
        ArrayList<DexKitData> cacheData = getCacheMapOrReadFile(dexFile);
        boolean isAdded = isAdded(tag, cacheData);
        if (!isAdded) {
            return getElementsAndWriteCache(tag, iDexKit, iDexKitList, dexKitBridge, cacheData, dexFile, list);
        } else {
            return getFileCache(tag, iDexKit, iDexKitList, cacheData, dexKitBridge, classLoader, list);
        }
    }

    private static ArrayList<DexKitData> getCacheMapOrReadFile(String dexFile) {
        ArrayList<DexKitData> cacheData = cacheMap.get(dexFile);
        if (cacheData == null) {
            Type type = new TypeToken<ArrayList<DexKitData>>() {
            }.getType();
            ArrayList<DexKitData> dataList = gson.fromJson(FileUtils.read(dexFile), type);
            cacheMap.put(dexFile, dataList);
            cacheData = dataList;
        }
        return cacheData;
    }

    private static boolean touchIfNeed(String dexFile) {
        Boolean isTouch = touchMap.get(dexFile);
        if (!Boolean.TRUE.equals(isTouch)) {
            if (!FileUtils.exists(dexFile)) {
                if (FileUtils.touch(dexFile)) {
                    if (FileUtils.write(dexFile, new JSONArray().toString()))
                        touchMap.put(dexFile, true);
                    else touchMap.put(dexFile, true);
                } else {
                    XposedLogUtils.logE(callTAG, "failed to create file!");
                    touchMap.put(dexFile, false);
                    return false;
                }
            }
        }
        return true;
    }

    @NonNull
    public static String getFile(String fileName) {
        return dexKit.dexPath + fileName;
    }

    /**
     * 删除指定路径缓存内指定标签的数据。
     */
    public static boolean removeData(@NotNull String tag, @NotNull String filePath) {
        try {
            ArrayList<DexKitData> dataList = getCacheMapOrReadFile(filePath);
            dataList.removeIf(dexKitData -> tag.equals(dexKitData.tag));
            FileUtils.write(filePath, gson.toJson(dataList));
            return true;
        } catch (Throwable e) {
            XposedLogUtils.logE(callTAG, e);
        }
        return false;
    }

    /**
     * 删除指定路径缓存内指定标签的数据。
     */
    public static boolean removeData(@NotNull String tag) {
        return removeData(tag, getFile(callTAG));
    }

    private static String getCallName(StackTraceElement[] stackTrace) {
        for (StackTraceElement stackTraceElement : stackTrace) {
            String callName = stackTraceElement.getClassName();
            if (callName.contains("com.sevtinge.hyperceiler.module.hook.")) {
                String last = callName.substring(callName.lastIndexOf('.') + 1);
                if (last.contains("$")) {
                    continue;
                }
                return last;
            }
        }
        throw new RuntimeException("Invalid call stack");
    }

    // 获取结果并写入缓存
    private static List<AnnotatedElement> getElementsAndWriteCache(String tag, IDexKit iDexKit, IDexKitList iDexKitList,
                                                                   DexKitBridge dexKitBridge, ArrayList<DexKitData> dadaList, String dexFile,
                                                                   List<AnnotatedElement> list) {
        ArrayList<AnnotatedElement> elements;
        if (iDexKit != null || iDexKitList != null) {
            elements = new ArrayList<>(getElements(iDexKit, iDexKitList, dexKitBridge));
        } else {
            elements = new ArrayList<>(list);
        }
        // if (dadaList.isEmpty()) return elements;
        dadaList.addAll(createJsonList(tag, elements));
        FileUtils.write(dexFile, gson.toJson(dadaList));
        cacheMap.put(dexFile, dadaList);
        return elements;
    }

    // 结果转为 JSON 储存
    private static ArrayList<DexKitData> createJsonList(String tag, ArrayList<AnnotatedElement> elements) {
        ArrayList<DexKitData> jsonList = new ArrayList<>();
        if (elements.isEmpty()) return new ArrayList<>();
        for (AnnotatedElement element : elements) {
            if (element instanceof Method method) {
                String methodName = method.getName();
                String clName = method.getDeclaringClass().getName();
                Class<?>[] param = method.getParameterTypes();
                ArrayList<String> paramList = new ArrayList<>();
                for (Class<?> p : param) {
                    paramList.add(p.getName());
                }
                DexKitData data = new DexKitData(tag, TYPE_METHOD, clName, methodName, paramList, DexKitData.EMPTY);
                jsonList.add(data);
            } else if (element instanceof Field field) {
                String fieldName = field.getName();
                String clName = field.getDeclaringClass().getName();
                DexKitData data = new DexKitData(tag, TYPE_FIELD, clName, DexKitData.EMPTY,
                        DexKitData.EMPTYLIST, fieldName);
                jsonList.add(data);
            } else if (element instanceof Constructor<?> constructor) {
                String clzName = constructor.getName();
                Class<?>[] param = constructor.getParameterTypes();
                ArrayList<String> paramList = new ArrayList<>();
                for (Class<?> p : param) {
                    paramList.add(p.getName());
                }
                DexKitData data = new DexKitData(tag, TYPE_CONSTRUCTOR, clzName,
                        DexKitData.EMPTY, paramList, DexKitData.EMPTY);
                jsonList.add(data);
            } else if (element instanceof Class<?> c) {
                String clzName = c.getName();
                DexKitData data = new DexKitData(tag, TYPE_CLASS, clzName,
                        DexKitData.EMPTY, DexKitData.EMPTYLIST, DexKitData.EMPTY);
                jsonList.add(data);
            }
        }
        return jsonList;
    }

    // 读取缓存
    private static List<AnnotatedElement> getFileCache(String tag, IDexKit iDexKit, IDexKitList iDexKitList,
                                                       ArrayList<DexKitData> dadaList, DexKitBridge dexKitBridge, ClassLoader classLoader,
                                                       List<AnnotatedElement> list) {
        ArrayList<AnnotatedElement> getElement = new ArrayList<>();
        for (DexKitData data : dadaList) {
            if (!tag.equals(data.tag)) {
                continue;
            }
            String type = data.type;
            switch (type) {
                case TYPE_CLASS -> {
                    String clzName = data.clazz;
                    getElement.add(getClass(clzName, classLoader));
                }
                case TYPE_METHOD -> {
                    String clzName = data.clazz;
                    String method = data.method;
                    ArrayList<String> paramList = data.param;
                    Class<?> clz = getClass(clzName, classLoader);
                    Class<?>[] paramArray = stringToClassArray(paramList, classLoader);
                    try {
                        Method getMethod = clz.getDeclaredMethod(method, paramArray);
                        getElement.add(getMethod);
                    } catch (NoSuchMethodException e) {
                        throwRuntime(e.toString());
                    }
                }
                case TYPE_FIELD, TYPE_FILED -> {
                    String clzName = data.clazz;
                    String field = data.field;
                    Class<?> clz = getClass(clzName, classLoader);
                    try {
                        Field getField = clz.getDeclaredField(field);
                        getElement.add(getField);
                    } catch (NoSuchFieldException e) {
                        throwRuntime(e.toString());
                    }
                }
                case TYPE_CONSTRUCTOR -> {
                    String clzName = data.clazz;
                    ArrayList<String> paramList = data.param;
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
        if (getElement.isEmpty()) {
            if (iDexKit != null || iDexKitList != null)
                return getElements(iDexKit, iDexKitList, dexKitBridge);
            else return list;
        }
        return getElement;
    }

    @NotNull
    private static Class<?> getClass(@Nullable String name, ClassLoader classLoader) {
        if (name == null) throwRuntime("str is null, cant get class!!");
        try {
            Class<?> c = null;
            String old = name;
            name = name.replace("[", "");
            int i = old.length() - name.length();
            if (i != 0) {
              c = switch (name.charAt(0)) {
                    case 'Z' -> boolean.class;
                    case 'B' -> byte.class;
                    case 'C' -> char.class;
                    case 'S' -> short.class;
                    case 'I' -> int.class;
                    case 'J' -> long.class;
                    case 'F' -> float.class;
                    case 'D' -> double.class;
                    default -> null;
                    };
              if (c != null) {
                return Array.newInstance(c, new int[i]).getClass();
              } else {
                name = name.replace(";", "").substring(1);
                return Array.newInstance(classLoader.loadClass(name), new int[i]).getClass();
              }
            }
            return switch (name.trim()) {
                case "int" -> int.class;
                case "boolean" -> boolean.class;
                case "byte" -> byte.class;
                case "short" -> short.class;
                case "long" -> long.class;
                case "float" -> float.class;
                case "double" -> double.class;
                case "char" -> char.class;
                case "void" -> void.class;
                default -> classLoader.loadClass(name);
            };
        } catch (ClassNotFoundException e) {
            throwRuntime(e.toString());
            return null;
        }
    }

    private static void throwRuntime(String msg) {
        throw new RuntimeException("failed: " + msg);
    }

    // 字符串获取为类列表
    private static Class<?>[] stringToClassArray(ArrayList<String> arrayList, ClassLoader classLoader) {
        if (arrayList.isEmpty()) return new Class<?>[]{};
        if (arrayList.get(0).isEmpty()) return new Class<?>[]{};
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (String s : arrayList) {
            classes.add(getClass(s, classLoader));
        }
        return classes.toArray(new Class<?>[classes.size()]);
    }

    // 是否已经存在缓存中
    private static boolean isAdded(String tag, ArrayList<DexKitData> dadaList) {
        for (DexKitData data : dadaList) {
            String mTag = data.tag;
            if (tag.equals(mTag)) {
                return true;
            }
        }
        return false;
    }

    // 获取接口结果
    private static List<AnnotatedElement> getElements(IDexKit iDexKit, IDexKitList iDexKitList, DexKitBridge dexKitBridge) {
        ArrayList<AnnotatedElement> memberList = new ArrayList<>();
        if (iDexKit != null) {
            try {
                memberList.add(iDexKit.dexkit(dexKitBridge));
            } catch (ReflectiveOperationException e) {
                throwRuntime(e.toString());
            }
        } else if (iDexKitList != null) {
            try {
                memberList.addAll(iDexKitList.dexkit(dexKitBridge));
            } catch (ReflectiveOperationException e) {
                throwRuntime(e.toString());
            }
        }
        return memberList;
    }

    /**
     * 将 BaseDataList<?> 转为 ArrayList<AnnotatedElement> 时使用，调用 IDexKitList 接口会用到。
     */
    public static List<AnnotatedElement> toElementList(List<?> baseDataList, ClassLoader classLoader) {
        if (baseDataList == null) return new ArrayList<>();
        ArrayList<AnnotatedElement> elements = new ArrayList<>(baseDataList.size());
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
     * 转为方法列表
     */
    public List<Method> toMethodList() {
        ArrayList<Method> methods = new ArrayList<>(elementList.size());
        for (AnnotatedElement element : elementList) {
            methods.add((Method) element);
        }
        elementList.clear();
        return methods;
    }

    /**
     * 转为字段列表
     */
    public List<Field> toFieldList() {
        ArrayList<Field> fields = new ArrayList<>(elementList.size());
        for (AnnotatedElement element : elementList) {
            fields.add((Field) element);
        }
        elementList.clear();
        return fields;
    }

    /**
     * 转为构造函数列表
     */
    public List<Constructor<?>> toConstructorList() {
        ArrayList<Constructor<?>> constructors = new ArrayList<>(elementList.size());
        for (AnnotatedElement element : elementList) {
            constructors.add((Constructor<?>) element);
        }
        elementList.clear();
        return constructors;
    }

    /**
     * 转为类列表
     */
    public List<Class<?>> toClassList() {
        ArrayList<Class<?>> classes = new ArrayList<>(elementList.size());
        for (AnnotatedElement element : elementList) {
            classes.add((Class<?>) element);
        }
        elementList.clear();
        return classes;
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
