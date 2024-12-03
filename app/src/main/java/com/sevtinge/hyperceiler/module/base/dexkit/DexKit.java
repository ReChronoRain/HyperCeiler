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

import static com.sevtinge.hyperceiler.utils.shell.ShellUtils.safeExecCommandWithRoot;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sevtinge.hyperceiler.R;
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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DexKit {
    public boolean isInit = false;
    // 更新版本可触发全部缓存重建，建议使用整数
    private static final String version = "4";
    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";
    private static final String TYPE_CONSTRUCTOR = "CONSTRUCTOR";
    private final String TAG;
    private static String callTAG;
    private static Gson gson;
    private static final HashMap<String, Boolean> touchMap = new HashMap<>();
    public static final HashMap<String, ArrayList<DexKitData>> cacheMap = new HashMap<>();
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
        if (privateDexKitBridge != null) {
            return;
        }

        if (loadPackageParam == null) {
            throw new RuntimeException(TAG != null ? TAG : "InitDexKit" + ": lpparam is null");
        }

        hostDir = loadPackageParam.appInfo.sourceDir;
        dexPath = loadPackageParam.appInfo.dataDir + DEXKIT_PATH;

        gson = new GsonBuilder().setPrettyPrinting().create();
        System.loadLibrary("dexkit");
        privateDexKitBridge = DexKitBridge.create(hostDir);
        versionCheck();
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

    /**
     * 检查当前软件版本是否发生变化。
     */
    private void versionCheck() {
        String versionName = Helpers.getPackageVersionName(loadPackageParam);
        String versionCode = Integer.toString(Helpers.getPackageVersionCode(loadPackageParam));
        String nameFile = getFile("versionName");
        String codeFile = getFile("versionCode");
        String dexkitVersion = getFile("cacheVersion");

        if (!FileUtils.mkdirs(dexPath)) {
            isMkdir = false;
            XposedLogUtils.logE(callTAG, "failed to create mkdirs: " + dexPath + " cant use dexkit cache!!");
        } else {
            isMkdir = true;
        }

        boolean isDexkitVersionTouched = FileUtils.touch(dexkitVersion);
        if (!isDexkitVersionTouched) {
            XposedLogUtils.logE(callTAG, "failed to create file: " + dexkitVersion);
            return;
        }

        String deVer = FileUtils.read(dexkitVersion);
        if (!version.equals(deVer)) {
            writeVersionFiles(nameFile, codeFile, versionName, versionCode);
            clearCache(dexPath);
            FileUtils.write(dexkitVersion, version);
        }

        boolean isNameFileTouched = FileUtils.touch(nameFile);
        boolean isCodeFileTouched = FileUtils.touch(codeFile);
        if (!isNameFileTouched || !isCodeFileTouched) {
            XposedLogUtils.logE(callTAG, "failed to create file: " + (isNameFileTouched ? codeFile : nameFile));
            return;
        }

        String verName = FileUtils.read(nameFile);
        String codeName = FileUtils.read(codeFile);
        if (needToUpdateVersionFiles(verName, codeName, versionName, versionCode)) {
            writeVersionFiles(nameFile, codeFile, versionName, versionCode);
            clearCache(dexPath);
        }
    }

    private void writeVersionFiles(String nameFile, String codeFile, String versionName, String versionCode) {
        FileUtils.write(nameFile, versionName);
        FileUtils.write(codeFile, versionCode);
    }

    private boolean needToUpdateVersionFiles(String verName, String codeName, String versionName, String versionCode) {
        return "null".equals(versionName) && "-1".equals(versionCode) ||
                (verName.isEmpty() || codeName.isEmpty()) ||
                !verName.equals(versionName) ||
                !codeName.equals(versionCode);
    }

    public static void clearCache() {
        clearCache(getFile(callTAG));
    }

    public static void clearCache(@NonNull String path) {
        FileUtils.delete(path, new FileVisitor<>() {
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

    @Nullable
    public static BaseDataList<?> useDexkitIfNoCache(String[] tags, IDexKitData iDexKitData) {
        callTAG = getCallName(Thread.currentThread().getStackTrace());
        if (getCacheMapOrReadFile(getFile(callTAG)).stream()
                .noneMatch(data -> Arrays.stream(tags)
                        .anyMatch(t -> t.equals(data.tag)))) {
            return iDexKitData.dexkit(getDexKitBridge());
        }
        return null;
    }

    /**
     * 获取一个结果的时候使用，并且设置 debug 模式，可以使结果不被写入缓存。
     */
    public static AnnotatedElement getDexKitBridge(String tag, boolean noCache, IDexKit iDexKit) {
        return getDexKitBridge(tag, noCache, iDexKit, null).get(0);
    }

    /**
     * 获取一个结果的时候使用。
     */
    public static AnnotatedElement getDexKitBridge(String tag, IDexKit iDexKit) {
        return getDexKitBridge(tag, false, iDexKit, null).get(0);
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
        dexKit.elementList = getDexKitBridge(tag, noCache, null, iDexKitList);
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
        dexKit.elementList = getDexKitBridge(tag, false, null, iDexKitList);
        return dexKit;
    }

    // 主体代码
    private static List<AnnotatedElement> getDexKitBridge(@NotNull String tag, boolean noCache, IDexKit iDexKit,
                                                          IDexKitList iDexKitList) {
        callTAG = getCallName(Thread.currentThread().getStackTrace());
        DexKitBridge dexKitBridge = getDexKitBridge();
        String dexFile = getFile(callTAG);

        if (noCache || !isMkdir || !touchIfNeed(dexFile)) {
            return getElement(dexKitBridge, iDexKit, iDexKitList);
        }

        return run(tag, dexFile, dexKitBridge, dexKit.loadPackageParam.classLoader, iDexKit, iDexKitList);
    }

    // 执行缓存或读取
    private static List<AnnotatedElement> run(@NonNull String tag, String dexFile, DexKitBridge dexKitBridge,
                                              ClassLoader classLoader, IDexKit iDexKit, IDexKitList iDexKitList) {
        ArrayList<DexKitData> cacheData = getCacheMapOrReadFile(dexFile);
        if (!haveCache(tag, cacheData)) {
            return getElementAndWriteCache(tag, iDexKit, iDexKitList, dexKitBridge, cacheData, dexFile);
        } else {
            return getFileCache(tag, classLoader, cacheData);
        }
    }

    // 获取结果并写入缓存
    private static List<AnnotatedElement> getElementAndWriteCache(String tag, IDexKit iDexKit, IDexKitList iDexKitList,
                                                                  DexKitBridge dexKitBridge, ArrayList<DexKitData> cacheDataList, String dexFile) {
        ArrayList<AnnotatedElement> elements = new ArrayList<>(getElement(dexKitBridge, iDexKit, iDexKitList));
        cacheDataList.addAll(createJsonList(tag, elements));
        FileUtils.write(dexFile, gson.toJson(cacheDataList));
        cacheMap.put(dexFile, cacheDataList);
        return elements;
    }

    // 结果转为 JSON 储存
    private static ArrayList<DexKitData> createJsonList(String tag, ArrayList<AnnotatedElement> elements) {
        ArrayList<DexKitData> jsonList = new ArrayList<>();
        if (elements.isEmpty()) return new ArrayList<>();
        for (AnnotatedElement element : elements) {
            if (element instanceof Method method) {
                jsonList.add(new DexKitData(tag, TYPE_METHOD, method.getDeclaringClass().getName(),
                        method.getName(), Arrays.stream(method.getParameterTypes())
                        .map(Class::getName)
                        .collect(Collectors.toCollection(ArrayList::new))
                        , DexKitData.EMPTY));
            } else if (element instanceof Field field) {
                jsonList.add(new DexKitData(tag, TYPE_FIELD, field.getDeclaringClass().getName(), DexKitData.EMPTY,
                        DexKitData.EMPTYLIST, field.getName()));
            } else if (element instanceof Constructor<?> constructor) {
                jsonList.add(new DexKitData(tag, TYPE_CONSTRUCTOR, constructor.getName(),
                        DexKitData.EMPTY, Arrays.stream(constructor.getParameterTypes())
                        .map(Class::getName)
                        .collect(Collectors.toCollection(ArrayList::new))
                        , DexKitData.EMPTY));
            } else if (element instanceof Class<?> c) {
                jsonList.add(new DexKitData(tag, TYPE_CLASS, c.getName(),
                        DexKitData.EMPTY, DexKitData.EMPTYLIST, DexKitData.EMPTY));
            }
        }
        return jsonList;
    }

    // 读取缓存
    private static List<AnnotatedElement> getFileCache(String tag, ClassLoader classLoader, ArrayList<DexKitData> dadaList) {
        ArrayList<AnnotatedElement> getElement = new ArrayList<>();
        for (DexKitData data : dadaList) {
            if (!tag.equals(data.tag)) {
                continue;
            }
            String type = data.type;
            try {
                switch (type) {
                    case TYPE_CLASS -> getElement.add(getClass(data.clazz, classLoader));
                    case TYPE_METHOD -> getElement.add(getClass(data.clazz, classLoader).getDeclaredMethod(data.method,
                            stringToClassArray(data.param, classLoader)));
                    case TYPE_FIELD -> getElement.add(getClass(data.clazz, classLoader).getDeclaredField(data.field));
                    case TYPE_CONSTRUCTOR -> getElement.add(getClass(data.clazz, classLoader).getConstructor(
                            stringToClassArray(data.param, classLoader)));
                }
            } catch (NoSuchFieldException | NoSuchMethodException e) {
                throwRuntime(e.toString());
            }
        }
        return getElement;
    }

    private static ArrayList<DexKitData> getCacheMapOrReadFile(String dexFile) {
        ArrayList<DexKitData> cacheData = cacheMap.get(dexFile);
        if (cacheData == null) {
            ArrayList<DexKitData> dataList = gson.fromJson(FileUtils.read(dexFile),
                    new TypeToken<ArrayList<DexKitData>>() {
                    }.getType());
            if (dataList == null)
                return new ArrayList<>();
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
                    FileUtils.write(dexFile, new JSONArray().toString());
                    touchMap.put(dexFile, true);
                } else {
                    XposedLogUtils.logE(callTAG, "failed to create file!");
                    touchMap.put(dexFile, false);
                    return false;
                }
            } else touchMap.put(dexFile, true);
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
        String call = Arrays.stream(stackTrace).map(StackTraceElement::getClassName)
                .filter(name -> name.contains("com.sevtinge.hyperceiler.module.hook."))
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .filter(find -> !find.contains("$"))
                .findFirst()
                .orElse(null);
        if (call == null) throwRuntime("Invalid call stack");
        return call;
    }

    @NotNull
    private static Class<?> getClass(@Nullable String name, ClassLoader classLoader) {
        if (name == null) throwRuntime("str is null, cant get class!!");
        try {
            Class<?> c;
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
        throw new RuntimeException("[HyperCeiler][E][" + callTAG + "]: " + msg);
    }

    // 字符串获取为类列表
    private static Class<?>[] stringToClassArray(ArrayList<String> arrayList, ClassLoader classLoader) {
        if (arrayList.isEmpty()) return new Class<?>[]{};
        if (arrayList.get(0).isEmpty()) return new Class<?>[]{};
        return arrayList.stream().map(s -> getClass(s, classLoader)).toArray(Class<?>[]::new);
    }

    // 是否已经存在缓存中
    private static boolean haveCache(String tag, ArrayList<DexKitData> dadaList) {
        return dadaList.stream().anyMatch(data -> tag.equals(data.tag));
    }

    // 获取接口结果
    private static List<AnnotatedElement> getElement(DexKitBridge dexKitBridge, IDexKit iDexKit, IDexKitList iDexKitList) {
        ArrayList<AnnotatedElement> memberList = new ArrayList<>();
        try {
            if (iDexKit != null) {
                memberList.add(iDexKit.dexkit(dexKitBridge));
            } else if (iDexKitList != null) {
                List<AnnotatedElement> list = iDexKitList.dexkit(dexKitBridge);
                if (list == null) return memberList;
                memberList.addAll(list);
            }
        } catch (ReflectiveOperationException e) {
            throwRuntime(e.toString());
        }
        return memberList;
    }

    /**
     * 将 BaseDataList<?> 转为 ArrayList<AnnotatedElement> 时使用，调用 IDexKitList 接口会用到。
     */
    public static List<AnnotatedElement> toElementList(List<?> baseDataList) {
        if (baseDataList == null) return new ArrayList<>();
        ArrayList<AnnotatedElement> elements = new ArrayList<>(baseDataList.size());
        for (Object baseData : baseDataList) {
            try {
                if (baseData instanceof MethodData) {
                    elements.add(((MethodData) baseData).getMethodInstance(dexKit.loadPackageParam.classLoader));
                } else if (baseData instanceof FieldData) {
                    elements.add(((FieldData) baseData).getFieldInstance(dexKit.loadPackageParam.classLoader));
                } else if (baseData instanceof ClassData) {
                    elements.add(((ClassData) baseData).getInstance(dexKit.loadPackageParam.classLoader));
                }
            } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
                XposedLogUtils.logE(callTAG, e.toString());
            }
        }
        return elements;
    }

    /**
     * 转为方法列表
     */
    public List<Method> toMethodList() {
        ArrayList<Method> list = elementList.stream().map(element -> (Method) element)
                .collect(Collectors.toCollection(ArrayList::new));
        elementList.clear();
        return list;
    }

    /**
     * 转为字段列表
     */
    public List<Field> toFieldList() {
        ArrayList<Field> fields = elementList.stream().map(elementList -> (Field) elementList)
                .collect(Collectors.toCollection(ArrayList::new));
        elementList.clear();
        return fields;
    }

    /**
     * 转为构造函数列表
     */
    public List<Constructor<?>> toConstructorList() {
        ArrayList<Constructor<?>> constructors = elementList.stream().map(elementList -> (Constructor<?>) elementList)
                .collect(Collectors.toCollection(ArrayList::new));
        elementList.clear();
        return constructors;
    }

    /**
     * 转为类列表
     */
    public List<Class<?>> toClassList() {
        ArrayList<Class<?>> classes = elementList.stream().map(elementList -> (Class<?>) elementList)
                .collect(Collectors.toCollection(ArrayList::new));
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

    public static void deleteAllCache(Context context) {
        String[] folderNames = context.getResources().getStringArray(R.array.xposed_scope);
        for (String folderName : folderNames) {
            String folderPath = "/data/data/" + folderName + "/cache";
            if (safeExecCommandWithRoot("ls " + folderPath).contains("dexkit")) {
                safeExecCommandWithRoot("rm -rf " + folderPath + "/dexkit");
            }
        }
    }
}
