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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter;

import static de.robv.android.xposed.XposedHelpers.setBooleanField;

import android.app.admin.DevicePolicyManager;
import android.graphics.drawable.Drawable;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class DisableDeviceManaged extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod(DevicePolicyManager.class, "isDeviceManaged", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        /*findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "isDeviceManaged", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });*/
        findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "hasCACertInCurrentUser", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "hasCACertInWorkProfile", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookConstructor("com.android.systemui.security.data.model.SecurityModel", boolean.class, boolean.class, boolean.class, boolean.class, String.class, String.class, boolean.class, boolean.class, String.class, String.class, boolean.class, boolean.class, boolean.class, Drawable.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = false;
                param.args[10] = false;
                param.args[11] = false;
                setBooleanField(param.thisObject, "isDeviceManaged", false);
                setBooleanField(param.thisObject, "hasCACertInCurrentUser", false);
                setBooleanField(param.thisObject, "hasCACertInWorkProfile", false);
            }
        });
    }
}
