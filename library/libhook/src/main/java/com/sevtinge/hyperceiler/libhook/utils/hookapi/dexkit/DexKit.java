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
    private static String TAG = "DexKit";
    private static volatile boolean isInit = false;
    private static final int mVersion = 7;
    private static final String DEXKIT_CACHE_FILE = "/files/hyperceiler/dexkit_cache.json";
    private static PackageLoadedParam mParam;
    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";

    private static volatile Gson mGson = null;
    private static volatile DexKitBridge mDexKitBridge = null;
    private static volatile File mCacheFile = null;
    private static volatile CacheData mCacheData = null;

    public static void ready(PackageLoadedParam param, String tag) {
        mParam = param;
        TAG = tag;
        isInit = false;
    }

    @NotNull
    public static synchronized DexKitBridge initDexkitBridge() {
        if (mDexKitBridge != null)
            return mDexKitBridge;
        if (isInit)
            throw new IllegalStateException(TAG + ": DexKit already closed! Check your hook lifecycle.");
        if (mParam == null)
            throw new IllegalStateException(TAG + ": DexKit not initialized! Override needDexKit() in your BaseLoad subclass and return true.");

        ApplicationInfo appInfo = mParam.getApplicationInfo();
        String hostDir = appInfo.sourceDir;
        String cacheFilePath = appInfo.dataDir + DEXKIT_CACHE_FILE;
        mGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        // 初始化缓存文件
        try {
            mCacheFile = new File(cacheFilePath);
            File cacheDir = mCacheFile.getParentFile();
            if (cacheDir != null && !cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            // 读取或创建缓存
            loadCacheData();

            // 检查阶段
            String osVersion = getSystemVersionIncremental();
            String pkgVersionName = getPackageVersionName();
            long pkgVersionCode = getPackageVersionCode();

            boolean needClear = false;
            boolean hasPkgVersion = !"null".equals(pkgVersionName) && pkgVersionCode != -1;
            String pkgVersion = hasPkgVersion ? pkgVersionName + "(" + pkgVersionCode + ")" : null;

            if (mCacheData.version != mVersion) {
                XposedLog.d(TAG, "DexKit version changed, clear all cache: " + mCacheData.version + " -> " + mVersion);
                needClear = true;
            }

            // 检查应用版本
            if (hasPkgVersion) {
                String oldPkgVersion = mCacheData.pkgVersion;
                if (oldPkgVersion == null || !oldPkgVersion.contains(pkgVersion)) {
                    XposedLog.d(TAG, "App version changed, clear all cache: " + oldPkgVersion + " -> " + pkgVersion);
                    needClear = true;
                }
            }

            // 对于 systemui 单独检查系统版本
            boolean isSystemUI = "com.android.systemui".equals(mParam.getPackageName());
            if (isSystemUI) {
                String oldOSVersion = mCacheData.osVersion;
                if (oldOSVersion == null || !oldOSVersion.contains(osVersion)) {
                    XposedLog.d(TAG, "System version changed, clear all cache: " + oldOSVersion + " -> " + osVersion);
                    needClear = true;
                }
            }

            // 如果任一检测触发，统一清理一次
            if (needClear) {
                mCacheData.cache.clear();
            }

            // 保证必要键存在并写入最新值
            mCacheData.version = mVersion;
            if (hasPkgVersion) {
                mCacheData.pkgVersion = pkgVersion;
            }
            if (isSystemUI) {
                mCacheData.osVersion = osVersion;
            }

            saveCacheData();
        } catch (Throwable t) {
            XposedLog.e(TAG, "Failed to init cache: ", t);
        }

        // 启动 DexKit
        System.loadLibrary("dexkit");
        mDexKitBridge = DexKitBridge.create(hostDir);
        isInit = true;

        return mDexKitBridge;
    }

    // ======================== 辅助方法 ========================

    private static String getSystemVersionIncremental() {
        return Build.VERSION.INCREMENTAL;
    }

    private static String getPackageVersionName() {
        if (mParam == null) return "null";
        return AppsTool.getPackageVersionName(mParam);
    }

    private static long getPackageVersionCode() {
        if (mParam == null) return -1;
        return AppsTool.getPackageVersionCode(mParam);
    }

    private static void loadCacheData() {
        if (mCacheData != null) return;

        try {
            if (mCacheFile != null && mCacheFile.exists()) {
                try (FileReader reader = new FileReader(mCacheFile)) {
                    mCacheData = mGson.fromJson(reader, CacheData.class);
                }
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, "Failed to load cache data: ", t);
        }

        if (mCacheData == null) {
            mCacheData = new CacheData();
        }
    }

    private static void saveCacheData() {
        if (mCacheFile == null || mCacheData == null) return;

        try {
            File cacheDir = mCacheFile.getParentFile();
            if (cacheDir != null && !cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            // 使用 FileLock 进行多进程安全读写
            try (FileChannel channel = FileChannel.open(mCacheFile.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                FileLock lock = channel.lock();
                try (FileWriter writer = new FileWriter(mCacheFile)) {
                    writer.write(mGson.toJson(mCacheData));
                    writer.flush();
                } finally {
                    lock.release();
                }
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, "Failed to save cache data: ", t);
        }
    }

    public static <T> T findMember(@NonNull String key, IDexKit iDexKit) {
        return findMember(key, mParam.getClassLoader(), iDexKit);
    }

    @SuppressWarnings("unchecked")
    public static <T> T findMember(@NonNull String key, ClassLoader classLoader, IDexKit iDexKit) {
        DexKitBridge dexKitBridge = initDexkitBridge();
        MemberData cachedData = mCacheData.cache.get(key);
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
                            XposedLog.w(TAG, "Unknown member data type: " + cachedData.type);
                    }
                }
            } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static <T> List<T> findMemberList(@NonNull String key, IDexKitList iDexKitList) {
        return findMemberList(key, mParam.getClassLoader(), iDexKitList);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> findMemberList(@NonNull String key, ClassLoader classLoader, IDexKitList iDexKitList) {
        DexKitBridge dexKitBridge = initDexkitBridge();
        MemberData cachedData = mCacheData.cache.get(key);
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
                        for (String s : cachedData.data) instanceList.add((T) new DexMethod(s).getMethodInstance(classLoader));
                        return instanceList;
                    case TYPE_FIELD:
                        for (String s : cachedData.data) instanceList.add((T) new DexField(s).getFieldInstance(classLoader));
                        return instanceList;
                    case TYPE_CLASS:
                        for (String s : cachedData.data) instanceList.add((T) new DexClass(s).getInstance(classLoader));
                        return instanceList;
                    default:
                        XposedLog.w(TAG, "Unknown member data type: " + cachedData.type);
                }
            } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return new ArrayList<>();
    }

    private static void safePutMember(@NonNull String key, @NonNull MemberData data) {
        if (mCacheData == null) return;
        try {
            mCacheData.cache.put(key, data);
            saveCacheData();
        } catch (Throwable t) {
            XposedLog.w(TAG, "Failed to write dexkit cache for key=" + key + ": " + t.getMessage(), t);
        }
    }

    public static void deleteAllCache(Context context) {
        String[] folderNames = getScopeList();
        for (String folderName : folderNames) {
            try {
                String folderPath = context.getFilesDir().getParent() + "/../" + folderName + "/files/hyperceiler";
                deleteDirectory(new File(folderPath));
            } catch (Throwable t) {
                XposedLog.w(TAG, "Failed to delete cache for " + folderName + ": " + t.getMessage(), t);
            }
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    public static synchronized void close() {
        if (!isInit) return;

        if (mDexKitBridge != null) {
            mDexKitBridge.close();
            mDexKitBridge = null;
        }
        mParam = null;
        mGson = null;
        isInit = false;
    }


    private static final class CacheData {
        public int version = 7;
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
            return "Type: " + type + ", Data: " + data;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof MemberData memberData
                && Objects.equals(memberData.type, type)
                && Objects.equals(memberData.data, data);
        }
    }

    private static String[] getScopeList() {
        List<String> scopeList = new ArrayList<>();
        try (InputStream is = DexKit.class.getClassLoader().getResourceAsStream("META-INF/xposed/scope.list");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    scopeList.add(line);
                }
            }
        } catch (IOException | NullPointerException e) {
            XposedLog.e(TAG, "Failed to read scope.list: " + e.getMessage(), e);
        }
        return scopeList.toArray(new String[0]);
    }
}
