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
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen;

import android.content.Context;
import android.widget.Toast;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.Objects;

public class DisableUnlockByBleToast extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.keyguard.KeyguardSecurityContainerController$2", "dismiss", boolean.class, int.class, boolean.class, "com.android.keyguard.KeyguardSecurityModel$SecurityMode", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod(Toast.class, "makeText", Context.class, int.class, int.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String resName = ((Context) param.args[0]).getResources().getResourceName((int) param.args[1]);
                        logD(TAG, lpparam.packageName, resName);
                        if (Objects.equals(resName, "com.android.systemui:string/miui_keyguard_ble_unlock_succeed_msg"))
                            findAndHookMethod(Toast.class, "show", new MethodHook() {
                                @Override
                                protected void before(MethodHookParam param) throws Throwable {
                                    param.setResult(null);
                                }
                            });
                    }
                });
            }
        });
        findAndHookMethod("com.android.keyguard.MiuiBleUnlockHelper", "tryUnlockByBle", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod(Toast.class, "makeText", Context.class, int.class, int.class, new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String resName = ((Context) param.args[0]).getResources().getResourceName((int) param.args[1]);
                        logD(TAG, lpparam.packageName, resName);
                        if (Objects.equals(resName, "com.android.systemui:string/miui_keyguard_ble_unlock_succeed_msg"))
                            findAndHookMethod(Toast.class, "show", new MethodHook() {
                                @Override
                                protected void before(MethodHookParam param) throws Throwable {
                                    param.setResult(null);
                                }
                            });
                    }
                });
            }
        });

    }
}
