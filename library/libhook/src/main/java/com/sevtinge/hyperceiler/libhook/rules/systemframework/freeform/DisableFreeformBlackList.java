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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setBooleanField;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.ArrayList;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

// https://github.com/HowieHChen/XiaomiHelper/blob/master/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/android/RemoveFreeformRestriction.kt
public class DisableFreeformBlackList extends BaseHook {

    @Override
    public void init() {
        Class<?> activityTaskManager = findClassIfExists("android.app.ActivityTaskManager");
        if (activityTaskManager != null) {
            hookAllMethods(activityTaskManager, "supportsSplitScreen", returnConstant(true));
        }

        Class<?> taskCls = findClassIfExists("com.android.server.wm.Task");
        if (taskCls != null) {
            hookAllMethods(taskCls, "isResizeable", returnConstant(true));
        }

        Class<?> atmService = findClassIfExists("com.android.server.wm.ActivityTaskManagerService");
        if (atmService != null) {
            hookAllMethods(atmService, "retrieveSettings", new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    setBooleanField(param.getThisObject(), "mDevEnableNonResizableMultiWindow", true);
                }
            });
        }

        Class<?> settingsObserver = findClassIfExists("com.android.server.wm.WindowManagerService$SettingsObserver");
        if (settingsObserver != null) {
            hookAllMethods(settingsObserver, "updateDevEnableNonResizableMultiWindow", returnConstant(null));
        }

        Class<?> miuiMultiWindowAdapter = findClassIfExists("android.util.MiuiMultiWindowAdapter");
        if (miuiMultiWindowAdapter != null) {
            String[] blackListFields = {
                    "FREEFORM_BLACK_LIST",
                    "ABNORMAL_FREEFORM_BLACK_LIST",
                    "START_FROM_FREEFORM_BLACK_LIST_ACTIVITY",
                    "FOREGROUND_PIN_APP_BLACK_LIST"
            };
            for (String fieldName : blackListFields) {
                try {
                    setStaticObjectField(miuiMultiWindowAdapter, fieldName, new ArrayList<String>());
                } catch (Throwable ignored) {
                }
            }

            String[] blackListMethods = {
                    "getFreeformBlackList",
                    "getFreeformBlackListFromCloud",
                    "getAbnormalFreeformBlackList",
                    "getAbnormalFreeformBlackListFromCloud",
                    "getStartFromFreeformBlackList",
                    "getStartFromFreeformBlackListFromCloud",
                    "getForegroundPinAppBlackList",
                    "getForegroundPinAppBlackListFromCloud"
            };
            for (String methodName : blackListMethods) {
                hookAllMethods(miuiMultiWindowAdapter, methodName, returnConstant(new ArrayList<String>()));
            }
        }

        Class<?> miuiMultiWindowUtils = findClassIfExists("android.util.MiuiMultiWindowUtils");
        if (miuiMultiWindowUtils != null) {
            hookAllMethods(miuiMultiWindowUtils, "isForceResizeable", returnConstant(true));
            hookAllMethods(miuiMultiWindowUtils, "supportFreeform", returnConstant(true));
        }
    }
}
