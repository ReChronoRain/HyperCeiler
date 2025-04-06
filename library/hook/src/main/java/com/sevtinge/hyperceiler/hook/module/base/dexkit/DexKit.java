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
import com.google.gson.reflect.TypeToken;
import com.sevtinge.hyperceiler.hook.R;
import com.tencent.mmkv.MMKV;

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

import java.util.ArrayList;
import java.util.List;
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
    private static boolean isInit = false;
    private static final int mVersion = 3;
    private static final String MMKV_PATH = "/files/hyperceiler/mmkv";
    private static XC_LoadPackage.LoadPackageParam mParam;
    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";
    private static MMKV mMMKV = null;
    private static Gson mGson = null;
    private static DexKitBridge mDexKitBridge;

    public static void ready(XC_LoadPackage.LoadPackageParam param, String tag) {
        mParam = param;
        TAG = tag;
        isInit = false;
    }

    @NotNull
    public static DexKitBridge initDexkitBridge() {
        if (mDexKitBridge != null)
            return mDexKitBridge;
        if (isInit)
            throw new RuntimeException(TAG + ": mDexKitBridge is null!");
        if (mParam == null)
            throw new RuntimeException(TAG + ": lpparam is null!");

        String hostDir = mParam.appInfo.sourceDir,
                mmkvPath = mParam.appInfo.dataDir + MMKV_PATH;
        mGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        // 启动 MMKV
        MMKV.initialize(mmkvPath, System::loadLibrary);
        mMMKV = MMKV.mmkvWithID("dexkit_cache", MMKV.MULTI_PROCESS_MODE);

        // 检查阶段
        if (mMMKV.containsKey("version")) {
            int version = mMMKV.getInt("version", 0);
            if (version != mVersion) {
                mMMKV.clear();
                mMMKV.putInt("version", mVersion);
            }
        } else
            mMMKV.putInt("version", mVersion);

        String osVersion = getSystemVersionIncremental();
        String pkgVersion = getPackageVersionName(mParam) + "(" + getPackageVersionCode(mParam) + ")";
        if (mMMKV.containsKey("pkgVersion")) {
            String oldPkgVersion = mMMKV.getString("pkgVersion", "null");
            if (!Objects.equals(pkgVersion, oldPkgVersion)) {
                mMMKV.clearAll();
                mMMKV.putString("pkgVersion", pkgVersion + "\n");
            }
        } else
            mMMKV.putString("pkgVersion", pkgVersion + "\n");

        // 对于长时间恒定不变的应用版本号但内部有改动的简易处理方式
        if (mParam.packageName.equals("com.android.systemui")) {
            if (mMMKV.containsKey("osVersion")) {
                String oldOSVersion = mMMKV.getString("osVersion", "null");
                if (!Objects.equals(osVersion, oldOSVersion)) {
                    mMMKV.clearAll();
                    mMMKV.putString("osVersion", osVersion + "\n");
                }
            } else
                mMMKV.putString("osVersion", osVersion + "\n");
        }

        // 启动 DexKit
        System.loadLibrary("dexkit");
        mDexKitBridge = DexKitBridge.create(hostDir);
        isInit = true;

        return mDexKitBridge;
    }

    /**
     * 虽然泛型对 kt 不慎友好，但是已经是最好的方法了。
     */
    public static <T> T findMember(@NonNull String key, IDexKit iDexKit) {
        return findMember(key, mParam.classLoader, iDexKit);
    }

    public static <T> T findMember(@NonNull String key, ClassLoader classLoader, IDexKit iDexKit) {
        DexKitBridge dexKitBridge = initDexkitBridge();
        String descriptor = mMMKV.getString(key, "");
        if (descriptor.isEmpty()) {
            try {
                BaseData baseData = iDexKit.dexkit(dexKitBridge);
                if (baseData instanceof FieldData fieldData) {
                    mMMKV.putString(key, mGson.toJson(new MemberData(TYPE_FIELD, fieldData.toDexField().serialize())) + "\n\n");
                    return (T) fieldData.getFieldInstance(classLoader);
                } else if (baseData instanceof MethodData methodData) {
                    mMMKV.putString(key, mGson.toJson(new MemberData(TYPE_METHOD, methodData.toDexMethod().serialize())) + "\n\n");
                    return (T) methodData.getMethodInstance(classLoader);
                } else if (baseData instanceof ClassData classData) {
                    mMMKV.putString(key, mGson.toJson(new MemberData(TYPE_CLASS, classData.toDexType().serialize())) + "\n\n");
                    return (T) classData.getInstance(classLoader);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            MemberData data = mGson.fromJson(descriptor, new TypeToken<MemberData>() {
            }.getType());
            try {
                switch (data.type) {
                    case TYPE_METHOD -> {
                        return (T) new DexMethod(data.serialize).getMethodInstance(classLoader);
                    }
                    case TYPE_FIELD -> {
                        return (T) new DexField(data.serialize).getFieldInstance(classLoader);
                    }
                    case TYPE_CLASS -> {
                        return (T) new DexClass(data.serialize).getInstance(classLoader);
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
        String descriptor = mMMKV.getString(key, "");
        if (descriptor.isEmpty()) {
            try {
                BaseDataList<?> baseDataList = iDexKitList.dexkit(dexKitBridge);
                ArrayList<String> serializeList = new ArrayList<>();
                ArrayList<T> instanceList = new ArrayList<>();
                if (baseDataList instanceof FieldDataList fieldDataList) {
                    for (FieldData f : fieldDataList) {
                        serializeList.add(f.toDexField().serialize());
                        instanceList.add((T) f.getFieldInstance(classLoader));
                    }
                    mMMKV.putString(key, mGson.toJson(new MemberData(TYPE_FIELD, serializeList)) + "\n\n");
                    return instanceList;
                } else if (baseDataList instanceof MethodDataList methodDataList) {
                    for (MethodData m : methodDataList) {
                        serializeList.add(m.toDexMethod().serialize());
                        instanceList.add((T) m.getMethodInstance(classLoader));
                    }
                    mMMKV.putString(key, mGson.toJson(new MemberData(TYPE_METHOD, serializeList)) + "\n\n");
                    return instanceList;
                } else if (baseDataList instanceof ClassDataList classDataList) {
                    for (ClassData c : classDataList) {
                        serializeList.add(c.toDexType().serialize());
                        instanceList.add((T) c.getInstance(classLoader));
                    }
                    mMMKV.putString(key, mGson.toJson(new MemberData(TYPE_CLASS, serializeList)) + "\n\n");
                    return instanceList;
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            MemberData data = mGson.fromJson(descriptor, new TypeToken<MemberData>() {
            }.getType());
            ArrayList<T> instanceList = new ArrayList<>();
            try {
                switch (data.type) {
                    case TYPE_METHOD -> {
                        for (String s : data.serializeList) {
                            instanceList.add((T) new DexMethod(s).getMethodInstance(classLoader));
                        }
                        return instanceList;
                    }
                    case TYPE_FIELD -> {
                        for (String s : data.serializeList) {
                            instanceList.add((T) new DexField(s).getFieldInstance(classLoader));
                        }
                        return instanceList;
                    }
                    case TYPE_CLASS -> {
                        for (String s : data.serializeList) {
                            instanceList.add((T) new DexClass(s).getInstance(classLoader));
                        }
                        return instanceList;
                    }
                }
            } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return new ArrayList<>();
    }

    public static void deleteAllCache(Context context) {
        String[] folderNames = context.getResources().getStringArray(R.array.xposed_scope);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            for (String folderName : folderNames) {
                String folderPath = "/data/data/" + folderName + MMKV_PATH;
                rootExecCmd("rm -rf " + folderPath);
                folderPath = "/data/user_de/0/" + folderName + MMKV_PATH;
                rootExecCmd("rm -rf " + folderPath);
            }
        });
    }

    /**
     * 请勿手动调用。
     */
    public static void close() {
        if (!isInit) return;

        if (mDexKitBridge != null) {
            mDexKitBridge.close();
            mDexKitBridge = null;
        }
        if (mMMKV != null) {
            mMMKV.close();
            mMMKV = null;
        }
        mParam = null;
        mGson = null;
        isInit = false;
    }

    private static final class MemberData {
        public String type;
        public String serialize = "";

        public ArrayList<String> serializeList = new ArrayList<>();

        public MemberData(String type, String serialize) {
            this.type = type;
            this.serialize = serialize;
        }

        public MemberData(String type, ArrayList<String> serializeList) {
            this.type = type;
            this.serializeList = serializeList;
        }

        @NonNull
        @Override
        public String toString() {
            return "Type: " + type + ", Serialize: " + serialize + ", SerializeList: " + serializeList;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof MemberData memberData
                    && memberData.type.equals(type)
                    && memberData.serialize.equals(serialize)
                    && memberData.serializeList.equals(serializeList);
        }
    }
}
