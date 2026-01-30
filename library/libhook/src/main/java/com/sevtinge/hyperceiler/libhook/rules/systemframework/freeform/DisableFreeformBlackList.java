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

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.ArrayList;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class DisableFreeformBlackList extends BaseHook {

    Class<?> mTaskCls;
    Class<?> mMiuiMultiWindowAdapter;
    Class<?> mMiuiMultiWindowUtils;

    Class<?> mMiuiFreeformServiceImpl;

    @Override
    public void init() {
        if (isMoreAndroidVersion(36)) {
            mMiuiFreeformServiceImpl = findClassIfExists("com.android.server.wm.MiuiFreeformServiceImpl");

            IMethodHook clearHook = new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    List<String> blackList = (List<String>) param.getResult();
                    if (blackList != null) blackList.clear();
                    param.setResult(blackList);
                }
            };
            hookAllMethods(mMiuiFreeformServiceImpl, "getFreeformBlackList", clearHook);
            hookAllMethods(mMiuiFreeformServiceImpl, "setFreeformBlackList", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    List<String> blackList = new ArrayList<>();
                    blackList.add("ab.cd.xyz");
                    param.getArgs()[0] = blackList;
                }
            });

            findAndHookMethod(mMiuiFreeformServiceImpl, "isForceResizeable", returnConstant(true));
            findAndHookMethod(mMiuiFreeformServiceImpl, "isSupportFreeFormMultiTask", String.class, returnConstant(true));
        } else {

            mMiuiMultiWindowAdapter = findClassIfExists("android.util.MiuiMultiWindowAdapter");
            mMiuiMultiWindowUtils = findClassIfExists("android.util.MiuiMultiWindowUtils");

            IMethodHook clearHook = new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    List<String> blackList = (List<String>) param.getResult();
                    if (blackList != null) blackList.clear();
                    param.setResult(blackList);
                }
            };
            hookAllMethods(mMiuiMultiWindowAdapter, "getFreeformBlackList", clearHook);
            hookAllMethods(mMiuiMultiWindowAdapter, "getFreeformBlackListFromCloud", clearHook);
            hookAllMethods(mMiuiMultiWindowAdapter, "setFreeformBlackList", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    List<String> blackList = new ArrayList<>();
                    blackList.add("ab.cd.xyz");
                    param.getArgs()[0] = blackList;
                }
            });

            findAndHookMethod(mMiuiMultiWindowUtils, "isForceResizeable", returnConstant(true));
            findAndHookMethod(mMiuiMultiWindowUtils, "supportFreeform", returnConstant(true));
        }
        // 强制所有活动设为可以调整大小
        try {
            mTaskCls = findClassIfExists("com.android.server.wm.Task");
            findAndHookMethod(mTaskCls, "isResizeable", returnConstant(true));
        } catch (Throwable t) {
            XposedLog.e(TAG, "DisableFreeformBlackList: hook isResizeable failed", t);
        }

        setResReplacement("android", "array", "freeform_black_list", R.array.miui_freeform_black_list);
        setResReplacement("android.miui", "array", "freeform_black_list", R.array.miui_freeform_black_list);
        setResReplacement("com.miui.rom", "array", "freeform_black_list", R.array.miui_freeform_black_list);
    }
}
