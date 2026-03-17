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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.effect;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DEVICE_TYPE_USB;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DEVICE_TYPE_WIRED;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sevtinge.hyperceiler.common.log.XposedLog;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备音效记忆管理
 *
 * @author Ling Qiqi
 */
public class DeviceEffectMemory {

    private static final String TAG = "DeviceEffectMemory";
    private static final String STORAGE_DIR = "/data/system/hyperceiler";
    private static final String STORAGE_FILE = "effect_memory.json";

    private final File mFile;
    private final Gson mGson;
    private DeviceEffectData mData;

    public DeviceEffectMemory() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) dir.mkdirs();
        mFile = new File(dir, STORAGE_FILE);
        mGson = new GsonBuilder().setPrettyPrinting().create();
        loadData();
    }

    private synchronized void loadData() {
        try {
            if (mFile.exists()) {
                String json = new String(Files.readAllBytes(mFile.toPath()));
                if (!json.isEmpty()) {
                    mData = mGson.fromJson(json, DeviceEffectData.class);
                    XposedLog.d(TAG, "Loaded " + mData.devices.size() + " devices, "
                        + mData.macToDeviceId.size() + " MAC mappings");
                }
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to load data", e);
        }
        if (mData == null) mData = new DeviceEffectData();
    }

    private synchronized void saveData() {
        try {
            String json = mGson.toJson(mData);
            try (FileOutputStream fos = new FileOutputStream(mFile)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to save data", e);
        }
    }


    // ==================== 设备 ID 归一化 ====================

    /**
     * 获取或创建设备 ID（连接时使用）
     */
    public String getOrCreateDeviceId(BluetoothDevice btDevice) {
        if (btDevice == null) return null;

        String mac = getBluetoothMac(btDevice);
        String rawId = generateBluetoothDeviceId(btDevice);
        if (mac == null) return rawId;

        // 如果这个 MAC 已经映射到了某个 ID，且该 ID 的记忆存在，直接返回
        String mappedId = mData.macToDeviceId.get(mac);
        if (mappedId != null && mData.devices.containsKey(mappedId)) {
            XposedLog.d(TAG, "Found existing mapping: MAC " + mac + " -> " + mappedId);
            return mappedId;
        }

        // 否则将这个 MAC 绑定到它自己的 rawId
        mData.macToDeviceId.put(mac, rawId);
        saveData();
        XposedLog.d(TAG, "Created new mapping: MAC " + mac + " -> " + rawId);
        return rawId;
    }

    /**
     * 强制将 MAC 绑定到指定的主设备 ID（断开时使用）
     * 解决 LE Audio 断开时 MAC 与连接时不同的问题
     */
    public void bindMacToDeviceId(BluetoothDevice btDevice, String targetDeviceId) {
        if (btDevice == null || targetDeviceId == null) return;

        String mac = getBluetoothMac(btDevice);
        if (mac == null) return;

        String currentMappedId = mData.macToDeviceId.get(mac);
        // 如果这个 MAC 还没绑定，或者绑定的不是当前会话的 targetDeviceId，则强制修正
        if (!targetDeviceId.equals(currentMappedId)) {
            mData.macToDeviceId.put(mac, targetDeviceId);
            saveData();
            XposedLog.d(TAG, "Bound LE Audio paired MAC " + mac + " to session ID: " + targetDeviceId);
        }
    }

    // ==================== 设备音效操作 ====================

    public EffectState getDeviceEffect(String deviceId) {
        if (deviceId == null) return null;
        return mData.devices.get(deviceId);
    }

    public void saveDeviceEffect(String deviceId, EffectState state, BluetoothDevice btDevice) {
        if (deviceId == null || state == null) return;

        if (btDevice != null) {
            state.macAddress = getBluetoothMac(btDevice);
        }

        mData.devices.put(deviceId, state);
        saveData();
        XposedLog.d(TAG, "Saved: " + deviceId + " -> " + state);
    }

    public void removeDeviceEffect(String deviceId) {
        if (deviceId == null) return;
        mData.devices.remove(deviceId);
        mData.macToDeviceId.values().removeIf(id -> id.equals(deviceId));
        saveData();
    }

    public EffectState getSpeakerEffect() {
        return mData.speakerEffect;
    }

    public void saveSpeakerEffect(EffectState state) {
        mData.speakerEffect = state;
        saveData();
        XposedLog.d(TAG, "Saved speaker: " + state);
    }

    public Map<String, EffectState> getAllDevices() {
        return new ConcurrentHashMap<>(mData.devices);
    }

    public void clearAll() {
        mData.devices.clear();
        mData.macToDeviceId.clear();
        mData.speakerEffect = null;
        saveData();
    }

    // ==================== 静态工具方法 ====================

    public static String getBluetoothMac(BluetoothDevice device) {
        if (device == null) return null;
        try {
            return device.getAddress();
        } catch (SecurityException e) {
            return null;
        }
    }

    public static String generateBluetoothDeviceId(BluetoothDevice device) {
        if (device == null) return null;
        try {
            String address = device.getAddress();
            return "bt_" + (address != null ? address.replace(":", "") : "unknown");
        } catch (SecurityException e) {
            return "bt_" + device.hashCode();
        }
    }

    public static String getBluetoothDeviceName(BluetoothDevice device) {
        if (device == null) return null;
        try {
            return device.getName();
        } catch (SecurityException e) {
            return null;
        }
    }

    public static String generateWiredDeviceId() {
        return DEVICE_TYPE_WIRED;
    }

    public static String generateUsbDeviceId() {
        return DEVICE_TYPE_USB;
    }

    // ==================== 数据类 ====================

    public static class DeviceEffectData {
        public int version = 2;
        public EffectState speakerEffect;
        public ConcurrentHashMap<String, EffectState> devices = new ConcurrentHashMap<>();
        public ConcurrentHashMap<String, String> macToDeviceId = new ConcurrentHashMap<>();
    }

    public static class EffectState {
        public String mainEffect;
        public boolean spatialAudio;
        public boolean surround;
        public long timestamp;
        public String deviceName;
        public String macAddress;

        public EffectState() {}

        public EffectState(String mainEffect, boolean spatialAudio, boolean surround) {
            this.mainEffect = mainEffect;
            this.spatialAudio = spatialAudio;
            this.surround = surround;
            this.timestamp = System.currentTimeMillis();
        }

        @NonNull
        @Override
        public String toString() {
            return "EffectState{main='" + mainEffect + "', spatial=" + spatialAudio +
                ", surround=" + surround + ", device='" + deviceName +
                "', mac='" + macAddress + "'}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EffectState that)) return false;
            return spatialAudio == that.spatialAudio && surround == that.surround &&
                Objects.equals(mainEffect, that.mainEffect);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mainEffect, spatialAudio, surround);
        }
    }
}
