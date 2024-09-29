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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodReplacement;

public class DisableFreeformBlackList extends BaseHook {

    Class<?> mTaskCls;
    Class<?> mMiuiMultiWindowAdapter;
    Class<?> mMiuiMultiWindowUtils;

    @Override
    public void init() {

        mTaskCls = findClassIfExists("com.android.server.wm.Task");
        mMiuiMultiWindowAdapter = findClassIfExists("android.util.MiuiMultiWindowAdapter");
        mMiuiMultiWindowUtils = findClassIfExists("android.util.MiuiMultiWindowUtils");

        MethodHook clearHook = new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                List<String> blackList = (List<String>) param.getResult();
                if (blackList != null) blackList.clear();
                param.setResult(blackList);
            }
        };
        hookAllMethods(mMiuiMultiWindowAdapter, "getFreeformBlackList", clearHook);
        hookAllMethods(mMiuiMultiWindowAdapter, "getFreeformBlackListFromCloud", clearHook);
        hookAllMethods(mMiuiMultiWindowAdapter, "setFreeformBlackList", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                List<String> blackList = new ArrayList<>();
                blackList.add("ab.cd.xyz");
                param.args[0] = blackList;
            }
        });

        findAndHookMethod(mMiuiMultiWindowUtils, "isForceResizeable", XC_MethodReplacement.returnConstant(true));
        findAndHookMethod(mMiuiMultiWindowUtils, "supportFreeform", XC_MethodReplacement.returnConstant(true));

        // 强制所有活动设为可以调整大小
        /*findAndHookMethod(mTaskCls, "isResizeable", XC_MethodReplacement.returnConstant(true));*/

        mResHook.setResReplacement("android", "array", "freeform_black_list", R.array.miui_freeform_black_list);
        mResHook.setResReplacement("android.miui", "array", "freeform_black_list", R.array.miui_freeform_black_list);
        mResHook.setResReplacement("com.miui.rom", "array", "freeform_black_list", R.array.miui_freeform_black_list);
    }
}
