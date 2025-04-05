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
package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class AddGoogleListHeader extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> mMiuiSettings = findClassIfExists("com.android.settings.MiuiSettings");
        findAndHookMethod(mMiuiSettings, "updateHeaderList", List.class, new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                List<?> list = (List<?>) param.args[0];
                XposedHelpers.callMethod(param.thisObject, "AddGoogleSettingsHeaders", list);
            }
        });
    }
}
