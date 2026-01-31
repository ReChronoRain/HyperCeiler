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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.control;

import android.os.RemoteException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基本音效控制类
 * 提供音效状态的存储和查询功能
 *
 * @author 焕晨HChen
 */
public abstract class BaseEffectControl {

    // 使用 ConcurrentHashMap 保证线程安全
    protected final Map<String, String> mEffectSupportMap = new ConcurrentHashMap<>();
    protected final Map<String, String> mEffectAvailableMap = new ConcurrentHashMap<>();
    protected final Map<String, String> mEffectActiveMap = new ConcurrentHashMap<>();
    protected final Map<String, String> mEffectEnabledMap = new ConcurrentHashMap<>();
    protected final Map<String, String> mEffectHasControlMap = new ConcurrentHashMap<>();

    /**
     * 更新所有音效状态 Map
     * 子类必须实现此方法
     */
    protected abstract void updateEffectMap();

    /**
     * 获取音效支持状态
     */
    public Map<String, String> getEffectSupportMap() throws RemoteException {
        updateEffectMap();
        return Collections.unmodifiableMap(new HashMap<>(mEffectSupportMap));
    }

    /**
     * 获取音效可用状态
     */
    public Map<String, String> getEffectAvailableMap() throws RemoteException {
        updateEffectMap();
        return Collections.unmodifiableMap(new HashMap<>(mEffectAvailableMap));
    }

    /**
     * 获取音效激活状态
     */
    public Map<String, String> getEffectActiveMap() throws RemoteException {
        updateEffectMap();
        return Collections.unmodifiableMap(new HashMap<>(mEffectActiveMap));
    }

    /**
     * 获取音效启用状态
     */
    public Map<String, String> getEffectEnabledMap() throws RemoteException {
        updateEffectMap();
        return Collections.unmodifiableMap(new HashMap<>(mEffectEnabledMap));
    }

    /**
     * 获取音效控制权状态
     */
    public Map<String, String> getEffectHasControlMap() throws RemoteException {
        updateEffectMap();
        return Collections.unmodifiableMap(new HashMap<>(mEffectHasControlMap));
    }
    /**
     * 安全地将布尔值放入 Map
     */
    protected void putBoolean(Map<String, String> map, String key, boolean value) {
        if (map != null && key != null) {
            map.put(key, String.valueOf(value));
        }
    }

    /**
     * 清空并更新 Map
     */
    protected void clearAndUpdate(Map<String, String> map, Map<String, String> newData) {
        if (map != null) {
            map.clear();
            if (newData != null) {
                map.putAll(newData);
            }
        }
    }
}
