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
package com.sevtinge.hyperceiler.libhook.rules.various.system;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class CollapseMiuiTitle extends BaseHook {

    @Override
    public void init() {
        Class<?> abvCls = findClassIfExists("com.miui.internal.widget.AbsActionBarView");

        int opt = PrefsBridge.getStringAsInt("various_collapse_miui_title", 0);

        if (abvCls != null)
            hookAllConstructors(abvCls, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.setIntField(param.getThisObject(), "mExpandState", (int) com.sevtinge.hyperceiler.libhook.base.BaseHook.getStaticObjectField(
                            findClassIfExists("miui.app.ActionBar"),
                            "STATE_EXPAND"));
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.setIntField(param.getThisObject(), "mInnerExpandState", (int) com.sevtinge.hyperceiler.libhook.base.BaseHook.getStaticObjectField(
                            findClassIfExists("miui.app.ActionBar"),
                            "STATE_COLLAPSE"));
                    if (opt == 2)
                        com.sevtinge.hyperceiler.libhook.base.BaseHook.setBooleanField(param.getThisObject(), "mResizable", false);
                }
            });

        abvCls = findClassIfExists("miuix.appcompat.internal.app.widget.ActionBarView");
        if (abvCls != null)
            hookAllConstructors(abvCls, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    try {
                        setExpandState(param.getThisObject(), opt == 1 || opt == 3);
                        setResizable(param.getThisObject(), opt == 3 || opt == 4);
                    } catch (Throwable ignore) {
                    }
                }
            });
    }

    private void setExpandState(Object obj, boolean state) {
        if (state) {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(obj, "setExpandState", com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(
                    findClassIfExists("miui.app.ActionBar"),
                    "STATE_COLLAPSE"));
        } else {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(obj, "setExpandState", com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(
                    findClassIfExists("miui.app.ActionBar"),
                    "STATE_EXPAND"));
        }
    }

    private void setResizable(Object obj, boolean state) {
        if (state) {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(obj, "setResizable", false);
        } else {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(obj, "setResizable", true);
        }
    }
}
