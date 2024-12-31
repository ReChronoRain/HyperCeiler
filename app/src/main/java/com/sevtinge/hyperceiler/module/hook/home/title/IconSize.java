/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.home.title;

import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setStaticIntField;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class IconSize extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        /*findAndHookMethod("com.miui.home.settings.IconSizeSeekBar", "getCurrentSetIconSizeValue", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(1.9f);
            }
        });
        findAndHookMethod("com.miui.home.launcher.common.PreferenceUtils", "getIconSizeScaleFromSP", float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(1.9f);
            }
        });*/
        findAndHookMethod("com.miui.home.launcher.GridConfig$IconConfig", "getIconSize", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(mPrefsMap.getInt("home_title_icon_size", 182));
            }
        });
        /*setStaticIntField(findClassIfExists("com.miui.home.launcher.GridConfig"), "sCellCountYDef", 8);
        findAndHookMethod("com.miui.home.launcher.compat.GridSizeCalRules", "getCellCountY", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(8);
            }
        });*/
        /*findAndHookMethod("com.miui.home.launcher.compat.GridSizeCalRules", "getWorkspaceCellSide", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(100);
            }
        });*/
        /*findAndHookMethod("com.miui.home.launcher.DeviceConfig", "getIconHeight", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(300);
            }
        });*/
        /*setStaticIntField(findClassIfExists("com.miui.home.launcher.GridConfig"), "sCellCountYDef", 7);
        findAndHookMethod("com.miui.home.launcher.compat.GridSizeCalRules", "getHotseatHeight", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(100);
            }
        });*/
        /*findAndHookMethod("com.miui.home.launcher.compat.PhoneDeviceRules", "calGridSizeByFixedRows", int.class, int.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                setIntField(param.thisObject, "mCellSize", 300);
                setIntField(param.thisObject, "mHotseatCellWidth", 270);
            }
        });
        findAndHookMethod("com.miui.home.launcher.compat.PhoneDeviceRules", "calGridSizeByVariable", int.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                setIntField(param.thisObject, "mCellSize", 300);
                setIntField(param.thisObject, "mHotseatCellWidth", 270);
            }
        });*/
    }
}
