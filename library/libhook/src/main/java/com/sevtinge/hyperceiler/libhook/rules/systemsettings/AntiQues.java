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
import android.os.Build;
import android.widget.EditText;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AntiQues extends BaseHook {
    @Override
    public void init() {
        if (Build.VERSION.SDK_INT >= 37) {
            // HyperOS 4 reads this final field in onSave. Android 17 no longer lets
            // the old hook change it, so use the existing save path directly.
            findAndHookMethod("com.android.settings.MiuiDeviceNameEditFragment", "onSave", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Object fragment = param.getThisObject();
                    EditText editText = (EditText) getObjectField(fragment, "mDeviceNameEdit");
                    String name = editText.getText().toString().trim();
                    callStaticMethod(fragment.getClass(), "onSaveImpl", name, fragment);
                    param.setResult(null);
                }
            });
            // In HyperOS 4, onSave only uses this result to choose whether to
            // run the SSID check. The other branch still saves the new config.
            findAndHookMethod("com.android.settings.wifi.EditTetherFragment", "isSoftApSsidchanged", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(false);
                }
            });
        } else {
            findAndHookMethod("com.android.settings.MiuiDeviceNameEditFragment", "onSave", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    boolean originalValue = getStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
                    setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true);
                    setObjectExtra(param, "originalValue", originalValue);
                }
                @Override
                public void after(HookParam param) {
                    boolean originalValue = (boolean) getObjectExtra(param, "originalValue");
                    setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", originalValue);
                }
            });
            findAndHookMethod("com.android.settings.wifi.EditTetherFragment", "onSave", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    boolean originalValue = getStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
                    setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true);
                    setObjectExtra(param, "originalValue", originalValue);
                }
                @Override
                public void after(HookParam param) {
                    boolean originalValue = (boolean) getObjectExtra(param, "originalValue");
                    setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", originalValue);
                }
            });
            findAndHookMethod("com.android.settings.DeviceNameCheckManager", "getDeviceNameCheckResult", Context.class, String.class, int.class, "com.android.settings.DeviceNameCheckManager$GetResultSuccessCallback", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    boolean originalValue = getStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
                    setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true);
                    setObjectExtra(param, "originalValue", originalValue);
                }
                @Override
                public void after(HookParam param) {
                    boolean originalValue = (boolean) getObjectExtra(param, "originalValue");
                    setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", originalValue);
                }
            });
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
