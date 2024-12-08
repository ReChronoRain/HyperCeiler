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

import com.sevtinge.hyperceiler.R;
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

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author 焕晨HChen
 * @co-author Ling Qiqi
 */
public class DexKit {
    private static final int mVersion = 6;
    public boolean isInit = false;
    private static final String MMKV_PATH = "/files/mmkv";
    private static XC_LoadPackage.LoadPackageParam mParam;
    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";
    private static MMKV mMMKV = null;
    private static DexKit mThis;
    private static DexKitBridge mDexKitBridge;
    private final String TAG;

    public DexKit(XC_LoadPackage.LoadPackageParam param, String tag) {
        mParam = param;
        TAG = tag;
        mThis = this;
    }

    @NotNull
    public static DexKitBridge initDexkitBridge() {
        if (mDexKitBridge == null) {
            if (mThis == null) {
                throw new RuntimeException("InitDexKit is null!!");
            } else {
                mThis.init();
            }
        }
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
        String type = mMMKV.getString(key + "_type", "");
        int size = mMMKV.getInt(key + "_size", 0);
        if (type.isEmpty() || size == 0) {
            try {
                BaseData baseData = iDexKit.dexkit(dexKitBridge);
                if (baseData instanceof FieldData fieldData) {
                    saveToMMKV(key, TYPE_FIELD, List.of(fieldData.toDexField().serialize()));
                    return (T) fieldData.getFieldInstance(classLoader);
                } else if (baseData instanceof MethodData methodData) {
                    saveToMMKV(key, TYPE_METHOD, List.of(methodData.toDexMethod().serialize()));
                    return (T) methodData.getMethodInstance(classLoader);
                } else if (baseData instanceof ClassData classData) {
                    saveToMMKV(key, TYPE_CLASS, List.of(classData.toDexType().serialize()));
                    return (T) classData.getInstance(classLoader);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                String serializedItem = mMMKV.getString(key + "_item_0", "");
                switch (type) {
                    case TYPE_METHOD -> {
                        return (T) new DexMethod(serializedItem).getMethodInstance(classLoader);
                    }
                    case TYPE_FIELD -> {
                        return (T) new DexField(serializedItem).getFieldInstance(classLoader);
                    }
                    case TYPE_CLASS -> {
                        return (T) new DexClass(serializedItem).getInstance(classLoader);
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
        String type = mMMKV.getString(key + "_type", "");
        int size = mMMKV.getInt(key + "_size", 0);
        if (type.isEmpty() || size == 0) {
            try {
                BaseDataList<?> baseDataList = iDexKitList.dexkit(dexKitBridge);
                ArrayList<String> serializeList = new ArrayList<>();
                ArrayList<T> instanceList = new ArrayList<>();
                if (baseDataList instanceof FieldDataList fieldDataList) {
                    for (FieldData f : fieldDataList) {
                        serializeList.add(f.toDexField().serialize());
                        instanceList.add((T) f.getFieldInstance(classLoader));
                    }
                    saveToMMKV(key, TYPE_FIELD, serializeList);
                    return instanceList;
                } else if (baseDataList instanceof MethodDataList methodDataList) {
                    for (MethodData m : methodDataList) {
                        serializeList.add(m.toDexMethod().serialize());
                        instanceList.add((T) m.getMethodInstance(classLoader));
                    }
                    saveToMMKV(key, TYPE_METHOD, serializeList);
                    return instanceList;
                } else if (baseDataList instanceof ClassDataList classDataList) {
                    for (ClassData c : classDataList) {
                        serializeList.add(c.toDexType().serialize());
                        instanceList.add((T) c.getInstance(classLoader));
                    }
                    saveToMMKV(key, TYPE_CLASS, serializeList);
                    return instanceList;
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            ArrayList<T> instanceList = new ArrayList<>();
            try {
                for (int i = 0; i < size; i++) {
                    String serializedItem = mMMKV.getString(key + "_item_" + i, "");
                    if (serializedItem.isEmpty()) continue;
                    switch (type) {
                        case TYPE_METHOD -> instanceList.add((T) new DexMethod(serializedItem).getMethodInstance(classLoader));
                        case TYPE_FIELD -> instanceList.add((T) new DexField(serializedItem).getFieldInstance(classLoader));
                        case TYPE_CLASS -> instanceList.add((T) new DexClass(serializedItem).getInstance(classLoader));
                    }
                }
                return instanceList;
            } catch (NoSuchMethodException | NoSuchFieldException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return new ArrayList<>();
    }

    private static void saveToMMKV(String key, String type, List<String> serializeList) {
        mMMKV.putString(key + "_type", type);
        mMMKV.putInt(key + "_size", serializeList.size());
        for (int i = 0; i < serializeList.size(); i++) {
            mMMKV.putString(key + "_item_" + i, serializeList.get(i));
        }
    }

    private void init() {
        if (mDexKitBridge != null) {
            return;
        }

        if (mParam == null) {
            throw new RuntimeException(TAG != null ? TAG : "InitDexKit" + ": lpparam is null");
        }

        String hostDir = mParam.appInfo.sourceDir,
                mmkvPath = mParam.appInfo.dataDir + MMKV_PATH;

        // 启动 MMKV
        MMKV.initialize(mmkvPath, System::loadLibrary);
        mMMKV = MMKV.defaultMMKV();
        int version = mMMKV.getInt("version", 0);
        if (version != 0 && version != mVersion) {
            mMMKV.clearAll();
        }
        mMMKV.putInt("version", mVersion);

        // 启动 DexKit
        System.loadLibrary("dexkit");
        mDexKitBridge = DexKitBridge.create(hostDir);
        isInit = true;
    }

    /**
     * 请勿手动调用。
     */
    public void close() {
        if (mDexKitBridge != null) {
            mDexKitBridge.close();
            mDexKitBridge = null;
        }
        if (mMMKV != null) {
            mMMKV.close();
            mMMKV = null;
        }
        mParam = null;
        mThis = null;
        isInit = false;
    }

    public static void deleteAllCache(Context context) {
        String[] folderNames = context.getResources().getStringArray(R.array.xposed_scope);
        for (String folderName : folderNames) {
            String folderPath = "/data/data/" + folderName + MMKV_PATH;
            if (safeExecCommandWithRoot("ls " + folderPath).contains("mmkv")) {
                safeExecCommandWithRoot("rm -rf " + folderPath);
            }
        }
    }
}
