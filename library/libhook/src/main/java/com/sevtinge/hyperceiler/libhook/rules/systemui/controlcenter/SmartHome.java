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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter;

import android.content.pm.ApplicationInfo;
import android.widget.RelativeLayout;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class SmartHome extends BaseHook {

    private ClassLoader mPluginLoader = null;

    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.shared.plugins.PluginManagerImpl", "getClassLoader", ApplicationInfo.class, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ApplicationInfo appInfo = (ApplicationInfo) param.getArgs()[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName)) {
                    if (mPluginLoader == null) {
                        mPluginLoader = (ClassLoader) param.getResult();
                        EzxHelpUtils.findAndHookMethod("miui.systemui.devicecontrols.ui.MiuiControlsUiControllerImpl", mPluginLoader, "updateOrientation", new IMethodHook() {
                            @Override
                            public void after(AfterHookParam param) {
                                RelativeLayout mParent = (RelativeLayout) getObjectField(param.getThisObject(), "parent");
                            }
                        });
                    }
                }
            }
        });
    }
}
