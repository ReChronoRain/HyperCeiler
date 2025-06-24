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
package com.sevtinge.hyperceiler.hook.module.hook.getapps;

import android.os.Build;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class DeviceModify extends BaseHook {

    String mDevice;
    String mModel;
    String mManufacturer;

    @Override
    public void init() {
        if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 108) {
            // 15sp
            mDevice = "dijun"; // O2S
            mModel = "25042PN24C";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 107) {
            // 15u
            mDevice = "xuanyuan"; // O1
            mModel = "25010PN30C";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 106) {
            // 15p
            mDevice = "haotian"; // O2
            mModel = "2410DPN6CC";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 105) {
            // 15
            mDevice = "dada"; // O3
            mModel = "24129PN74C";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 156) {
            // civi5p
            mDevice = "luming";
            mModel = "25067PYE3C";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 202) {
            // flip2
            mDevice = "bixi"; // O8
            mModel = "2505APX7BC";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 224) {
            // f4
            mDevice = "goku";  // N18
            mModel = "24072PX77C";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 190) {
            // alpha
            mDevice = "avenger";
            mModel = "MIX Alpha";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 210) {
            // pad7
            mDevice = "uke"; // O81
            mModel = "2410CRP4CC";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 211) {
            // pad7p
            mDevice = "muyu"; // O82
            mModel = "24091RPADC";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 212) {
            // pad7u
            mDevice = "jinghu";
            mModel = "25032RP42C";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 213) {
            // pad7sp
            mDevice = "violin";
            mModel = "25053RP5CC";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 354) {
            // n14pp
            mDevice = "amethyst";
            mModel = "24090RA29C"; // O16U
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 314) {
            // k80u
            mDevice = "dali";
            mModel = "25060RK16C";
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 313) {
            // k80pc
            mDevice = "miro";
            mModel = "24127RK2CC";
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 312) {
            // k80p
            mDevice = "miro";
            mModel = "24122RKC7C";
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 311) {
            // k80
            mDevice = "zorn";
            mModel = "24117RK2CC";
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 310) {
            // k70u
            mDevice = "rothko";
            mModel = "2407FRK8EC";
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 335) {
            // t4p
            mDevice = "onyx";
            mModel = "25053RT47C";
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 403) {
            // 14c
            mDevice = "lake";
            mModel = "2409BRN2CC";
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 1) {
            // customization
            mDevice = mPrefsMap.getString("market_device_modify_device", "");
            mModel = mPrefsMap.getString("market_device_modify_model", "");
            mManufacturer = mPrefsMap.getString("market_device_modify_manufacturer", "");
        }
        findAndHookConstructor("com.xiaomi.market.MarketApp", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                XposedHelpers.setStaticObjectField(Build.class, "DEVICE", mDevice);
                XposedHelpers.setStaticObjectField(Build.class, "MODEL", mModel);
                XposedHelpers.setStaticObjectField(Build.class, "MANUFACTURER", mManufacturer);
            }
        });
    }
}
