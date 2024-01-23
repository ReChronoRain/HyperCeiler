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
package com.sevtinge.hyperceiler.module.hook.various;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;
import miui.app.ActionBar;

public class CollapseMiuiTitle extends BaseHook {

    @Override
    public void init() {
        Class<?> abvCls = findClassIfExists("com.miui.internal.widget.AbsActionBarView");

        int opt = mPrefsMap.getStringAsInt("various_collapse_miui_title", 0);

        if (abvCls != null)
            hookAllConstructors(abvCls, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedHelpers.setIntField(param.thisObject, "mExpandState", ActionBar.STATE_EXPAND);
                    XposedHelpers.setIntField(param.thisObject, "mInnerExpandState", ActionBar.STATE_COLLAPSE);
                    if (opt == 2) XposedHelpers.setBooleanField(param.thisObject, "mResizable", false);
                }
            });

        abvCls = findClassIfExists("miuix.appcompat.internal.app.widget.ActionBarView");
        if (abvCls != null)
            hookAllConstructors(abvCls, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    try {
                        setExpandState(param.thisObject, opt == 1 || opt == 3);
                        setResizable(param.thisObject, opt == 3 || opt == 4);
                    } catch (Throwable ignore) {
                    }
                }
            });
    }

    private void setExpandState(Object obj, boolean state) {
        if (state) {
            XposedHelpers.callMethod(obj, "setExpandState", ActionBar.STATE_COLLAPSE);
        } else {
            XposedHelpers.callMethod(obj, "setExpandState", ActionBar.STATE_EXPAND);
        }
    }

    private void setResizable(Object obj, boolean state) {
        if (state) {
            XposedHelpers.callMethod(obj, "setResizable", false);
        } else {
            XposedHelpers.callMethod(obj, "setResizable", true);
        }
    }
}
