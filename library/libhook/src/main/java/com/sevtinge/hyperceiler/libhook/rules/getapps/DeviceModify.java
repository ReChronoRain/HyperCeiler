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
package com.sevtinge.hyperceiler.libhook.rules.getapps;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;

import android.os.Build;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;


public class DeviceModify extends BaseHook {

    String mDevice;
    String mModel;
    String mManufacturer;

    @Override
    public void init() {
        mDevice = mPrefsMap.getString("market_device_modify_device", "");
        mModel = mPrefsMap.getString("market_device_modify_model", "");
        mManufacturer = mPrefsMap.getString("market_device_modify_manufacturer", "");
        findAndHookConstructor("com.xiaomi.market.MarketApp", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                setStaticObjectField(Build.class, "DEVICE", mDevice);
                setStaticObjectField(Build.class, "MODEL", mModel);
                setStaticObjectField(Build.class, "MANUFACTURER", mManufacturer);
            }
        });
    }
}
