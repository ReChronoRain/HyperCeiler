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
package com.sevtinge.hyperceiler.utils.api.effect.control;

import android.os.RemoteException;

import java.util.HashMap;
import java.util.Map;

/**
 * 基本音效控制类
 *
 * @author 焕晨HChen
 */
public class BaseEffectControl {
    final HashMap<String, String> mEffectSupportMap = new HashMap<>();
    final HashMap<String, String> mEffectAvailableMap = new HashMap<>();
    final HashMap<String, String> mEffectActiveMap = new HashMap<>();
    final HashMap<String, String> mEffectEnabledMap = new HashMap<>();
    final HashMap<String, String> mEffectHasControlMap = new HashMap<>();

    void updateEffectMap() {
    }

    public Map<String, String> getEffectSupportMap() throws RemoteException {
        updateEffectMap();
        return mEffectSupportMap;
    }

    public Map<String, String> getEffectAvailableMap() throws RemoteException {
        updateEffectMap();
        return mEffectAvailableMap;
    }

    public Map<String, String> getEffectActiveMap() throws RemoteException {
        updateEffectMap();
        return mEffectActiveMap;
    }

    public Map<String, String> getEffectEnabledMap() throws RemoteException {
        updateEffectMap();
        return mEffectEnabledMap;
    }

    public Map<String, String> getEffectHasControlMap() throws RemoteException {
        updateEffectMap();
        return mEffectHasControlMap;
    }
}
