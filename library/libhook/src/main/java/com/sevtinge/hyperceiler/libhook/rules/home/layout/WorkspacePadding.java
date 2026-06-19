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
package com.sevtinge.hyperceiler.libhook.rules.home.layout;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode;

import android.content.Context;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class WorkspacePadding extends HomeBaseHookNew {

    // Context mContext;
    Class<?> mDeviceConfig;
    boolean usePx = false;

    @SuppressWarnings("unused")
    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        mDeviceConfig = findClassIfExists(DEVICE_CONFIG_NEW);
        initBaseCore();
    }

    @Override
    public void initBase() {
        mDeviceConfig = findClassIfExists(DEVICE_CONFIG_OLD);
        initBaseCore();
    }

    private void initBaseCore() {

        // findAndHookMethod(mDeviceConfig, "Init", Context.class, boolean.class, new IMethodHook() {
        //     @Override
        //     protected void before(MethodHookParam param) {
        //         mContext = (Context) param.args[0];
        //     }
        // });

        if (PrefsBridge.getBoolean("home_layout_workspace_padding_bottom_enable")) {
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingBottom",
                getPrefDimensionHook("home_layout_workspace_padding_bottom")
                             );
        }

        if (PrefsBridge.getBoolean("home_layout_workspace_padding_top_enable")) {
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingTop",
                getPrefDimensionHook("home_layout_workspace_padding_top")
                             );
        }

        if (PrefsBridge.getBoolean("home_layout_workspace_padding_horizontal_enable")) {
            XposedLog.d("===============home_layout_workspace_padding_horizontal: " + PrefsBridge.getInt("home_layout_workspace_padding_horizontal", 0));
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingSide",
                getPrefDimensionHook("home_layout_workspace_padding_horizontal")
                             );
        }
    }

    private IMethodHook getPrefDimensionHook(String key) {
        return new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (usePx) {
                    param.setResult(DisplayUtils.dp2px(
                        (float) PrefsBridge.getInt(key, 0)
                    ));
                    //logD(TAG, param.packageName, "Invoke setDimensionPixelSizeFormPrefs with $key, $defaultValue - result: ${param.result}");
                } else {
                    param.setResult(PrefsBridge.getInt(key, 0));
                }
            }
        };
    }
}
