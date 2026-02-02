/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import org.jetbrains.annotations.NotNull;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.FieldDataList;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.base.BaseData;
import org.luckypray.dexkit.wrap.DexClass;
import org.luckypray.dexkit.wrap.DexField;
import org.luckypray.dexkit.wrap.DexMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

/**
 * DexKit 工具类
 *
 * @author 焕晨HChen
 * @co-author Ling Qiqi
 */
public class DexKit {
    private static final String TAG_DEFAULT = "DexKit";
    private static final int CACHE_VERSION = 7;
    private static final String DEXKIT_CACHE_DIR = "hyperceiler";
    private static final String DEXKIT_CACHE_FILE = "dexkit_cache.json";

    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";
    private static final Object sLock = new Object();

    private static volatile boolean sIsInit = false;
    private static volatile boolean sIsClosed = false;

    private static volatile String sTag = TAG_DEFAULT;
    private static volatile PackageLoadedParam sParam = null;
    private static volatile DexKitBridge sDexKitBridge = null;
    private static volatile Gson sGson = null;
    private static volatile File sCacheFile = null;
    private static volatile CacheData sCacheData = null;

    /**
     * 准备 DexKit 会话（必须在 initDexkitBridge 之前调用）
     */
    public static void ready(PackageLoadedParam param, String tag) {
        synchronized (sLock) {
            sParam = param;
            sTag = (tag != null && !tag.isEmpty()) ? tag : TAG_DEFAULT;
            sIsInit = false;
            sIsClosed = false;
            XposedLog.d(sTag, "DexKit ready for package: " + param.getPackageName());
        }
    }

    /**
     * 初始化并获取 DexKitBridge 实例
     */
    @NotNull
    public static DexKitBridge initDexkitBridge() {
        synchronized (sLock) {
            // 如果已经初始化且未关闭，直接返回
            if (sDexKitBridge != null && sIsInit && !sIsClosed) {
                return sDexKitBridge;
            }

            // 检查是否已关闭
            if (sIsClosed) {
                throw new IllegalStateException(sTag + ": DexKit already closed! Check your hook lifecycle.");
            }

            // 检查是否已准备
            if (sParam == null) {
                throw new IllegalStateException(sTag + ": DexKit not initialized! Override needDexKit() in your BaseLoad subclass and return true.");
            }

            ApplicationInfo appInfo = sParam.getApplicationInfo();
            String hostDir = appInfo.sourceDir;

            // 初始化 Gson
            sGson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

            // 初始化缓存
            initCache(appInfo);

            // 加载 DexKit 原生库并创建实例
            System.loadLibrary("dexkit");
            sDexKitBridge = DexKitBridge.create(hostDir);
            sIsInit = true;

            XposedLog.d(sTag, "DexKit initialized successfully");
            return sDexKitBridge;
        }
    }

    /**
     * 初始化缓存系统
     */
    private static void initCache(ApplicationInfo appInfo) {
        try {
            File cacheBaseDir = new File(appInfo.dataDir, "cache");
            File cacheDir = new File(cacheBaseDir, DEXKIT_CACHE_DIR);

            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                XposedLog.w(sTag, "Failed to create cache directory: " + cacheDir.getAbsolutePath());
            }

            sCacheFile = new File(cacheDir, DEXKIT_CACHE_FILE);

            // 加载缓存数据
            loadCacheData();
            // 验证并更新缓存
            validateAndUpdateCache();
        } catch (Throwable t) {
            XposedLog.e(sTag, "Failed to init cache", t);
            sCacheData = new CacheData();
        }
    }

    /**
     * 加载缓存数据
     */
    private static void loadCacheData() {
        if (sCacheData != null) return;

        if (sCacheFile != null && sCacheFile.exists()) {
            try (FileReader reader = new FileReader(sCacheFile)) {
                sCacheData = sGson.fromJson(reader, CacheData.class);
            } catch (Throwable t) {
                XposedLog.w(sTag, "Failed to load cache data", t);
            }
        }

        if (sCacheData == null) {
            sCacheData = new CacheData();
        }
    }

    /**
     * 验证并更新缓存
     */
    private static void validateAndUpdateCache() {
        if (sCacheData == null || sParam == null) return;

        boolean needClear = false;
        String pkgName = sParam.getPackageName();

        // 检查缓存版本
        if (sCacheData.version != CACHE_VERSION) {
            XposedLog.d(sTag, "Cache version changed: " + sCacheData.version + " -> " + CACHE_VERSION);
            needClear = true;
        }

        // 检查应用版本
        String pkgVersionName = getPackageVersionName();
        long pkgVersionCode = getPackageVersionCode();
        boolean hasPkgVersion = !"null".equals(pkgVersionName) && pkgVersionCode != -1;
        String pkgVersion = hasPkgVersion ? pkgVersionName + "(" + pkgVersionCode + ")" : null;

        if (hasPkgVersion && !Objects.equals(sCacheData.pkgVersion, pkgVersion)) {
            XposedLog.d(sTag, "App version changed: " + sCacheData.pkgVersion + " -> " + pkgVersion);
            needClear = true;
        }

        // SystemUI 额外检查系统版本
        boolean isSystemUI = "com.android.systemui".equals(pkgName);
        String osVersion = getSystemVersionIncremental();

        if (isSystemUI && !Objects.equals(sCacheData.osVersion, osVersion)) {
            XposedLog.d(sTag, "System version changed: " + sCacheData.osVersion + " -> " + osVersion);
            needClear = true;
        }

        // 清理并更新缓存
        if (needClear) {
            sCacheData.cache.clear();
        }

        sCacheData.version = CACHE_VERSION;
        if (hasPkgVersion) {
            sCacheData.pkgVersion = pkgVersion;
        }
        if (isSystemUI) {
            sCacheData.osVersion = osVersion;
        }

        saveCacheData();
    }

    /**
     * 保存缓存数据
     */
    private static void saveCacheData() {
        if (sCacheFile == null || sCacheData == null || sGson == null) return;

        try {
            File cacheDir = sCacheFile.getParentFile();
            if (cacheDir != null && !cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    XposedLog.w(sTag, "Failed to create cache directory: " + cacheDir.getAbsolutePath());
                    return;
                }
            }

            if (!sCacheFile.exists()) {
                if (!sCacheFile.createNewFile()) {
                    XposedLog.w(sTag, "Failed to create cache file: " + sCacheFile.getAbsolutePath());
                    return;
                }
            }

            try (
                FileChannel channel = FileChannel.open(
                    sCacheFile.toPath(),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
                );
                FileLock ignore = channel.lock()
            ) {
                try (FileWriter writer = new FileWriter(sCacheFile)) {
                    writer.write(sGson.toJson(sCacheData));
                    writer.flush();
                }
            }
        } catch (Throwable t) {
            XposedLog.w(sTag, "Failed to save cache data", t);
        }
    }

    // ======================== 查找方法 ========================

    public static <T> T findMember(@NonNull String key, IDexKit iDexKit) {
        synchronized (sLock) {
            if (sParam == null) {
                throw new IllegalStateException("DexKit not ready");
            }
            return findMember(key, sParam.getClassLoader(), iDexKit);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T findMember(@NonNull String key, ClassLoader classLoader, IDexKit iDexKit) {
        DexKitBridge dexKitBridge = initDexkitBridge();

        synchronized (sLock) {
            MemberData cachedData = sCacheData.cache.get(key);

            if (cachedData == null) {
                try {
                    BaseData baseData = iDexKit.dexkit(dexKitBridge);
                    if (baseData instanceof FieldData fieldData) {
                        String serialized = fieldData.toDexField().serialize();
                        safePutMember(key, new MemberData(TYPE_FIELD, serialized));
                        return (T) fieldData.getFieldInstance(classLoader);
                    } else if (baseData instanceof MethodData methodData) {
                        String serialized = methodData.toDexMethod().serialize();
                        safePutMember(key, new MemberData(TYPE_METHOD, serialized));
                        return (T) methodData.getMethodInstance(classLoader);
                    } else if (baseData instanceof ClassData classData) {
                        String serialized = classData.toDexType().serialize();
                        safePutMember(key, new MemberData(TYPE_CLASS, serialized));
                        return (T) classData.getInstance(classLoader);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    String serialized = cachedData.data.isEmpty() ? null : cachedData.data.getFirst();
                    if (serialized != null) {
                        switch (cachedData.type) {
                            case TYPE_METHOD:
                                return (T) new DexMethod(serialized).getMethodInstance(classLoader);
                            case TYPE_FIELD:
                                return (T) new DexField(serialized).getFieldInstance(classLoader);
                            case TYPE_CLASS:
                                return (T) new DexClass(serialized).getInstance(classLoader);
                            default:
                                XposedLog.w(sTag, "Unknown member data type: " + cachedData.type);
                        }
                    }
                } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public static <T> List<T> findMemberList(@NonNull String key, IDexKitList iDexKitList) {
        synchronized (sLock) {
            if (sParam == null) {
                throw new IllegalStateException("DexKit not ready");
            }
            return findMemberList(key, sParam.getClassLoader(), iDexKitList);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> findMemberList(@NonNull String key, ClassLoader classLoader, IDexKitList iDexKitList) {
        DexKitBridge dexKitBridge = initDexkitBridge();

        synchronized (sLock) {
            MemberData cachedData = sCacheData.cache.get(key);

            if (cachedData == null) {
                try {
                    BaseDataList<?> baseDataList = iDexKitList.dexkit(dexKitBridge);
                    ArrayList<String> serializeList = new ArrayList<>();
                    ArrayList<T> instanceList = new ArrayList<>();

                    if (baseDataList instanceof FieldDataList fieldDataList) {
                        for (FieldData f : fieldDataList) {
                            serializeList.add(f.toDexField().serialize());
                            instanceList.add((T) f.getFieldInstance(classLoader));
                        }
                        safePutMember(key, new MemberData(TYPE_FIELD, serializeList));
                        return instanceList;
                    } else if (baseDataList instanceof MethodDataList methodDataList) {
                        for (MethodData m : methodDataList) {
                            serializeList.add(m.toDexMethod().serialize());
                            instanceList.add((T) m.getMethodInstance(classLoader));
                        }
                        safePutMember(key, new MemberData(TYPE_METHOD, serializeList));
                        return instanceList;
                    } else if (baseDataList instanceof ClassDataList classDataList) {
                        for (ClassData c : classDataList) {
                            serializeList.add(c.toDexType().serialize());
                            instanceList.add((T) c.getInstance(classLoader));
                        }
                        safePutMember(key, new MemberData(TYPE_CLASS, serializeList));
                        return instanceList;
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ArrayList<T> instanceList = new ArrayList<>();
                try {
                    switch (cachedData.type) {
                        case TYPE_METHOD:
                            for (String s : cachedData.data)
                                instanceList.add((T) new DexMethod(s).getMethodInstance(classLoader));
                            return instanceList;
                        case TYPE_FIELD:
                            for (String s : cachedData.data)
                                instanceList.add((T) new DexField(s).getFieldInstance(classLoader));
                            return instanceList;
                        case TYPE_CLASS:
                            for (String s : cachedData.data)
                                instanceList.add((T) new DexClass(s).getInstance(classLoader));
                            return instanceList;
                        default:
                            XposedLog.w(sTag, "Unknown member data type: " + cachedData.type);
                    }
                } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new ArrayList<>();
    }

    private static void safePutMember(@NonNull String key, @NonNull MemberData data) {
        if (sCacheData == null) return;
        try {
            sCacheData.cache.put(key, data);
            saveCacheData();
        } catch (Throwable t) {
            XposedLog.w(sTag, "Failed to write dexkit cache for key=" + key, t);
        }
    }

    // ======================== 辅助方法 ========================

    private static String getSystemVersionIncremental() {
        return Build.VERSION.INCREMENTAL;
    }

    private static String getPackageVersionName() {
        return (sParam != null) ? AppsTool.getPackageVersionName(sParam) : "null";
    }

    private static long getPackageVersionCode() {
        return (sParam != null) ? AppsTool.getPackageVersionCode(sParam) : -1;
    }

    // ======================== 清理方法 ========================

    /**
     * 删除所有缓存
     */
    public static void deleteAllCache(Context context) {
        if (context == null) return;

        String[] folderNames = getScopeList();
        for (String folderName : folderNames) {
            try {
                File baseDir = new File(context.getFilesDir().getParent(), "../" + folderName + "/cache/" + DEXKIT_CACHE_DIR);
                deleteDirectoryRecursively(baseDir);
            } catch (Throwable t) {
                XposedLog.w(TAG_DEFAULT, "Failed to delete cache for " + folderName, t);
            }
        }
    }

    private static void deleteDirectoryRecursively(File dir) {
        if (dir == null || !dir.exists()) return;

        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectoryRecursively(file);
                }
            }
        }
        dir.delete();
    }

    /**
     * 关闭 DexKit 会话
     */
    public static void close() {
        synchronized (sLock) {
            if (!sIsInit) return;

            if (sDexKitBridge != null) {
                try {
                    sDexKitBridge.close();
                } catch (Throwable t) {
                    XposedLog.w(sTag, "Error closing DexKitBridge", t);
                }
                sDexKitBridge = null;
            }

            sParam = null;
            sGson = null;
            sCacheFile = null;
            sCacheData = null;
            sIsInit = false;
            sIsClosed = true;

            XposedLog.d(sTag, "DexKit closed");
        }
    }

    // ======================== 内部类 ========================

    private static final class CacheData {
        public int version = CACHE_VERSION;
        public String pkgVersion;
        public String osVersion;
        public Map<String, MemberData> cache = new HashMap<>();
    }

    private static final class MemberData {
        public String type;
        public List<String> data = new ArrayList<>();

        public MemberData(String type, String serialize) {
            this.type = type;
            this.data.add(serialize);
        }

        public MemberData(String type, ArrayList<String> serializeList) {
            this.type = type;
            this.data = new ArrayList<>(serializeList);
        }

        @NonNull
        @Override
        public String toString() {
            return "MemberData{type='" + type + "', data=" + data + "}";
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MemberData other)) return false;
            return Objects.equals(type, other.type) && Objects.equals(data, other.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, data);
        }
    }

    private static String[] getScopeList() {
        List<String> scopeList = new ArrayList<>();
        ClassLoader classLoader = DexKit.class.getClassLoader();
        if (classLoader == null) {
            return new String[0];
        }

        try (InputStream is = classLoader.getResourceAsStream("META-INF/xposed/scope.list")) {
            if (is == null) {
                XposedLog.w(TAG_DEFAULT, "scope.list not found");
                return new String[0];
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        scopeList.add(line);
                    }
                }
            }
        } catch (IOException e) {
            XposedLog.e(TAG_DEFAULT, "Failed to read scope.list", e);
        }
        return scopeList.toArray(new String[0]);
    }
}
