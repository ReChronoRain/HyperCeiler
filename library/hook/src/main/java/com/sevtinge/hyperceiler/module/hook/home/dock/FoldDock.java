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
package com.sevtinge.hyperceiler.module.hook.home.dock;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class FoldDock extends BaseHook {

    Class<?> mHotSeatsList;
    Class<?> mHotSeatsListRecentsAppProvider;

    Class<?> mDeviceConfig;
    Class<?> mApplication;

    @Override
    public void init() {
        mHotSeatsList = findClassIfExists("com.miui.home.launcher.hotseats.HotSeatsList");
        mHotSeatsListRecentsAppProvider = findClassIfExists("com.miui.home.launcher.hotseats.HotSeatsListRecentsAppProvider");
        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");
        mApplication = findClassIfExists("com.miui.home.launcher.Application");

        findAndHookMethod(mHotSeatsListRecentsAppProvider, "getLimitCount", XC_MethodReplacement.returnConstant(0));

        findAndHookMethod(mDeviceConfig, "getHotseatMaxCount", XC_MethodReplacement.returnConstant(5));


        findAndHookMethod("com.miui.home.launcher.hotseats.HotSeats", "initContent", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod(mDeviceConfig, "isFoldDevice",
                    XC_MethodReplacement.returnConstant(true));
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {

                findAndHookMethod(mDeviceConfig, "isFoldDevice",
                    XC_MethodReplacement.returnConstant(false));
            }
        });
        findAndHookMethod("com.miui.home.launcher.hotseats.HotSeats", "updateContentView", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod(mApplication, "isInFoldLargeScreen",
                    XC_MethodReplacement.returnConstant(true));
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                findAndHookMethod(mApplication, "isInFoldLargeScreen",
                    XC_MethodReplacement.returnConstant(false));
            }
        });

        findAndHookMethod("com.miui.home.launcher.allapps.LauncherMode", "isHomeSupportSearchBar", Context.class, XC_MethodReplacement.returnConstant(false));
    }
}
