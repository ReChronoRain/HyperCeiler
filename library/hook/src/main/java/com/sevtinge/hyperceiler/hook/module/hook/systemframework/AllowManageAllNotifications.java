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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework;

import static com.sevtinge.hyperceiler.hook.module.base.tool.HookTool.MethodHook.returnConstant;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class AllowManageAllNotifications extends BaseHook {

    @Override
    public void init() {

        findAndHookMethod("android.app.NotificationChannel", lpparam.classLoader, "isBlockable", returnConstant(true));

        findAndHookMethod("android.app.NotificationChannel", lpparam.classLoader, "setBlockable", boolean.class, new MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });

    }
}
