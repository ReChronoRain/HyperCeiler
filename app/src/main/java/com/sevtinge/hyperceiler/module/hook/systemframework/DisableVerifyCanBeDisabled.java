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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;

public class DisableVerifyCanBeDisabled extends BaseHook {
    @Override
    public void init() {
        Class<?> pkg = findClassIfExists("com.android.server.pm.PackageManagerServiceImpl");
        if (pkg == null) {
            logE(TAG, "find class E com.android.server.pm.PackageManagerServiceImpl");
            return;
        }
        Method[] methods = pkg.getDeclaredMethods();
        Method voidMethod = null;
        Method booleanMethod = null;
        for (Method method : methods) {
            if ("canBeDisabled".equals(method.getName())) {
                if (method.getReturnType().equals(void.class)) {
                    voidMethod = method;
                    break;
                } else if (method.getReturnType().equals(boolean.class)) {
                    booleanMethod = method;
                    break;
                }
            }
        }
        if (voidMethod == null && booleanMethod == null) {
            logE(TAG, "method is null");
            return;
        }
        if (voidMethod != null) {
            hookMethod(voidMethod,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );
        } else {
            hookMethod(booleanMethod,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        }
    }
}
