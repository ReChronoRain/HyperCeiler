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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.binder;

import android.os.RemoteException;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AutoEffectSwitchForSystem;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.control.BaseEffectControl;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.Collections;
import java.util.Map;

/**
 * Binder 服务实现
 * 提供跨进程的音效信息查询接口
 *
 * @author 焕晨HChen
 */
public class EffectInfoService extends IEffectInfo.Stub {

    private static final String TAG = "EffectInfoService";
    private final BaseEffectControl mBaseEffectControl;

    public EffectInfoService(BaseEffectControl baseEffectControl) {
        this.mBaseEffectControl = baseEffectControl;
        XposedLog.d(TAG, "EffectInfoService created with controller: " + baseEffectControl);
    }

    @Override
    public boolean isEarphoneConnection() throws RemoteException {
        return AutoEffectSwitchForSystem.getEarPhoneStateFinal();
    }

    @Override
    public Map<String, String> getEffectSupportMap() throws RemoteException {
        return safeGetMap(() -> mBaseEffectControl.getEffectSupportMap());
    }

    @Override
    public Map<String, String> getEffectAvailableMap() throws RemoteException {
        return safeGetMap(() -> mBaseEffectControl.getEffectAvailableMap());
    }

    @Override
    public Map<String, String> getEffectActiveMap() throws RemoteException {
        return safeGetMap(() -> mBaseEffectControl.getEffectActiveMap());
    }

    @Override
    public Map<String, String> getEffectEnabledMap() throws RemoteException {
        return safeGetMap(() -> mBaseEffectControl.getEffectEnabledMap());
    }

    @Override
    public Map<String, String> getEffectHasControlMap() throws RemoteException {
        return safeGetMap(() -> mBaseEffectControl.getEffectHasControlMap());
    }

    /**
     * 安全地获取 Map，处理空指针和异常
     */
    private Map<String, String> safeGetMap(MapSupplier supplier) throws RemoteException {
        if (mBaseEffectControl == null) {
            XposedLog.w(TAG, "BaseEffectControl is null");
            return Collections.emptyMap();
        }

        try {
            Map<String, String> result = supplier.get();
            return result != null ? result : Collections.emptyMap();
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to get map", e);
            throw new RemoteException("Failed to get effect map: " + e.getMessage());
        }
    }

    /**
     * Map 提供者接口
     */
    @FunctionalInterface
    private interface MapSupplier {
        Map<String, String> get() throws RemoteException;
    }
}
