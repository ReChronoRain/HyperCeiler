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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.home.title;

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class IconSize extends HomeBaseHookNew {

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        findAndHookMethod("com.miui.home.common.gridconfig.GridConfig$IconConfig", "getIconSize", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult(PrefsBridge.getInt("home_title_icon_size", 182));
            }
        });
    }

    @Override
    public void initBase() {
        /*findAndHookMethod("com.miui.home.settings.IconSizeSeekBar", "getCurrentSetIconSizeValue", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult(1.9f);
            }
        });
        findAndHookMethod("com.miui.home.launcher.common.PreferenceUtils", "getIconSizeScaleFromSP", float.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult(1.9f);
            }
        });*/
        findAndHookMethod("com.miui.home.launcher.GridConfig$IconConfig", "getIconSize", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult(PrefsBridge.getInt("home_title_icon_size", 182));
            }
        });
        /*setStaticIntField(findClassIfExists("com.miui.home.launcher.GridConfig"), "sCellCountYDef", 8);
        findAndHookMethod("com.miui.home.launcher.compat.GridSizeCalRules", "getCellCountY", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult(8);
            }
        });*/
        /*findAndHookMethod("com.miui.home.launcher.compat.GridSizeCalRules", "getWorkspaceCellSide", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult(100);
            }
        });*/
        /*findAndHookMethod("com.miui.home.launcher.DeviceConfig", "getIconHeight", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult(300);
            }
        });*/
        /*setStaticIntField(findClassIfExists("com.miui.home.launcher.GridConfig"), "sCellCountYDef", 7);
        findAndHookMethod("com.miui.home.launcher.compat.GridSizeCalRules", "getHotseatHeight", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult(100);
            }
        });*/
        /*findAndHookMethod("com.miui.home.launcher.compat.PhoneDeviceRules", "calGridSizeByFixedRows", int.class, int.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                setIntField(param.thisObject, "mCellSize", 300);
                setIntField(param.thisObject, "mHotseatCellWidth", 270);
            }
        });
        findAndHookMethod("com.miui.home.launcher.compat.PhoneDeviceRules", "calGridSizeByVariable", int.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                setIntField(param.thisObject, "mCellSize", 300);
                setIntField(param.thisObject, "mHotseatCellWidth", 270);
            }
        });*/
    }
}
