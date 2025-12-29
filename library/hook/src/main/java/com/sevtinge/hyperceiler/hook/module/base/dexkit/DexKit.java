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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.base.dexkit;

import static com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.getPackageVersionCode;
import static com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.getPackageVersionName;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sevtinge.hyperceiler.hook.R;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;

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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author 焕晨HChen
 * @co-author Ling Qiqi
 */
public class DexKit {
    private static String TAG = "DexKit";
    private static volatile boolean isInit = false;
    private static final int mVersion = 7;
    private static final String DEXKIT_CACHE_FILE = "/files/hyperceiler/dexkit_cache.json";
    private static XC_LoadPackage.LoadPackageParam mParam;
    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";

    private static volatile Gson mGson = null;
    private static volatile DexKitBridge mDexKitBridge = null;
    private static volatile File mCacheFile = null;
    private static volatile CacheData mCacheData = null;

    public static void ready(XC_LoadPackage.LoadPackageParam param, String tag) {
        mParam = param;
        TAG = tag;
        isInit = false;
    }

    @NotNull
    public static synchronized DexKitBridge initDexkitBridge() {
        if (mDexKitBridge != null)
            return mDexKitBridge;
        if (isInit)
            throw new RuntimeException(TAG + ": mDexKitBridge is null!");
        if (mParam == null)
            throw new RuntimeException(TAG + ": lpparam is null!");

        String hostDir = mParam.appInfo.sourceDir;
        String cacheFilePath = mParam.appInfo.dataDir + DEXKIT_CACHE_FILE;
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
            String pkgVersionName = getPackageVersionName(mParam);
            int pkgVersionCode = getPackageVersionCode(mParam);

            boolean needClear = false;
            boolean hasPkgVersion = !Objects.equals(pkgVersionName, "null") && pkgVersionCode != -1;
            String pkgVersion = hasPkgVersion ? pkgVersionName + "(" + pkgVersionCode + ")" : null;

            if (mCacheData.version != mVersion) {
                XposedLogUtils.logD(TAG, "DexKit version changed, clear all cache: " + mCacheData.version + " -> " + mVersion);
                needClear = true;
            }

            // 检查应用版本
            if (hasPkgVersion) {
                String oldPkgVersion = mCacheData.pkgVersion;
                if (oldPkgVersion == null || !oldPkgVersion.contains(pkgVersion)) {
                    XposedLogUtils.logD(TAG, "App version changed, clear all cache: " + oldPkgVersion + " -> " + pkgVersion);
                    needClear = true;
                }
            }

            // 对于 systemui 单独检查系统版本
            boolean isSystemUI = mParam.packageName.equals("com.android.systemui");
            if (isSystemUI) {
                String oldOSVersion = mCacheData.osVersion;
                if (oldOSVersion == null || !oldOSVersion.contains(osVersion)) {
                    XposedLogUtils.logD(TAG, "System version changed, clear all cache: " + oldOSVersion + " -> " + osVersion);
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
            XposedLogUtils.logE(TAG, "Failed to init cache: ", t);
        }

        // 启动 DexKit
        System.loadLibrary("dexkit");
        mDexKitBridge = DexKitBridge.create(hostDir);
        isInit = true;

        return mDexKitBridge;
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
            XposedLogUtils.logW(TAG, "Failed to load cache data: ", t);
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
            XposedLogUtils.logW(TAG, "Failed to save cache data: ", t);
        }
    }

    /**
     * 虽然泛型对 kt 不甚友好，但是已经是最好的方法了。
     */
    public static <T> T findMember(@NonNull String key, IDexKit iDexKit) {
        return findMember(key, mParam.classLoader, iDexKit);
    }

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
                String serialized = cachedData.data.isEmpty() ? null : cachedData.data.get(0);
                if (serialized != null) {
                    switch (cachedData.type) {
                        case TYPE_METHOD:
                            return (T) new DexMethod(serialized).getMethodInstance(classLoader);
                        case TYPE_FIELD:
                            return (T) new DexField(serialized).getFieldInstance(classLoader);
                        case TYPE_CLASS:
                            return (T) new DexClass(serialized).getInstance(classLoader);
                        default:
                            XposedLogUtils.logW(TAG, "Unknown member data type: " + cachedData.type);
                    }
                }
            } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static <T> List<T> findMemberList(@NonNull String key, IDexKitList iDexKitList) {
        return findMemberList(key, mParam.classLoader, iDexKitList);
    }

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
                        XposedLogUtils.logW(TAG, "Unknown member data type: " + cachedData.type);
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
            XposedLogUtils.logW(TAG, "Failed to write dexkit cache for key=" + key + ": " + t.getMessage(), t);
        }
    }

    public static void deleteAllCache(Context context) {
        String[] folderNames = context.getResources().getStringArray(R.array.xposed_scope);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            for (String folderName : folderNames) {
                try {
                    String folderPath = "/data/data/" + folderName + "/files/hyperceiler";
                    rootExecCmd("rm -f " + folderPath);
                    folderPath = "/data/user_de/0/" + folderName + "/files/hyperceiler";
                    rootExecCmd("rm -f " + folderPath);
                } catch (Throwable t) {
                    XposedLogUtils.logW(TAG, "Failed to delete cache for " + folderName + ": " + t.getMessage(), t);
                }
            }
        });
    }

    /**
     * 请勿手动调用。
     */
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
}
