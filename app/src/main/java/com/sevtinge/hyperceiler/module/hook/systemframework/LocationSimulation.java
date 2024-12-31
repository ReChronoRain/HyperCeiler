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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;


public class LocationSimulation extends BaseHook {

    Class<?> mTelephonyManager;

    @Override
    public void init() {
        mTelephonyManager = findClassIfExists("android.telephony.TelephonyManager");

        if (mTelephonyManager != null) {

            findAndHookMethod(mTelephonyManager, "getNetworkOperatorName", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getNetworkOperatorName：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimOperatorName", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getSimOperatorName：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimOperator", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getSimOperator：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNetworkOperator", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getNetworkOperator：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimCountryIso", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getSimCountryIso：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNetworkCountryIso", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getNetworkCountryIso：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNeighboringCellInfo", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getNeighboringCellInfo：" + param.getResult());
                }
            });

        }
    }
}
