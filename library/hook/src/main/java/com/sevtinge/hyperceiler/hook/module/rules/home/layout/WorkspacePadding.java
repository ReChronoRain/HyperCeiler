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
package com.sevtinge.hyperceiler.hook.module.rules.home.layout;

import static com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.getPackageVersionCode;

import android.content.Context;

import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils;

public class WorkspacePadding extends HomeBaseHookNew {

    Context mContext;
    Class<?> mDeviceConfig;

    @Override
    public void initBase() {
        mDeviceConfig = findClassIfExists(getPackageVersionCode(lpparam) < 600000000 ? DEVICE_CONFIG_NEW : DEVICE_CONFIG_OLD);

        findAndHookMethod(mDeviceConfig, "Init", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                mContext = (Context) param.args[0];
            }
        });

        if (mPrefsMap.getBoolean("home_layout_workspace_padding_bottom_enable")) {
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingBottom", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(DisplayUtils.dp2px(mContext, mPrefsMap.getInt("home_layout_workspace_padding_bottom", 0)));
                }
            });
        }

        if (mPrefsMap.getBoolean("home_layout_workspace_padding_top_enable")) {
            try {
                // 新版本桌面，先标记，后续再做进一步修改
                findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingTop", Context.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(DisplayUtils.dp2px(mPrefsMap.getInt("home_layout_workspace_padding_top", 0)));
                    }
                });
            } catch (Throwable t) {
                findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingTop", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(DisplayUtils.dp2px(mPrefsMap.getInt("home_layout_workspace_padding_top", 0)));
                    }
                });
            }
        }

        if (mPrefsMap.getBoolean("home_layout_workspace_padding_horizontal_enable")) {
            logE("===============home_layout_workspace_padding_horizontal: " + mPrefsMap.getInt("home_layout_workspace_padding_horizontal", 0));
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingSide", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(DisplayUtils.dp2px(mPrefsMap.getInt("home_layout_workspace_padding_horizontal", 0)));
                }
            });
        }
    }
}
