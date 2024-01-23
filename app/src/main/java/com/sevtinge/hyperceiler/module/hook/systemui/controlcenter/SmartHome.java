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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import android.content.pm.ApplicationInfo;
import android.widget.RelativeLayout;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class SmartHome extends BaseHook {

    private ClassLoader mPluginLoader = null;

    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.shared.plugins.PluginManagerImpl", "getClassLoader", ApplicationInfo.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName)) {
                    if (mPluginLoader == null) {
                        mPluginLoader = (ClassLoader) param.getResult();
                        findAndHookMethod("miui.systemui.devicecontrols.ui.MiuiControlsUiControllerImpl", mPluginLoader, "updateOrientation", new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) throws Throwable {
                                RelativeLayout mParent = (RelativeLayout) XposedHelpers.getObjectField(param.thisObject, "parent");
                            }
                        });
                    }
                }
            }
        });
    }
}
