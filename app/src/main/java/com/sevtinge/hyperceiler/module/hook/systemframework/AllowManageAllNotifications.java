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

import com.sevtinge.hyperceiler.module.base.hook.HookTool;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class AllowManageAllNotifications implements IXposedHookZygoteInit  {
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws NoSuchMethodException {

        XposedHelpers.findAndHookMethod("android.app.NotificationChannel", startupParam.getClass().getClassLoader(), "isBlockable", HookTool.MethodHook.returnConstant(true));

        XposedHelpers.findAndHookMethod("android.app.NotificationChannel", startupParam.getClass().getClassLoader(), "setBlockable", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });

    }
}
