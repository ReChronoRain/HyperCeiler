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
package com.sevtinge.hyperceiler.hook.utils.api.effect.binder;

import android.os.RemoteException;

import com.sevtinge.hyperceiler.hook.IEffectInfo;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.AutoEffectSwitchForSystem;
import com.sevtinge.hyperceiler.hook.utils.api.effect.control.BaseEffectControl;

import java.util.Collections;
import java.util.Map;

/**
 * Binder 本地实现
 *
 * @author 焕晨HChen
 */
public class EffectInfoService extends IEffectInfo.Stub {
    private final BaseEffectControl mBaseEffectControl;

    public EffectInfoService(BaseEffectControl baseEffectControl) {
        mBaseEffectControl = baseEffectControl;
    }

    @Override
    public boolean isEarphoneConnection() throws RemoteException {
        return AutoEffectSwitchForSystem.getEarPhoneStateFinal();
    }

    @Override
    public Map<String, String> getEffectSupportMap() throws RemoteException {
        if (mBaseEffectControl == null)
            return Collections.emptyMap();
        return mBaseEffectControl.getEffectSupportMap();
    }

    @Override
    public Map<String, String> getEffectAvailableMap() throws RemoteException {
        if (mBaseEffectControl == null)
            return Collections.emptyMap();
        return mBaseEffectControl.getEffectAvailableMap();
    }

    @Override
    public Map<String, String> getEffectActiveMap() throws RemoteException {
        if (mBaseEffectControl == null)
            return Collections.emptyMap();
        return mBaseEffectControl.getEffectActiveMap();
    }

    @Override
    public Map<String, String> getEffectEnabledMap() throws RemoteException {
        if (mBaseEffectControl == null)
            return Collections.emptyMap();
        return mBaseEffectControl.getEffectEnabledMap();
    }

    @Override
    public Map<String, String> getEffectHasControlMap() throws RemoteException {
        if (mBaseEffectControl==null)
            return Collections.emptyMap();
        return mBaseEffectControl.getEffectHasControlMap();
    }
}
