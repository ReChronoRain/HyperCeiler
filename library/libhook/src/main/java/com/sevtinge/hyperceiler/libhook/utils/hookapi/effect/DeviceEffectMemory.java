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
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

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
    private static final String SETTINGS_KEY = "bluetooth_ear_device_effect_memory";

    private final Context mContext;
    private final Gson mGson;
    private DeviceEffectData mData;

    public DeviceEffectMemory(Context context) {
        this.mContext = context;
        this.mGson = new GsonBuilder().create();
        loadData();
    }

    /**
     * 加载数据
     */
    private void loadData() {
        try {
            String json = Settings.Global.getString(mContext.getContentResolver(), SETTINGS_KEY);
            if (json != null && !json.isEmpty()) {
                mData = mGson.fromJson(json, DeviceEffectData.class);
                XposedLog.d(TAG, "Loaded " + mData.devices.size() + " devices");
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to load data", e);
        }

        if (mData == null) {
            mData = new DeviceEffectData();
        }
    }

    /**
     * 保存数据
     */
    private synchronized void saveData() {
        try {
            String json = mGson.toJson(mData);
            Settings.Global.putString(mContext.getContentResolver(), SETTINGS_KEY, json);
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to save data", e);
        }
    }

    // ==================== 设备音效操作 ====================

    public EffectState getDeviceEffect(String deviceId) {
        if (deviceId == null) return null;
        return mData.devices.get(deviceId);
    }

    public void saveDeviceEffect(String deviceId, EffectState state) {
        if (deviceId == null || state == null) return;
        mData.devices.put(deviceId, state);
        saveData();
        XposedLog.d(TAG, "Saved: " + deviceId + " -> " + state);
    }

    public void removeDeviceEffect(String deviceId) {
        if (deviceId == null) return;
        mData.devices.remove(deviceId);
        saveData();}

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
        mData.speakerEffect = null;
        saveData();
    }

    // ==================== 静态工具方法 ====================

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
        public int version = 1;
        public EffectState speakerEffect;
        public ConcurrentHashMap<String, EffectState> devices = new ConcurrentHashMap<>();
    }

    public static class EffectState {
        public String mainEffect;
        public boolean spatialAudio;
        public boolean surround;
        public long timestamp;
        public String deviceName;

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
                ", surround=" + surround + ", device='" + deviceName + "'}";
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
