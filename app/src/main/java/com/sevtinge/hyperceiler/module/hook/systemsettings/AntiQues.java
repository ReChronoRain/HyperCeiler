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
package com.sevtinge.hyperceiler.module.hook.systemsettings;

import static de.robv.android.xposed.XposedHelpers.getStaticBooleanField;
import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AntiQues extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.settings.MiuiDeviceNameEditFragment", "onSave", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                boolean originalValue = getStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true);
                param.setObjectExtra("originalValue", originalValue);
            }
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                boolean originalValue = (boolean) param.getObjectExtra("originalValue");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", originalValue);
            }
        });
        findAndHookMethod("com.android.settings.DeviceNameCheckManager", "getDeviceNameCheckResult", Context.class, String.class, int.class, "com.android.settings.DeviceNameCheckManager$GetResultSuccessCallback", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                boolean originalValue = getStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true);
                param.setObjectExtra("originalValue", originalValue);
            }
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                boolean originalValue = (boolean) param.getObjectExtra("originalValue");
                setStaticBooleanField(findClassIfExists("miui.os.Build"), "IS_INTERNATIONAL_BUILD", originalValue);
            }
        });
        findAndHookMethod("com.android.settings.bluetooth.MiuiBTUtils", "isSupportNameComplianceCheck", Context.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.settings.bluetooth.MiuiBTUtils", "isInternationalBuild", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
