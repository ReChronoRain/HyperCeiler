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
package com.sevtinge.hyperceiler.libhook.rules.home.other;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class InfiniteScroll extends BaseHook {

    @Override
    public void init() {

        findAndHookMethod("com.miui.home.launcher.ScreenView", "getSnapToScreenIndex", int.class, int.class, int.class, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (param.getArgs()[0] != param.getResult()) return;
                int screenCount = (int) callMethod(param.getThisObject(), "getScreenCount");
                if ((int) param.getArgs()[2] == -1 && (int) param.getArgs()[0] == 0)
                    param.setResult(screenCount);
                else if ((int) param.getArgs()[2] == 1 && (int) param.getArgs()[0] == screenCount - 1)
                    param.setResult(0);
            }
        });

        findAndHookMethod("com.miui.home.launcher.ScreenView", "getSnapUnitIndex", int.class, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                int index = (int) param.getResult();
                int mCurrentScreenIndex = EzxHelpUtils.getIntField(param.getThisObject(), getPackageName().equals("com.miui.home") ? "mCurrentScreenIndex" : "mCurrentScreen");
                if (mCurrentScreenIndex != index) return;
                int screenCount = (int) callMethod(param.getThisObject(), "getScreenCount");
                if (index == 0) {
                    param.setResult(screenCount);
                } else if (index == screenCount - 1) {
                    param.setResult(0);
                }
            }
        });
    }
}
