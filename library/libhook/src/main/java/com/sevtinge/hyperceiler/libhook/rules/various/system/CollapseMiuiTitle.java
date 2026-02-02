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

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class CollapseMiuiTitle extends BaseHook {

    @Override
    public void init() {
        Class<?> abvCls = findClassIfExists("com.miui.internal.widget.AbsActionBarView");

        int opt = mPrefsMap.getStringAsInt("various_collapse_miui_title", 0);

        if (abvCls != null)
            hookAllConstructors(abvCls, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    EzxHelpUtils.setIntField(param.getThisObject(), "mExpandState", (int) EzxHelpUtils.getStaticObjectField(
                            findClassIfExists("miui.app.ActionBar"),
                            "STATE_EXPAND"));
                    EzxHelpUtils.setIntField(param.getThisObject(), "mInnerExpandState", (int) EzxHelpUtils.getStaticObjectField(
                            findClassIfExists("miui.app.ActionBar"),
                            "STATE_COLLAPSE"));
                    if (opt == 2)
                        EzxHelpUtils.setBooleanField(param.getThisObject(), "mResizable", false);
                }
            });

        abvCls = findClassIfExists("miuix.appcompat.internal.app.widget.ActionBarView");
        if (abvCls != null)
            hookAllConstructors(abvCls, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
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
            EzxHelpUtils.callMethod(obj, "setExpandState", EzxHelpUtils.getObjectField(
                    findClassIfExists("miui.app.ActionBar"),
                    "STATE_COLLAPSE"));
        } else {
            EzxHelpUtils.callMethod(obj, "setExpandState", EzxHelpUtils.getObjectField(
                    findClassIfExists("miui.app.ActionBar"),
                    "STATE_EXPAND"));
        }
    }

    private void setResizable(Object obj, boolean state) {
        if (state) {
            EzxHelpUtils.callMethod(obj, "setResizable", false);
        } else {
            EzxHelpUtils.callMethod(obj, "setResizable", true);
        }
    }
}
