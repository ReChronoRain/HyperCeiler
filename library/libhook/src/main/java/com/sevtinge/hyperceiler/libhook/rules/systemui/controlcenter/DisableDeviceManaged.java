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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setBooleanField;

import android.app.admin.DevicePolicyManager;
import android.graphics.drawable.Drawable;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class DisableDeviceManaged extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod(DevicePolicyManager.class, "isDeviceManaged", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(false);
            }
        });
        /*findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "isDeviceManaged", new MethodHook(){
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(false);
            }
        });*/
        findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "hasCACertInCurrentUser", new IMethodHook(){
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "hasCACertInWorkProfile", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(false);
            }
        });
        findAndHookConstructor("com.android.systemui.security.data.model.SecurityModel", boolean.class, boolean.class, boolean.class, boolean.class, String.class, String.class, boolean.class, boolean.class, String.class, String.class, boolean.class, boolean.class, boolean.class, Drawable.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = false;
                param.getArgs()[10] = false;
                param.getArgs()[11] = false;
                setBooleanField(param.getThisObject(), "isDeviceManaged", false);
                setBooleanField(param.getThisObject(), "hasCACertInCurrentUser", false);
                setBooleanField(param.getThisObject(), "hasCACertInWorkProfile", false);
            }
        });
    }
}
