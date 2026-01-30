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

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getStaticBooleanField;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setStaticBooleanField;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class AntiQues extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.MiuiDeviceNameEditFragment", "onSave", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                boolean originalValue = getStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true);
                setObjectExtra(param, "originalValue", originalValue);
            }
            @Override
            public void after(AfterHookParam param) {
                boolean originalValue = (boolean) getObjectExtra(param, "originalValue");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", originalValue);
            }
        });
        findAndHookMethod("com.android.settings.wifi.EditTetherFragment", "onSave", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                boolean originalValue = getStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true);
                setObjectExtra(param, "originalValue", originalValue);
            }
            @Override
            public void after(AfterHookParam param) {
                boolean originalValue = (boolean) getObjectExtra(param, "originalValue");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", originalValue);
            }
        });
        findAndHookMethod("com.android.settings.DeviceNameCheckManager", "getDeviceNameCheckResult", Context.class, String.class, int.class, "com.android.settings.DeviceNameCheckManager$GetResultSuccessCallback", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                boolean originalValue = getStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true);
                setObjectExtra(param, "originalValue", originalValue);
            }
            @Override
            public void after(AfterHookParam param) {
                boolean originalValue = (boolean) getObjectExtra(param, "originalValue");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", originalValue);
            }
        });
        findAndHookMethod("com.android.settings.bluetooth.MiuiBTUtils", "isSupportNameComplianceCheck", Context.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.settings.bluetooth.MiuiBTUtils", "isInternationalBuild", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(true);
            }
        });
    }
}
