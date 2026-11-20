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
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AntiQues extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.DeviceNameCheckManager", "getDeviceNameCheckResult",
            Context.class, String.class, int.class,
            "com.android.settings.DeviceNameCheckManager$GetResultSuccessCallback", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    // The caller's callback saves the name. IS_INTERNATIONAL_BUILD is final
                    // on newer ROMs, so bypass the network check at its shared entry point.
                    Object[] args = param.getArgs();
                    callMethod(args[3], "resolveResult", args[1]);
                    param.setResult(null);
                }
            });
        Class<?> tetherFragment = findClassIfExists("com.android.settings.wifi.EditTetherFragment");
        if (tetherFragment != null) {
            findAndHookMethod(tetherFragment, "isSoftApSsidchanged", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    // Preserve the original wifi_ap_ssid_changed preference update, then
                    // let onSave use its existing path for saving without an online check.
                    if (param.getThrowable() == null) {
                        param.setResult(false);
                    }
                }
            });
            deoptimizeMethods(tetherFragment, "onSave");
        }
        findAndHookMethod("com.android.settings.bluetooth.MiuiBTUtils", "isSupportNameComplianceCheck", Context.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.settings.bluetooth.MiuiBTUtils", "isInternationalBuild", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(true);
            }
        });
    }
}
