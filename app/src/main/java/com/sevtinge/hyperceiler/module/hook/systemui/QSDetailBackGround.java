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
package com.sevtinge.hyperceiler.module.hook.systemui;

import android.content.pm.ApplicationInfo;
import android.graphics.Paint;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class QSDetailBackGround extends BaseHook {

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
                    }

                    hookAllMethods("miui.systemui.widget.SmoothRoundDrawable", mPluginLoader, "inflate", new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            Paint mPaint = (Paint) XposedHelpers.getObjectField(param.thisObject, "mPaint");
                            mPaint.setAlpha(mPrefsMap.getInt("system_control_center_qs_detail_bg", 0));
                        }
                    });
                }
            }
        });
    }
}
